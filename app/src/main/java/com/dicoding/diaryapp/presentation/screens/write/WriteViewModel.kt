package com.dicoding.diaryapp.presentation.screens.write

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.diaryapp.data.database.entity.ImageToDelete
import com.dicoding.diaryapp.data.database.entity.ImageToDeleteDao
import com.dicoding.diaryapp.data.database.entity.ImageToUpload
import com.dicoding.diaryapp.data.database.entity.ImageToUploadDao
import com.dicoding.diaryapp.data.repository.MongoDB
import com.dicoding.diaryapp.model.Diary
import com.dicoding.diaryapp.model.GalleryImage
import com.dicoding.diaryapp.model.GalleryState
import com.dicoding.diaryapp.model.Mood
import com.dicoding.diaryapp.util.Constants.WRITE_SCREEN_ARGUMENT_KEY
import com.dicoding.diaryapp.model.RequestState
import com.dicoding.diaryapp.util.fetchImagesFromFirebase
import com.dicoding.diaryapp.util.toRealmInstant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import io.realm.kotlin.types.ObjectId
import io.realm.kotlin.types.RealmInstant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import javax.inject.Inject

@HiltViewModel
class WriteViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imagesToUploadDao: ImageToUploadDao,
    private val imageToDeleteDao: ImageToDeleteDao
) : ViewModel() {
    val galleryState = GalleryState()
    var uiState by mutableStateOf(uiState())
        private set

    init {
        getDiaryIdArgument()
        fetchSelectedDiary()
    }

    private fun getDiaryIdArgument() {
        uiState = uiState.copy(
            selectedDiaryId = savedStateHandle.get<String>(key = WRITE_SCREEN_ARGUMENT_KEY)
        )

    }

    private fun fetchSelectedDiary() {
        if (uiState.selectedDiaryId != null) {
            viewModelScope.launch(Dispatchers.Main) {
                MongoDB.getSelecctedDiary(ObjectId.Companion.from(uiState.selectedDiaryId!!))
                    .catch {
                        //  interception  any exceptin to the emit
                        emit(RequestState.Error(Exception("Diary is already deleted")))
                    }
                    .collect { diary ->
                        if (diary is RequestState.Success) {
                            setSelectedDiary(diary = diary.data)
                            setTitle(title = diary.data.title)
                            setDescription(description = diary.data.description)
                            setMood(mood = Mood.valueOf(diary.data.mood))

                            fetchImagesFromFirebase(
                                remoteImagePaths = diary.data.images,
                                onImageDownload = { downloadImage ->
                                    galleryState.addImage(
                                        GalleryImage(
                                            image = downloadImage,
                                            remoteImagePath = extractImagePath(
                                                fullImageUrl = downloadImage.toString()
                                            )
                                        )
                                    )

                                }
                            )

                        }
                    }

            }

        }
    }

    private fun setSelectedDiary(diary: Diary) {
        uiState = uiState.copy(selectedDiary = diary)
    }

    fun setTitle(title: String) {
        uiState = uiState.copy(title = title)
    }

    fun setDescription(description: String) {
        uiState = uiState.copy(description = description)
    }

    private fun setMood(mood: Mood) {
        uiState = uiState.copy(mood = mood)
    }

    fun updateDateTime(zonedDateTime: ZonedDateTime) {
        uiState =
            uiState.copy(updatedDateTime = zonedDateTime.toInstant().toRealmInstant())


    }

    fun upsertDiary(
        diary: Diary, onSuccess: () -> Unit, onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.selectedDiaryId != null) {
                updateDiary(diary = diary, onSuccess = onSuccess, onError = onError)

            } else {
                insertDiary(diary = diary, onSuccess = onSuccess, onError = onError)
            }


        }
    }

    private suspend fun insertDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val result = MongoDB.addNewDiary(diary.apply {
            if (uiState.updatedDateTime != null) {
                date = uiState.updatedDateTime!!
            }
        })
        if (result is RequestState.Success) {
            uploadImagesToFirebase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }

        } else if (result is RequestState.Error)
            withContext(Dispatchers.Main) {
                onError(result.exception.message!!)
            }


    }

    private suspend fun updateDiary(
        diary: Diary,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val result = MongoDB.updateDiary(diary.apply {
            _id = ObjectId.Companion.from(uiState.selectedDiaryId!!)
            date = if (uiState.updatedDateTime != null) {
                uiState.updatedDateTime!!
            } else {
                uiState.selectedDiary!!.date
            }
        })
        if (result is RequestState.Success) {
            uploadImagesToFirebase()
            deleteImageFromFirebase()
            withContext(Dispatchers.Main) {
                onSuccess()
            }
        } else if (result is RequestState.Error) {
            withContext(Dispatchers.Main) {
                onError(result.exception.message!!)
            }
        }
    }

    fun deleteDiary(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (uiState.selectedDiaryId != null) {
                val result = MongoDB.deleteDiary(ObjectId.Companion.from(uiState.selectedDiaryId!!))
                if (result is RequestState.Success) {
                    withContext(Dispatchers.Main) {
                        uiState.selectedDiary?.let { deleteImageFromFirebase(images = it.images) }
                        onSuccess()
                    }
                } else if (result is RequestState.Error) {
                    withContext(Dispatchers.Main) {
                        onError(result.exception.message!!)
                    }
                }
            }

        }
    }

    fun addImage(image: Uri, imageType: String) {
        val remoteImagePath =
            "images/${FirebaseAuth.getInstance().currentUser?.uid}/${image.lastPathSegment}-${System.currentTimeMillis()}.$imageType"
        galleryState.addImage(
            GalleryImage(
                image = image,
                remoteImagePath = remoteImagePath
            )
        )
    }

    private fun uploadImagesToFirebase() {
        val storage = FirebaseStorage.getInstance().reference
        galleryState.images.forEach { image ->
            Log.d("UploadImage", image.remoteImagePath)
            val imagePath = storage.child(image.remoteImagePath)
            imagePath.putFile(image.image)
                .addOnProgressListener { taskSnapshot ->
                    val sessionUri = taskSnapshot.uploadSessionUri
                    if (sessionUri != null) {
                        viewModelScope.launch(Dispatchers.IO) {
                            imagesToUploadDao.addImageToUpload(
                                ImageToUpload(
                                    remoteImagePath = image.remoteImagePath,
                                    imageUri = image.image.toString(),
                                    sessionUri = sessionUri.toString()
                                )
                            )
                        }

                    }
                }
                .addOnFailureListener {
                    Log.d("UploadImage", "Failed")
                }
                .addOnCanceledListener {
                    Log.d("UploadImage", "Canceled")
                }
        }
    }

    private fun deleteImageFromFirebase(images: List<String>? = null) {
        val storage = FirebaseStorage.getInstance().reference
        if (images != null) {
            images.forEach { remotePath ->
                storage.child(remotePath).delete()
                    .addOnFailureListener{
                    viewModelScope.launch(Dispatchers.IO) {
                        imageToDeleteDao.addImageToDelete(ImageToDelete(remoteImagePath = remotePath))
                    }
                }
            }
        } else {
            galleryState.imagesToBeDeleted.map {
                it.remoteImagePath
            }.forEach { remotePath ->
                storage.child(remotePath).delete()
                    .addOnFailureListener{
                        viewModelScope.launch(Dispatchers.IO) {
                            imageToDeleteDao.addImageToDelete(ImageToDelete(remoteImagePath = remotePath))
                        }
                    }
            }
        }

    }

    private fun extractImagePath(fullImageUrl: String): String {
        val chunks = fullImageUrl.split("%2F")
        val imageName = chunks[2].split("?").first()
        return "images/${Firebase.auth.currentUser?.uid}/$imageName"
    }

}

data class uiState(
    val selectedDiaryId: String? = null,
    val selectedDiary: Diary? = null,
    val title: String = "",
    val description: String = "",
    val mood: Mood = Mood.Neutral,
    val updatedDateTime: RealmInstant? = null
)