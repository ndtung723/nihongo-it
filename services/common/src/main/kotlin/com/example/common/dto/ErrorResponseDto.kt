package com.example.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponseDto(
    @JsonProperty("message")
    val message: String? = null,
    @JsonProperty("errors")
    val errors: List<FieldErrorDto>? = null,
)
