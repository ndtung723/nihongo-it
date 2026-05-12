package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @JsonProperty(value = "email")
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String,

    @JsonProperty(value = "password")
    @field:NotBlank(message = "Password is required")
    @field:Size(max = 100, message = "Password must not exceed 100 characters")
    val password: String,
)
