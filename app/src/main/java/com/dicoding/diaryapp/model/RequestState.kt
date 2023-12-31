package com.dicoding.diaryapp.model

sealed class RequestState<out T>{
    object Idle: RequestState<Nothing>()
    object Loading: RequestState<Nothing>()
    data class Success<T>(val data: T): RequestState<T>()
    data class Error(val exception: Throwable): RequestState<Nothing>()
}
