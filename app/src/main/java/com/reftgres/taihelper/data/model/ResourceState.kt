package com.reftgres.taihelper.data.model

/**
 * Обертка для результата операции
 */
sealed class ResourceState<out T> {
    data class Success<out T>(val data: T) : ResourceState<T>()
    data class Error(val message: String, val exception: Exception? = null) : ResourceState<Nothing>()
    object Loading : ResourceState<Nothing>()
}