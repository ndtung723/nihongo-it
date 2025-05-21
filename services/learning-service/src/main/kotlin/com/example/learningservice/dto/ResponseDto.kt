package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL) // Only include non-null fields in JSON
data class ResponseDto(
    @JsonProperty("status")
    val status: ResponseType,
    @JsonProperty("message")
    val message: String? = null,
    @JsonProperty("messageId")
    val messageId: String? = null,
    @JsonProperty("errors")
    val errors: Map<String, String?>? = null, // Optional, excluded if null
)