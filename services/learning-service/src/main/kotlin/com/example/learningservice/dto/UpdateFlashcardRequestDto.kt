package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateFlashcardRequestDto(
    @JsonProperty("frontText")
    val frontText: String,
    
    @JsonProperty("backText")
    val backText: String,
)