package com.dicoding.diaryapp

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.dicoding.diaryapp.data.database.entity.ImageToDeleteDao
import com.dicoding.diaryapp.data.database.entity.ImageToUploadDao
import com.dicoding.diaryapp.navigation.Screen
import com.dicoding.diaryapp.navigation.SetupNavGraph
import com.dicoding.diaryapp.ui.theme.DiaryAppTheme
import com.dicoding.diaryapp.util.Constants.APP_ID
import com.dicoding.diaryapp.util.retryDeletingImageFromFirebase
import com.dicoding.diaryapp.util.retryUploadingImageToFirebase
import com.google.firebase.FirebaseApp
import dagger.hilt.android.AndroidEntryPoint
import io.realm.kotlin.mongodb.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var imageToUploadDao: ImageToUploadDao
    @Inject
    lateinit var imageToDeleteDao: ImageToDeleteDao
    var keepSplashOpened = true
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen().setKeepOnScreenCondition{
            keepSplashOpened
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        FirebaseApp.initializeApp(this)

        setContent {
            DiaryAppTheme {
                val navController = rememberNavController()
                SetupNavGraph(
                    startDestination = getStartDestination(),
                    navController = navController,
                    onDataLoaded = {
                        keepSplashOpened = false
                    }
                )
            }
        }
        cleanupCheck(
            scope = lifecycleScope, imageToDeleteDao = imageToDeleteDao, imageToUpladDao = imageToUploadDao
        )
    }
}

private fun cleanupCheck(
    scope: CoroutineScope,
    imageToUpladDao : ImageToUploadDao,
    imageToDeleteDao: ImageToDeleteDao
){
    scope.launch (Dispatchers.IO) {
        val images = imageToUpladDao.getAllImages()
        images.forEach { imageToUpload ->
            retryUploadingImageToFirebase(imageToUpload, onSuccess = {
                scope.launch(Dispatchers.IO){
                    imageToUpladDao.cleanupImage(imageId = imageToUpload.id)
                }
            })
        }
        val result2 = imageToDeleteDao.getAllImages()
        result2.forEach { imageToDelete ->
            retryDeletingImageFromFirebase(imageToDelete, onSuccess = {
                scope.launch(Dispatchers.IO){
                    imageToDeleteDao.cleanupImage(imageId = imageToDelete.id)
                }
            })
        }
    }
}

private fun getStartDestination(): String {
    val user = App.create(APP_ID).currentUser
    return  if(user != null && user.loggedIn) Screen.Home.route
    else Screen.Authentication.route
}
