package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class CreateFlashcardRequestDto(
    @JsonProperty("frontText")
    val frontText: String,

    @JsonProperty("backText")
    val backText: String,

    @JsonProperty("vocabularyId")
    val vocabularyId: UUID? = null,
)
