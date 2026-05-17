package com.example.userservice.service

import com.example.common.exception.BusinessException
import com.example.userservice.controller.UpdatePreferencesRequest
import com.example.userservice.dto.UpdateProfileRequestDto
import com.example.userservice.entity.UserEntity
import com.example.userservice.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class UserService(
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
            "profilePicture" to user.profilePicture.orEmpty(),
            "currentLevel" to user.currentLevel?.name.orEmpty(),
            "jlptGoal" to user.jlptGoal?.name.orEmpty(),
            "streakCount" to user.streakCount,
            "points" to user.points,
            "lastLogin" to user.lastLogin?.toString().orEmpty(),
            "isEmailVerified" to user.isEmailVerified,
            "createdAt" to user.createdAt.toString(),
        )
    }

    @Transactional
    fun updateNotificationPreferences(
        userId: UUID,
        request: UpdatePreferencesRequest,
    ) {
        logger.info("Updating notification preferences for user $userId")

        val user = getUserById(userId)
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val basePrefs: MutableSet<String> =
            if (request.notificationPreferences != null) {
                request.notificationPreferences
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .toMutableSet()
            } else {
                user.notificationPreferences.toMutableSet()
            }

        if (request.leechNotificationsEnabled == true) {
            basePrefs.add("leech")
        } else if (request.leechNotificationsEnabled == false) {
            basePrefs.remove("leech")
        }

        userRepository.save(
            user.copy(
                notificationPreferences = basePrefs,
                reminderEnabled = request.reminderEnabled ?: user.reminderEnabled,
                reminderTime = request.reminderTime?.let { LocalTime.parse(it, timeFormatter) } ?: user.reminderTime,
                minCardThreshold = request.minCardThreshold ?: user.minCardThreshold,
                updatedAt = LocalDateTime.now(),
            ),
        )
        logger.info("Notification preferences updated for user $userId")
    }

    fun getNotificationPreferences(userId: UUID): Map<String, Any> {
        val user = getUserById(userId)
        return mapOf(
            "notificationPreferences" to user.notificationPreferences.toList(),
            "reminderEnabled" to user.reminderEnabled,
            "reminderTime" to (user.reminderTime?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "20:00"),
            "minCardThreshold" to (user.minCardThreshold ?: 5),
        )
    }

    @Transactional
    fun updateProfile(
        userId: UUID,
        request: UpdateProfileRequestDto,
    ) {
        val user = getUserById(userId)

        if (request.currentLevel.ordinal > request.jlptGoal.ordinal) {
            throw BusinessException("Current level cannot be higher than goal level")
        }

        userRepository.save(
            user.copy(
                fullName = request.fullName,
                currentLevel = request.currentLevel,
                jlptGoal = request.jlptGoal,
                updatedAt = LocalDateTime.now(),
            ),
        )

        logger.debug("Profile updated for user: $userId")
    }

    /**
     * Get user by ID
     */
    fun getUserById(userId: UUID): UserEntity =
        userRepository
            .findById(userId)
            .orElseThrow { BusinessException("User not found with ID: $userId") }
}
