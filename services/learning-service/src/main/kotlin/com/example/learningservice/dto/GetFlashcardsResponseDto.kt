package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetFlashcardsResponseDto(
    @JsonProperty("data")
    val data: List<FlashcardDTO>,
)
