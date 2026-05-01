package com.example.learningservice.scheduled

import com.example.learningservice.repository.UserRepository
import com.example.learningservice.repository.ReviewLogRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalDate

/**
 * Scheduled job to track and update user streaks
 * This job runs every day at midnight to check which users have
 * maintained their streak and which ones need to be reset
 */
@Component
class StreakTrackerJob(
    private val userRepository: UserRepository,
    private val reviewLogRepository: ReviewLogRepository
) {
    private val logger = LoggerFactory.getLogger(StreakTrackerJob::class.java)
    
    /**
     * Reset streak counts for users who haven't completed reviews in the past 48 hours
     * Runs daily at 11:59 PM
     * Hiệu chỉnh: Cho phép bỏ lỡ 1 ngày và bảo vệ streak
     */
    @Scheduled(cron = "0 59 23 * * *") // Run at 11:59 PM every day
    @Transactional
    fun resetStreaks() {
        logger.info("==================== STREAK RESET JOB START ====================")
        logger.info("Running streak reset job at ${LocalDateTime.now()}")
        
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        
        val startOfYesterday = yesterday.atStartOfDay()
        val endOfYesterday = yesterday.plusDays(1).atStartOfDay().minusSeconds(1)
        
        val startOfTwoDaysAgo = twoDaysAgo.atStartOfDay()
        val endOfTwoDaysAgo = twoDaysAgo.plusDays(1).atStartOfDay().minusSeconds(1)
        
        logger.info("Today: $today")
        logger.info("Yesterday: $yesterday (${startOfYesterday} to ${endOfYesterday})")
        logger.info("Two days ago: $twoDaysAgo (${startOfTwoDaysAgo} to ${endOfTwoDaysAgo})")
        
        // Get all active users with streak > 0
        val allUsers = userRepository.findByIsActiveTrueAndStreakCountGreaterThan(0)
        logger.info("Found ${allUsers.size} active users with positive streak")
        
        // Process each user
        var resetCount = 0
        var reducedCount = 0
        
        allUsers.forEach { user ->
            logger.info("Processing user ${user.email} (${user.userId}) with current streak: ${user.streakCount}")
            
            // Check if user studied yesterday
            val yesterdayReviews = reviewLogRepository.findByUserIdAndReviewTimestampBetween(
                user.userId!!, startOfYesterday, endOfYesterday
            )
            
            // Check if user studied two days ago
            val twoDaysAgoReviews = reviewLogRepository.findByUserIdAndReviewTimestampBetween(
                user.userId!!, startOfTwoDaysAgo, endOfTwoDaysAgo
            )
            
            // Check today's reviews as well (to handle edge cases)
            val startOfToday = today.atStartOfDay()
            val todayReviews = reviewLogRepository.findByUserIdAndReviewTimestampAfter(
                user.userId!!, startOfToday
            )
            
            logger.info("User activity: Today=${todayReviews.size} reviews, Yesterday=${yesterdayReviews.size} reviews, Two days ago=${twoDaysAgoReviews.size} reviews")
            
            // Handle streak logic with protected buffer
            val updatedUser = when {
                // If reviewed today, keep streak intact (job may run before user's usual study time)
                todayReviews.isNotEmpty() -> {
                    logger.info("User has reviewed today - keeping streak intact")
                    null // No change needed
                }
                
                // If reviewed yesterday, keep streak intact
                yesterdayReviews.isNotEmpty() -> {
                    logger.info("User reviewed yesterday - keeping streak intact")
                    null // No change needed
                }
                
                // If missed yesterday but reviewed two days ago, reduce streak by 1 but don't reset
                // This gives a 1-day buffer to protect streaks (like Duolingo's streak freeze)
                twoDaysAgoReviews.isNotEmpty() && user.streakCount > 1 -> {
                    val newStreak = user.streakCount - 1
                    logger.info("User missed yesterday but reviewed two days ago - reducing streak from ${user.streakCount} to $newStreak")
                    reducedCount++
                    user.copy(
                        streakCount = newStreak,
                        updatedAt = LocalDateTime.now()
                    )
                }
                
                // If missed both yesterday and day before, reset streak
                else -> {
                    logger.info("User has not reviewed in the past 48 hours - resetting streak from ${user.streakCount} to 0")
                    resetCount++
                    user.copy(
                        streakCount = 0,
                        updatedAt = LocalDateTime.now()
                    )
                }
            }
            
            // Save changes if needed
            if (updatedUser != null) {
                userRepository.save(updatedUser)
                logger.info("Updated streak for user ${user.email}: ${user.streakCount} -> ${updatedUser.streakCount}")
            }
        }
        
        logger.info("Streak job summary: Processed ${allUsers.size} users, Reset $resetCount streaks, Reduced $reducedCount streaks")
        logger.info("==================== STREAK RESET JOB END ====================")
    }
} 