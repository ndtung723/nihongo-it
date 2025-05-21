package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateFlashcardRequestDto(
    @JsonProperty("frontText")
    val frontText: String,
    
    @JsonProperty("backText")
    val backText: String,

    @JsonProperty("vocabularyId")
    val vocabularyId: UUID? = null
) 