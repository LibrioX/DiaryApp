package com.dicoding.diaryapp.presentation.screens.home

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.diaryapp.data.repository.Diaries
import com.dicoding.diaryapp.data.repository.MongoDB
import com.dicoding.diaryapp.model.RequestState
import kotlinx.coroutines.launch

class HomeViewModel :ViewModel() {
    var diaries: MutableState<Diaries> = mutableStateOf(RequestState.Idle)

    init {
        observeAllDiaries()
    }

    private fun observeAllDiaries(){
        viewModelScope.launch {
            MongoDB.getAllDiaries().collect {
                diaries.value = it
            }
        }

    }
}