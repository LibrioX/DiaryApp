package com.dicoding.diaryapp.data.repository

import com.dicoding.diaryapp.model.Diary
import com.dicoding.diaryapp.util.RequestState
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

typealias Diaries = RequestState<Map<LocalDate, List<Diary>>>

interface MongoRepository {
    fun configureTheRealm()
    fun getAllDiaries(): Flow<Diaries>
}