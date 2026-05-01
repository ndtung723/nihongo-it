package com.example.userservice.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * DTO for requesting a password reset
 */
@Schema(description = "Request for initiating password reset process")
data class PasswordResetRequestDto(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Please provide a valid email address")
    @Schema(description = "User's email address", example = "user@example.com", required = true)
    val email: String
)

/**
 * DTO for resetting password with token
 */
@Schema(description = "Request for resetting password using token")
data class ResetPasswordDto(
    @field:NotBlank(message = "Token is required")
    @Schema(description = "Password reset token received via email", required = true)
    val token: String,
    
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Schema(description = "New password", required = true)
    val password: String,
    
    @field:NotBlank(message = "Password confirmation is required")
    @Schema(description = "Confirmation of new password", required = true)
    val confirmPassword: String
)

/**
 * DTO for password reset response
 */
@Schema(description = "Response for password reset operations")
data class PasswordResetResponseDto(
    @Schema(description = "Response message", required = true)
    val message: String
)