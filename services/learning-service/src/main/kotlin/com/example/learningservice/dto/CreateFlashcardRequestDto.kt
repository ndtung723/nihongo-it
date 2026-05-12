package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateFlashcardRequestDto(
    @JsonProperty("frontText")
    @get:NotBlank(message = "Front text is required")
    @get:Size(max = 500, message = "Front text must not exceed 500 characters")
    val frontText: String,

    @JsonProperty("backText")
    @get:NotBlank(message = "Back text is required")
    @get:Size(max = 500, message = "Back text must not exceed 500 characters")
    val backText: String,

    @JsonProperty("vocabularyId")
    val vocabularyId: UUID? = null,
)
