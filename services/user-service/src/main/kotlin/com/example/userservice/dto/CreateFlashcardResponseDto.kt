package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateFlashcardResponseDto(
    @JsonProperty("data")
    val data: FlashcardDTO? = null,
)
