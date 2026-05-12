package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SignupResponseDto(
    val message: String? = null,
)
