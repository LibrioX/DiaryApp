package com.dicoding.diaryapp.data.repository

import com.dicoding.diaryapp.model.Diary
import com.dicoding.diaryapp.model.RequestState
import io.realm.kotlin.types.ObjectId
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

typealias Diaries = RequestState<Map<LocalDate, List<Diary>>>

interface MongoRepository {
    fun configureTheRealm()
    fun getAllDiaries(): Flow<Diaries>
    fun getSelecctedDiary(diaryId: ObjectId)  : Flow<RequestState<Diary>>
    suspend fun addNewDiary(diary: Diary): RequestState<Diary>
    suspend fun updateDiary(diary: Diary): RequestState<Diary>
    suspend fun deleteDiary(diaryId: ObjectId): RequestState<Diary>
}