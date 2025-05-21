package com.example.learningservice.dto

import com.example.learningservice.entity.JlptLevel
import com.example.learningservice.validation.ValidJlptProgression
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@ValidJlptProgression(message = "Current level cannot be higher than goal level")
data class UpdateProfileRequestDto(
    @field:NotBlank(message = "Full name cannot be blank")
    @field:Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @JsonProperty("fullName")
    val fullName: String,

    @JsonProperty("currentLevel")
    val currentLevel: JlptLevel,

    @JsonProperty("jlptGoal")
    val jlptGoal: JlptLevel
) 