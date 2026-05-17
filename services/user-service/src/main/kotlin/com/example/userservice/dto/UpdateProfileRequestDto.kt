package com.example.userservice.dto

import com.example.userservice.entity.JlptLevel
import com.example.userservice.validation.ValidJlptProgression
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
    val jlptGoal: JlptLevel,
)
