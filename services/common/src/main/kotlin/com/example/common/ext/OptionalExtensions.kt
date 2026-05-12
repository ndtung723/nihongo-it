package com.example.common.ext

import com.example.common.exception.BusinessException
import java.util.Optional

fun <T> Optional<T>.orThrow(message: String): T = orElseThrow { BusinessException(message) }
