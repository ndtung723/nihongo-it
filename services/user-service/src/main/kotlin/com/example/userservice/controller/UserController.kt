package com.example.userservice.controller

import com.example.userservice.service.UserService
import com.example.userservice.util.UserAuthUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/user/user")
@Tag(name = "User Management", description = "API endpoints for managing user profiles and preferences")
class UserController(
    private val userService: UserService,
    private val userAuthUtil: UserAuthUtil,
) {
    /**
     * Get user profile information
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user's profile information")
    fun getUserProfile(): ResponseEntity<Any> {
        val userId =
            userAuthUtil.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated")

        return try {
            val profile = userService.getUserProfile(userId)
            ResponseEntity.ok(profile)
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get user profile: ${e.message}"))
        }
    }

    /**
     * Update notification preferences for the current user
     */
    @PutMapping("/preferences")
    @Operation(summary = "Update notification preferences for the current user")
    fun updateNotificationPreferences(
        @RequestBody request: UpdatePreferencesRequest,
    ): ResponseEntity<Any> {
        val userId =
            userAuthUtil.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated")

        return try {
            userService.updateNotificationPreferences(userId, request)
            ResponseEntity.ok(mapOf("message" to "Preferences updated successfully"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to update preferences: ${e.message}"))
        }
    }

    /**
     * Get user preferences including notification settings
     */
    @GetMapping("/preferences")
    @Operation(summary = "Get current user's notification preferences")
    fun getNotificationPreferences(): ResponseEntity<Any> {
        val userId =
            userAuthUtil.getCurrentUserId()
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated")

        return try {
            val preferences = userService.getNotificationPreferences(userId)
            ResponseEntity.ok(preferences)
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("error" to "Failed to get preferences: ${e.message}"))
        }
    }
}

/**
 * Request DTO for updating user notification preferences
 */
data class UpdatePreferencesRequest(
    val notificationPreferences: String? = null,
    val reminderEnabled: Boolean? = null,
    val reminderTime: String? = null,
    val minCardThreshold: Int? = null,
    val leechNotificationsEnabled: Boolean? = null,
)
