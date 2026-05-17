package com.example.learningservice.dto

import com.example.learningservice.entity.JlptLevel
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserCreateRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Email format is invalid")
    @field:Size(max = 50, message = "Email must be less than 50 characters")
    @JsonProperty("email")
    val email: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    @JsonProperty("password")
    val password: String,
    @field:NotBlank(message = "Full name is required")
    @field:Size(max = 100, message = "Full name must be less than 100 characters")
    @JsonProperty("fullName")
    val fullName: String,
    @JsonProperty("profilePicture")
    val profilePicture: String? = null,
    @JsonProperty("currentLevel")
    val currentLevel: JlptLevel? = JlptLevel.N5,
    @JsonProperty("jlptGoal")
    val jlptGoal: JlptLevel? = JlptLevel.N3,
    @JsonProperty("roleId")
    val roleId: Int = 2, // Default to ROLE_USER
)
