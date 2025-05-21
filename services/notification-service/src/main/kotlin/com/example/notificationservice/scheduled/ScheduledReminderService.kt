package com.example.notificationservice.scheduled

import com.example.notificationservice.entity.UserEntity
import com.example.notificationservice.repository.FlashcardRepository
import com.example.notificationservice.repository.UserRepository
import com.example.notificationservice.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Service for scheduling and sending reminder notifications for flashcard review
 */
@Service
class ScheduledReminderService @Autowired constructor(
    private val userRepository: UserRepository,
    private val flashcardRepository: FlashcardRepository,
    private val notificationService: NotificationService
) {
    private val logger = LoggerFactory.getLogger(ScheduledReminderService::class.java)
    /**
     * Base URL for the frontend application, used for generating action URLs in notifications
     */
    @Value("\${app.frontend-url:http://localhost:5173}")
    private lateinit var baseFrontendUrl: String
    /**
     * Scheduled task that runs every 10 seconds to check for users who need flashcard review reminders
     * Note: This is a testing frequency; change to a longer interval for production use
     */
    @Scheduled(fixedDelay = 3 * 60 * 1000)
    @Transactional
    fun checkAndSendFlashcardReminders() {
        try {
            logger.info("=== SCHEDULER: Starting flashcard reminder check at ${LocalDateTime.now()} ===")
            
            val now = LocalDateTime.now()
            val currentTime = now.toLocalTime()

            // Find all active users with reminder enabled
            val users = userRepository.findByIsActiveAndReminderEnabled(true, true)
            logger.info("SCHEDULER: Found ${users.size} active users with reminders enabled")

            if (users.isEmpty()) {
                logger.info("SCHEDULER: No users with reminders enabled. Skipping check.")
                return
            }

            // Process each user
            var remindersSent = 0
            users.forEach { user -> 
                val result = processUserReminderWithLogging(user, now, currentTime)
                if (result) remindersSent++
            }

            logger.info("=== SCHEDULER: Completed reminder check. Sent $remindersSent notifications out of ${users.size} users ===")
        } catch (e: Exception) {
            logger.error("SCHEDULER ERROR: Failed to process scheduled reminders", e)
        }
    }

    /**
     * Process reminders for a single user with detailed logging
     * @return true if a notification was sent, false otherwise
     */
    private fun processUserReminderWithLogging(user: UserEntity, now: LocalDateTime, currentTime: LocalTime): Boolean {
        try {
            logger.debug("SCHEDULER: Processing reminders for user ${user.userId} (${user.email})")
            
            // Check if it's time to send a reminder for this user
            if (!isReminderTime(user.reminderTime, currentTime)) {
                logger.debug("SCHEDULER: Not reminder time for user ${user.userId}. Current: $currentTime, Reminder: ${user.reminderTime}")
                return false
            }
            
            logger.debug("SCHEDULER: It's reminder time for user ${user.userId}")
            
            // Get due cards for this user
            val dueCards = flashcardRepository.findDueCards(user.userId!!, now)
            logger.debug("SCHEDULER: User ${user.userId} has ${dueCards.size} due cards")
            
            // Only send reminder if there are due cards
            if (dueCards.isEmpty()) {
                logger.debug("SCHEDULER: No due cards for user ${user.userId}")
                return false
            }
            
            // Check for minimum card threshold
            val threshold = user.minCardThreshold ?: 1
            if (dueCards.size < threshold) {
                logger.debug("SCHEDULER: User ${user.userId} has ${dueCards.size} cards, below threshold of $threshold")
                return false
            }
            
            // Create action URL for the notification
            val actionUrl = "/flashcards/study"
            
            // Send notification
            logger.info("SCHEDULER: Sending reminder to user ${user.userId} for ${dueCards.size} due cards")
            
            // If email notification is enabled for this user
            if (user.notificationPreferences.contains("email")) {
                // Get the base URL from environment or config
                val baseUrl = baseFrontendUrl // or fetch from config
                val fullActionUrl = "$baseUrl$actionUrl"
                
                // Send specialized flashcard reminder email
                notificationService.sendFlashcardReminderEmail(
                    to = user.email,
                    cardCount = dueCards.size,
                    actionUrl = fullActionUrl
                )
            }

            return true
        } catch (e: Exception) {
            logger.error("SCHEDULER ERROR: Failed to process reminder for user ${user.userId}", e)
            return false
        }
    }

    /**
     * Check if the current time matches the user's reminder time (within a 15-minute window)
     */
    private fun isReminderTime(reminderTime: LocalTime?, currentTime: LocalTime): Boolean {
        if (reminderTime == null) return false
        
        // Check if current time is within 15 minutes of the reminder time
        val minuteDifference = ChronoUnit.MINUTES.between(
            reminderTime, 
            currentTime
        ).let { if (it < 0) it + 24 * 60 else it } // Handle wrapping around midnight

//        return minuteDifference < 15
        return minuteDifference < 15
    }
} 