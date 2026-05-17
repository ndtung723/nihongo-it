package com.example.learningservice.repository

import com.example.learningservice.entity.ReviewLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
@Suppress("TooManyFunctions")
interface ReviewLogRepository : JpaRepository<ReviewLogEntity, UUID> {
    fun findByUserIdAndReviewTimestampAfter(
        userId: UUID,
        after: LocalDateTime,
    ): List<ReviewLogEntity>

    @Query("SELECT COUNT(DISTINCT r.flashcard.id) FROM ReviewLogEntity r WHERE r.reviewTimestamp > :after")
    fun countDistinctFlashcardIdByReviewTimestampAfter(
        @Param("after") after: LocalDateTime,
    ): Long

    @Query(
        """
        SELECT CAST(CAST(review_timestamp AS DATE) AS VARCHAR) as date,
               COUNT(review_log_id) as count,
               AVG(CAST(rating AS FLOAT)) as avg_rating
        FROM review_logs
        WHERE user_id = :userId AND review_timestamp BETWEEN :startDate AND :endDate
        GROUP BY CAST(review_timestamp AS DATE)
        ORDER BY CAST(review_timestamp AS DATE)
    """,
        nativeQuery = true,
    )
    fun getUserReviewStats(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): List<Array<Any>>

    // Find reviews by user ID after a date, ordered by timestamp desc
    @Query("SELECT r FROM ReviewLogEntity r WHERE r.userId = :userId AND r.reviewTimestamp > :startDate ORDER BY r.reviewTimestamp DESC")
    fun findRecentReviewsByUser(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
    ): List<ReviewLogEntity>

    // Find most recent review by user ID
    fun findFirstByUserIdOrderByReviewTimestampDesc(userId: UUID): ReviewLogEntity?

    // Count reviews by user ID and date range
    fun countByUserIdAndReviewTimestampAfter(
        userId: UUID,
        startDate: LocalDateTime,
    ): Int

    // Find reviews after a specific date
    fun findByReviewTimestampAfter(startDate: LocalDateTime): List<ReviewLogEntity>

    // Count reviews by day for a user
    @Query(
        "SELECT CAST(r.reviewTimestamp as date) as reviewDate, COUNT(r) " +
            "FROM ReviewLogEntity r " +
            "WHERE r.userId = :userId AND r.reviewTimestamp > :startDate " +
            "GROUP BY CAST(r.reviewTimestamp as date) " +
            "ORDER BY reviewDate DESC",
    )
    fun countReviewsByDayForUser(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
    ): List<Array<Any>>

    fun findTopByUserIdOrderByReviewTimestampDesc(userId: UUID): ReviewLogEntity?

    // Find reviews by user ID and date range (between two timestamps)
    fun findByUserIdAndReviewTimestampBetween(
        userId: UUID,
        startTimestamp: LocalDateTime,
        endTimestamp: LocalDateTime,
    ): List<ReviewLogEntity>

    // Single aggregate query for StreakTrackerJob: replaces 3 queries per user with 1 query for all users
    @Query(
        value = """
            SELECT user_id,
                MAX(CASE WHEN review_timestamp >= :startOfToday THEN 1 ELSE 0 END) AS reviewed_today,
                MAX(CASE WHEN review_timestamp >= :startOfYesterday AND review_timestamp < :startOfToday THEN 1 ELSE 0 END) AS reviewed_yesterday,
                MAX(CASE WHEN review_timestamp >= :startOfTwoDaysAgo AND review_timestamp < :startOfYesterday THEN 1 ELSE 0 END) AS reviewed_two_days_ago
            FROM review_logs
            WHERE user_id IN :userIds AND review_timestamp >= :startOfTwoDaysAgo
            GROUP BY user_id
        """,
        nativeQuery = true,
    )
    fun getUserActivitySummary(
        @Param("userIds") userIds: List<UUID>,
        @Param("startOfToday") startOfToday: LocalDateTime,
        @Param("startOfYesterday") startOfYesterday: LocalDateTime,
        @Param("startOfTwoDaysAgo") startOfTwoDaysAgo: LocalDateTime,
    ): List<Array<Any>>
}
