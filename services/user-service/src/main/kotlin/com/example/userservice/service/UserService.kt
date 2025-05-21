package com.example.userservice.service

import com.example.userservice.controller.UpdatePreferencesRequest
import com.example.userservice.entity.UserEntity
import com.example.userservice.exception.BusinessException
import com.example.userservice.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class UserService @Autowired constructor(
    private val userRepository: UserRepository,
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)
    
    /**
     * Get user profile information
     */
    fun getUserProfile(userId: UUID): Map<String, Any> {
        val user = getUserById(userId)
        
        return mapOf(
            "userId" to (user.userId ?: ""),
            "email" to user.email,
            "fullName" to user.fullName,
            "profilePicture" to (user.profilePicture ?: ""),
            "currentLevel" to (user.currentLevel?.name ?: ""),
            "jlptGoal" to (user.jlptGoal?.name ?: ""),
            "streakCount" to user.streakCount,
            "points" to user.points,
            "lastLogin" to (user.lastLogin?.toString() ?: ""),
            "isEmailVerified" to user.isEmailVerified,
            "createdAt" to user.createdAt.toString()
        )
    }
    
    /**
     * Update notification preferences for a user
     */
    @Transactional
    fun updateNotificationPreferences(userId: UUID, request: UpdatePreferencesRequest) {
        logger.info("Updating notification preferences for user $userId")
        
        val user = getUserById(userId)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        
        // Create a copy of the user with updated preferences
        val updatedUser = user.copy(
            notificationPreferences = request.notificationPreferences ?: user.notificationPreferences,
            reminderEnabled = request.reminderEnabled ?: user.reminderEnabled,
            reminderTime = if (request.reminderTime != null) {
                LocalTime.parse(request.reminderTime, timeFormatter)
            } else {
                user.reminderTime
            },
            minCardThreshold = request.minCardThreshold ?: user.minCardThreshold,
            updatedAt = LocalDateTime.now()
        ).let { updatedUser ->
            // Handle leech notifications by adding/removing "leech" from notificationPreferences
            if (request.leechNotificationsEnabled != null) {
                val preferences = updatedUser.notificationPreferences.split(",").toMutableList()
                
                if (request.leechNotificationsEnabled && !preferences.contains("leech")) {
                    preferences.add("leech")
                } else if (!request.leechNotificationsEnabled) {
                    preferences.remove("leech")
                }
                
                updatedUser.copy(notificationPreferences = preferences.joinToString(","))
            } else {
                updatedUser
            }
        }
        
        userRepository.save(updatedUser)
        logger.info("Notification preferences updated for user $userId")
    }
    
    /**
     * Get notification preferences for a user
     */
    fun getNotificationPreferences(userId: UUID): Map<String, Any> {
        val user = getUserById(userId)
        
        val notificationPrefs = user.notificationPreferences.split(",").map { it.trim() }
        
        return mapOf(
            "notificationPreferences" to user.notificationPreferences,
            "reminderEnabled" to user.reminderEnabled,
            "reminderTime" to (user.reminderTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "20:00"),
            "minCardThreshold" to (user.minCardThreshold ?: 5)
        )
    }
    
    /**
     * Get user by IDi
     */
    fun getUserById(userId: UUID): UserEntity {
        return userRepository.findById(userId)
            .orElseThrow { BusinessException("User not found with ID: $userId") }
    }
}