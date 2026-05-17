package com.example.learningservice.scheduled

import com.example.learningservice.repository.ReviewLogRepository
import com.example.learningservice.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Component
class StreakTrackerJob(
    private val userRepository: UserRepository,
    private val reviewLogRepository: ReviewLogRepository,
) {
    private val logger = LoggerFactory.getLogger(StreakTrackerJob::class.java)

    companion object {
        private const val COL_USER_ID = 0
        private const val COL_TODAY = 1
        private const val COL_YESTERDAY = 2
        private const val COL_TWO_DAYS_AGO = 3
    }

    @Scheduled(cron = "0 59 23 * * *")
    @Transactional
    fun resetStreaks() {
        logger.info("Running streak reset job at ${LocalDateTime.now()}")

        val today = LocalDate.now()
        val startOfToday = today.atStartOfDay()
        val startOfYesterday = today.minusDays(1).atStartOfDay()
        val startOfTwoDaysAgo = today.minusDays(2).atStartOfDay()

        val allUsers = userRepository.findByIsActiveTrueAndStreakCountGreaterThan(0)
        if (allUsers.isEmpty()) {
            logger.info("No active users with positive streak — skipping")
            return
        }

        val userIds = allUsers.mapNotNull { it.userId }

        // Single aggregate query instead of 3 queries × N users
        val activityRows =
            reviewLogRepository.getUserActivitySummary(
                userIds,
                startOfToday,
                startOfYesterday,
                startOfTwoDaysAgo,
            )
        val activityMap: Map<UUID, Triple<Boolean, Boolean, Boolean>> =
            activityRows.associate { row ->
                val uid = UUID.fromString(row[COL_USER_ID].toString())
                val reviewedToday = (row[COL_TODAY] as Number).toInt() > 0
                val reviewedYesterday = (row[COL_YESTERDAY] as Number).toInt() > 0
                val reviewedTwoDaysAgo = (row[COL_TWO_DAYS_AGO] as Number).toInt() > 0
                uid to Triple(reviewedToday, reviewedYesterday, reviewedTwoDaysAgo)
            }

        val now = LocalDateTime.now()
        var resetCount = 0
        var reducedCount = 0

        val updatedUsers =
            allUsers.mapNotNull { user ->
                val userId = user.userId ?: return@mapNotNull null
                val (reviewedToday, reviewedYesterday, reviewedTwoDaysAgo) =
                    activityMap[userId]
                        ?: Triple(false, false, false)

                when {
                    reviewedToday || reviewedYesterday -> null
                    reviewedTwoDaysAgo && user.streakCount > 1 -> {
                        reducedCount++
                        user.copy(streakCount = user.streakCount - 1, updatedAt = Instant.now())
                    }
                    else -> {
                        resetCount++
                        user.copy(streakCount = 0, updatedAt = Instant.now())
                    }
                }
            }

        if (updatedUsers.isNotEmpty()) {
            userRepository.saveAll(updatedUsers)
        }

        logger.info(
            "Streak job done: processed ${allUsers.size} users, " +
                "reset $resetCount, reduced $reducedCount",
        )
    }
}
