package com.example.common.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class FieldErrorDto(
    @JsonProperty("field")
    val field: String,
    @JsonProperty("message")
    val message: String,
)
