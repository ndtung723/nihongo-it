package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ReviewFlashcardResponseDto(
    @JsonProperty("data")
    val data: FlashcardDTO,
)
