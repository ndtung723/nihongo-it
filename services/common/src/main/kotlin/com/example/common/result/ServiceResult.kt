package com.example.common.result

import com.example.common.exception.BusinessException

sealed class ServiceResult<out T> {
    data class Success<out T>(val value: T) : ServiceResult<T>()
    data class Failure(val message: String) : ServiceResult<Nothing>()

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Failure -> throw BusinessException(message)
    }

    fun <R> map(transform: (T) -> R): ServiceResult<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}
