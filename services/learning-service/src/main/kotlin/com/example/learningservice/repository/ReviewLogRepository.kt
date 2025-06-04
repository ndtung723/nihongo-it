package com.example.learningservice.repository

import com.example.learningservice.entity.ReviewLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface ReviewLogRepository : JpaRepository<ReviewLogEntity, UUID> {
    fun findByUserIdAndReviewTimestampAfter(userId: UUID, after: LocalDateTime): List<ReviewLogEntity>
    
    @Query("SELECT COUNT(DISTINCT r.flashcard.id) FROM ReviewLogEntity r WHERE r.reviewTimestamp > :after")
    fun countDistinctFlashcardIdByReviewTimestampAfter(@Param("after") after: LocalDateTime): Long
    
    @Query("""
        SELECT CAST(CAST(review_timestamp AS DATE) AS VARCHAR) as date, 
               COUNT(review_log_id) as count,
               AVG(CAST(rating AS FLOAT)) as avg_rating
        FROM review_logs
        WHERE user_id = :userId AND review_timestamp BETWEEN :startDate AND :endDate
        GROUP BY CAST(review_timestamp AS DATE)
        ORDER BY CAST(review_timestamp AS DATE)
    """, nativeQuery = true)
    fun getUserReviewStats(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<Array<Any>>

    // Find reviews by user ID and date range
    fun findByUserIdAndReviewTimestampAfterOrderByReviewTimestampDesc(
        userId: UUID, 
        startDate: LocalDateTime
    ): List<ReviewLogEntity>
    
    // Find most recent review by user ID
    fun findFirstByUserIdOrderByReviewTimestampDesc(userId: UUID): ReviewLogEntity?
    
    // Count reviews by user ID and date range
    fun countByUserIdAndReviewTimestampAfter(userId: UUID, startDate: LocalDateTime): Int
    
    // Find reviews after a specific date
    fun findByReviewTimestampAfter(startDate: LocalDateTime): List<ReviewLogEntity>
    
    // Count reviews by day for a user
    @Query("SELECT CAST(r.reviewTimestamp as date) as reviewDate, COUNT(r) " +
           "FROM ReviewLogEntity r " +
           "WHERE r.userId = :userId AND r.reviewTimestamp > :startDate " +
           "GROUP BY CAST(r.reviewTimestamp as date) " +
           "ORDER BY reviewDate DESC")
    fun countReviewsByDayForUser(
        @Param("userId") userId: UUID,
        @Param("startDate") startDate: LocalDateTime
    ): List<Array<Any>>
    
    fun findTopByUserIdOrderByReviewTimestampDesc(userId: UUID): ReviewLogEntity?

    // Find reviews by user ID and date range (between two timestamps)
    fun findByUserIdAndReviewTimestampBetween(
        userId: UUID,
        startTimestamp: LocalDateTime,
        endTimestamp: LocalDateTime
    ): List<ReviewLogEntity>
} 