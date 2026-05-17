package com.example.learningservice.service

import com.example.learningservice.dto.GetStatisticsResponseDto
import com.example.learningservice.dto.ReviewTrendDto
import com.example.learningservice.dto.StudyStatisticsDto
import com.example.learningservice.dto.StudySummaryDto
import com.example.learningservice.entity.ReviewLogEntity
import com.example.learningservice.repository.FlashcardRepository
import com.example.learningservice.repository.ReviewLogRepository
import com.example.learningservice.repository.UserRepository
import com.example.learningservice.util.UserAuthUtil
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class FlashcardStatisticsService(
    private val flashcardRepository: FlashcardRepository,
    private val reviewLogRepository: ReviewLogRepository,
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val userAuthUtil: UserAuthUtil,
) {
    private val logger = LoggerFactory.getLogger(FlashcardStatisticsService::class.java)

    // Get study statistics
    @Suppress("LongMethod", "MagicNumber")
    fun getStudyStatistics(): GetStatisticsResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Getting study statistics for user: $userId")

        val now = LocalDateTime.now()
        val allUserCards = flashcardRepository.findByUser_UserId(requireNotNull(userId) { "User not authenticated" })
        val totalCards = allUserCards.size
        val dueCardsNow = allUserCards.count { it.due <= now }

        // Calculate cards due by day for the next 7 days — in-memory, no extra DB calls
        val dueCardsByDate = allUserCards.groupingBy { it.due.toLocalDate() }.eachCount()
        val cardsDueByDay = mutableMapOf<String, Int>()
        for (i in 0..6) {
            val date = now.plusDays(i.toLong()).toLocalDate()
            cardsDueByDay[date.toString()] =
                if (i == 0) {
                    allUserCards.count { !it.due.toLocalDate().isAfter(date) }
                } else {
                    dueCardsByDate[date] ?: 0
                }
        }

        // Get review history
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        val recentReviews = reviewLogRepository.findByUserIdAndReviewTimestampAfter(userId, thirtyDaysAgo)

        // Daily review counts
        val dailyReviews =
            recentReviews
                .groupBy { it.reviewTimestamp.toLocalDate().toString() }
                .mapValues { it.value.size }

        // Retention rate by day
        val retentionRateByDay =
            recentReviews
                .groupBy { it.reviewTimestamp.toLocalDate().toString() }
                .mapValues { entry ->
                    val dayReviews = entry.value
                    val correctCount = dayReviews.count { it.rating >= 3 }
                    if (dayReviews.isNotEmpty()) {
                        (correctCount.toDouble() / dayReviews.size) * 100.0
                    } else {
                        0.0
                    }
                }

        // Overall retention rate
        val correctReviews = recentReviews.count { it.rating >= 3 }
        val overallRetentionRate =
            if (recentReviews.isNotEmpty()) {
                (correctReviews.toDouble() / recentReviews.size) * 100.0
            } else {
                0.0
            }

        // Memory strength distribution
        val memoryStrengthDistribution =
            allUserCards
                .groupBy {
                    val stab = it.stability ?: 0.0
                    when {
                        stab == 0.0 -> "new"
                        stab <= 1.0 -> "weak"
                        stab <= 10.0 -> "medium"
                        else -> "strong"
                    }
                }.mapValues { it.value.size }

        // Get cards by state
        val cardsByState =
            allUserCards
                .groupBy {
                    FSRSService.State.entries
                        .find { state -> state.value == it.state }
                        ?.name
                        ?.lowercase() ?: "unknown"
                }.mapValues { it.value.size }

        // JLPT level statistics (if vocabulary is available)
        val cardsByJlptLevel =
            allUserCards
                .filter { it.vocabulary != null }
                .groupBy { it.vocabulary?.jlptLevel ?: "unknown" }
                .mapValues { it.value.size }

        val statistics =
            StudyStatisticsDto(
                summary =
                    StudySummaryDto(
                        totalCards = totalCards,
                        dueCardsNow = dueCardsNow,
                        reviewsLast30Days = recentReviews.size,
                        currentStreak = userService.getUserById(userId).streakCount,
                        overallRetentionRate = overallRetentionRate,
                    ),
                cardsDueByDay = cardsDueByDay,
                dailyReviews = dailyReviews,
                retentionRateByDay = retentionRateByDay,
                memoryStrengthDistribution = memoryStrengthDistribution,
                cardsByState = cardsByState,
                cardsByJlptLevel = cardsByJlptLevel.mapKeys { it.key.toString() },
                reviewTrend = calculateReviewTrend(recentReviews),
                averageRating = if (recentReviews.isNotEmpty()) recentReviews.map { it.rating }.average() else 0.0,
            )

        return GetStatisticsResponseDto(data = statistics)
    }

    /**
     * Get flashcard statistics for a specific user
     */
    @Suppress("LongMethod", "MagicNumber")
    fun getUserFlashcardStatistics(userId: UUID): StudyStatisticsDto {
        logger.info("Getting flashcard statistics for user: $userId")

        val user =
            userRepository
                .findById(userId)
                .orElseThrow { EntityNotFoundException("User not found with id: $userId") }

        val allFlashcards = flashcardRepository.findByUser_UserId(userId)
        if (allFlashcards.isEmpty()) {
            return StudyStatisticsDto(
                summary = StudySummaryDto(0, 0, 0, user.streakCount, 0.0),
                cardsDueByDay = emptyMap(),
                dailyReviews = emptyMap(),
                retentionRateByDay = emptyMap(),
                memoryStrengthDistribution = mapOf("new" to 0, "weak" to 0, "medium" to 0, "strong" to 0),
                cardsByState = emptyMap(),
                cardsByJlptLevel = emptyMap(),
                reviewTrend = ReviewTrendDto("neutral", 0.0),
                averageRating = 0.0,
            )
        }

        val now = LocalDateTime.now()
        val thirtyDaysAgo = now.minusDays(30)
        val dueCardsNow = allFlashcards.count { it.due <= now }

        val recentReviews =
            reviewLogRepository.findRecentReviewsByUser(
                userId,
                thirtyDaysAgo,
            )

        val correctReviews = recentReviews.count { it.rating >= 3 }
        val overallRetentionRate =
            if (recentReviews.isNotEmpty()) {
                (correctReviews.toDouble() / recentReviews.size) * 100
            } else {
                0.0
            }

        val cardsByState =
            allFlashcards
                .groupBy {
                    FSRSService.State.entries
                        .find { s -> s.value == it.state }
                        ?.name
                        ?.lowercase() ?: "new"
                }.mapValues { it.value.size }

        val cardsByJlptLevel =
            allFlashcards
                .mapNotNull { it.vocabulary?.jlptLevel }
                .groupBy { it.toString() }
                .mapValues { it.value.size }

        val dueCardsByDate = allFlashcards.groupingBy { it.due.toLocalDate() }.eachCount()
        val cardsDueByDay =
            (0..29).associate { day ->
                val dueDate = now.plusDays(day.toLong()).toLocalDate()
                dueDate.toString() to (dueCardsByDate[dueDate] ?: 0)
            }

        val dailyReviews =
            recentReviews
                .groupBy { it.reviewTimestamp.toLocalDate().toString() }
                .mapValues { it.value.size }

        val retentionRateByDay =
            recentReviews
                .groupBy { it.reviewTimestamp.toLocalDate().toString() }
                .mapValues { entry ->
                    val dayReviews = entry.value
                    val dayCorrect = dayReviews.count { it.rating >= 3 }
                    (dayCorrect.toDouble() / dayReviews.size) * 100
                }

        val memoryStrengthDistribution =
            mapOf(
                "new" to allFlashcards.count { (it.stability ?: 0.0) == 0.0 },
                "weak" to
                    allFlashcards.count {
                        val s = it.stability ?: 0.0
                        s > 0.0 && s < 1.0
                    },
                "medium" to
                    allFlashcards.count {
                        val s = it.stability ?: 0.0
                        s >= 1.0 && s < 10.0
                    },
                "strong" to allFlashcards.count { (it.stability ?: 0.0) >= 10.0 },
            )

        return StudyStatisticsDto(
            summary =
                StudySummaryDto(
                    totalCards = allFlashcards.size,
                    dueCardsNow = dueCardsNow,
                    reviewsLast30Days = recentReviews.size,
                    currentStreak = user.streakCount,
                    overallRetentionRate = overallRetentionRate,
                ),
            cardsByState = cardsByState,
            cardsByJlptLevel = cardsByJlptLevel,
            dailyReviews = dailyReviews,
            retentionRateByDay = retentionRateByDay,
            memoryStrengthDistribution = memoryStrengthDistribution,
            cardsDueByDay = cardsDueByDay,
            reviewTrend = calculateReviewTrend(recentReviews),
            averageRating = if (recentReviews.isNotEmpty()) recentReviews.map { it.rating }.average() else 0.0,
        )
    }

    /**
     * Get the last review date for a user
     */
    @Transactional(readOnly = true)
    fun getLastReviewDate(userId: UUID): LocalDateTime? =
        reviewLogRepository
            .findFirstByUserIdOrderByReviewTimestampDesc(userId)
            ?.reviewTimestamp

    /**
     * Get review history for a user
     */
    @Transactional(readOnly = true)
    fun getUserReviewHistory(
        userId: UUID,
        days: Int,
    ): List<Map<String, Any>> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        val reviews =
            reviewLogRepository.findRecentReviewsByUser(
                userId,
                startDate,
            )

        return reviews.map { review ->
            val map =
                mutableMapOf<String, Any>(
                    ("reviewId" to review.reviewLogId) as Pair<String, Any>,
                    ("flashcardId" to review.flashcard.flashcardId) as Pair<String, Any>,
                    "rating" to review.rating,
                    "elapsedDays" to review.elapsedDays,
                    "scheduledDays" to review.scheduledDays,
                    "state" to review.state,
                    "timestamp" to review.reviewTimestamp.format(DateTimeFormatter.ISO_DATE_TIME),
                )

            // Handle nullable vocabulary separately
            review.flashcard.vocabulary?.let { vocab ->
                map["vocabulary"] =
                    mapOf(
                        "vocabId" to vocab.vocabId,
                        "term" to vocab.term,
                        "meaning" to vocab.meaning,
                        "jlptLevel" to vocab.jlptLevel,
                    )
            } ?: run {
                map["vocabulary"] = emptyMap<String, Any>()
            }

            map
        }
    }

    @Suppress("MagicNumber")
    private fun calculateReviewTrend(reviews: List<ReviewLogEntity>): ReviewTrendDto {
        if (reviews.isEmpty()) return ReviewTrendDto("neutral", 0.0)

        val twoWeeksAgo = LocalDateTime.now().minusDays(14)
        val oneWeekAgo = LocalDateTime.now().minusDays(7)

        val previousWeekCount =
            reviews.count {
                it.reviewTimestamp.isAfter(twoWeeksAgo) && it.reviewTimestamp.isBefore(oneWeekAgo)
            }
        val currentWeekCount = reviews.count { it.reviewTimestamp.isAfter(oneWeekAgo) }

        val trend =
            when {
                previousWeekCount == 0 -> "up"
                currentWeekCount > previousWeekCount -> "up"
                currentWeekCount < previousWeekCount -> "down"
                else -> "neutral"
            }

        val percentage =
            when {
                previousWeekCount > 0 -> ((currentWeekCount - previousWeekCount).toDouble() / previousWeekCount) * 100.0
                currentWeekCount > 0 -> 100.0
                else -> 0.0
            }

        return ReviewTrendDto(trend, percentage)
    }
}
