package com.example.userservice.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO for changing user password
 */
data class ChangePasswordRequestDto(
    @get:NotBlank(message = "Current password is required")
    @get:Size(max = 100, message = "Current password must not exceed 100 characters")
    val currentPassword: String,
    @get:NotBlank(message = "New password is required")
    @get:Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
    val newPassword: String,
    @get:NotBlank(message = "Password confirmation is required")
    @get:Size(max = 100, message = "Password confirmation must not exceed 100 characters")
    val confirmPassword: String,
)
