package com.example.tweetapp.model

sealed class ApiState<out T : Any> {
    data class Success<out T : Any>(val data: T) : ApiState<T>()
    data class Error(val message: String) : ApiState<Nothing>()
    object Loading: ApiState<Nothing>()
}
