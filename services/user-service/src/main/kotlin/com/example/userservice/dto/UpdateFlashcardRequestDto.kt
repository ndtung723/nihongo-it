package com.example.nihongoit.dto.flashcard

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateFlashcardRequestDto(
    @JsonProperty("frontText")
    val frontText: String,

    @JsonProperty("backText")
    val backText: String,
)
