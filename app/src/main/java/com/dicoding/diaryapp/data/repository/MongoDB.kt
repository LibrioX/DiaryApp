package com.dicoding.diaryapp.data.repository

import android.util.Log
import com.dicoding.diaryapp.model.Diary
import com.dicoding.diaryapp.util.Constants.APP_ID
import com.dicoding.diaryapp.model.RequestState
import com.dicoding.diaryapp.util.toInstant
import io.realm.kotlin.Realm
import io.realm.kotlin.ext.query
import io.realm.kotlin.log.LogLevel
import io.realm.kotlin.mongodb.App
import io.realm.kotlin.mongodb.sync.SyncConfiguration
import io.realm.kotlin.query.Sort
import io.realm.kotlin.types.ObjectId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId

object MongoDB : MongoRepository {
    private val app = App.create(APP_ID)
    private val user = app.currentUser
    private lateinit var realm: Realm

    init {
        configureTheRealm()
    }

    override fun configureTheRealm() {
        if (user != null) {
            val config = SyncConfiguration.Builder(user, setOf(Diary::class))
                .initialSubscriptions { sub ->
                    add(
                        query = sub.query<Diary>("ownerId == $0", user.identity),
                        name = "User's Diaries"
                    )

                }
                .log(LogLevel.ALL).build()
            realm = Realm.open(config)
        }

    }

    override fun getAllDiaries(): Flow<Diaries> {
        return if (user != null) {
            Log.d("HomeScreen", "HomeScreen: ${user.identity}")
            try {
                realm.query<Diary>(query = "ownerId == $0", user.identity)
                    .sort(property = "date", sortOrder = Sort.DESCENDING)
                    .asFlow()
                    .map { result ->
                        Log.d("HomeScreen", "HomeScreen: ${result.list}")
                        RequestState.Success(
                            data = result.list.groupBy {
                                it.date.toInstant()
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                            }
                        )
                    }
            } catch (e: Exception) {
                flow { emit(RequestState.Error(e)) }
            }
        } else {
            flow { emit(RequestState.Error(UserNotAuthenticatedException())) }
        }
    }

    override fun getSelecctedDiary(diaryId: ObjectId): Flow<RequestState<Diary>> {
        return if (user != null) {
            try {
                realm.query<Diary>(query = "_id == $0", diaryId).asFlow().map {
                    RequestState.Success(it.list.first())
                }

            } catch (e: Exception) {
                flow { emit(RequestState.Error(e)) }
            }
        } else {
            flow { emit(RequestState.Error(UserNotAuthenticatedException())) }
        }
    }

    override suspend fun addNewDiary(diary: Diary): RequestState<Diary> {
        return if (user != null) {
            realm.write {
                try {
                    val addedDIary = copyToRealm(diary.apply {
                        ownerId = user.identity
                    })
                    RequestState.Success(addedDIary)
                } catch (e: Exception) {
                    RequestState.Error(e)
                }
            }

        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }

    override suspend fun updateDiary(diary: Diary): RequestState<Diary> {
        return if (user != null) {
            realm.write {
                val queryDiary = query<Diary>(query = "_id== $0", diary._id).first().find()
                if (queryDiary != null) {
                    queryDiary.apply {
                        mood = diary.mood
                        title = diary.title
                        description = diary.description
                        images = diary.images
                        date = diary.date
                    }
                    RequestState.Success(data = queryDiary)
                } else {
                    RequestState.Error(Exception("Diary not found"))
                }
            }

        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }

    override suspend fun deleteDiary(diaryId: ObjectId): RequestState<Diary> {
        return if (user != null) {
            realm.write {
                val diary = query<Diary>(
                    query = "_id == $0 AND ownerId == $1",
                    diaryId,
                    user.identity
                ).first().find()
                if (diary != null) {
                    try {
                        delete(diary)
                        RequestState.Success(diary)
                    } catch (e: Exception) {
                        RequestState.Error(e)
                    }
                } else {
                    RequestState.Error(Exception("Diary not found"))
                }

            }

        } else {
            RequestState.Error(UserNotAuthenticatedException())
        }
    }
}

private class UserNotAuthenticatedException : Exception("User is not authenticated")