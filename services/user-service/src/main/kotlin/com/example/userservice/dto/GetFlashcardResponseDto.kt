package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetFlashcardResponseDto(
    @JsonProperty("data")
    val data: FlashcardDTO,
)
