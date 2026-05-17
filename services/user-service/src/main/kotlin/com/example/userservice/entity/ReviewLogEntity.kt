package com.example.userservice.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * Entity to track each review of a flashcard, based on the Go-FSRS ReviewLog structure
 */
@Entity
@Table(name = "review_logs")
data class ReviewLogEntity(
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "review_log_id", updatable = false, nullable = false)
    val reviewLogId: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flashcard_id")
    val flashcard: FlashcardEntity,
    @Column(name = "user_id")
    val userId: UUID,
    @Column(name = "rating")
    val rating: Int, // 1-4 scale (Again, Hard, Good, Easy)
    @Column(name = "scheduled_days")
    val scheduledDays: Double, // Days scheduled for next review
    @Column(name = "elapsed_days")
    val elapsedDays: Double, // Days elapsed since due date
    @Column(name = "review_timestamp")
    val reviewTimestamp: LocalDateTime = LocalDateTime.now(), // When the review happened
    @Column(name = "state")
    val state: Int, // Card state when reviewed (0=New, 1=Learning, 2=Review, 3=Relearning)
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
