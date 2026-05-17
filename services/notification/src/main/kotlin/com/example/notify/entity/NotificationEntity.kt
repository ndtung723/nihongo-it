package com.example.notify.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "notifications")
data class NotificationEntity(
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "notification_id", updatable = false, nullable = false)
    val notificationId: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val user: UserEntity,
    @Column(name = "title", nullable = false)
    val title: String,
    @Column(name = "message", columnDefinition = "text")
    val message: String,
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    val type: NotificationType,
    @Column(name = "is_read")
    val isRead: Boolean = false,
    @Column(name = "action_url")
    val actionUrl: String? = null,
    @Column(name = "notification_channel")
    @Enumerated(EnumType.STRING)
    val notificationChannel: NotificationChannel = NotificationChannel.EMAIL,
    @Column(name = "sent_at")
    val sentAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "read_at")
    val readAt: LocalDateTime? = null,
    @Column(name = "review_count")
    val reviewCount: Int? = null, // Number of flashcards due for review
    @Column(name = "review_category")
    val reviewCategory: String? = null, // Category of items due (flashcard)
    @Column(name = "priority_level")
    val priorityLevel: Int = 0, // Priority based on FSRS algorithm (0-low, 5-high)
    @Column(name = "scheduled_for")
    val scheduledFor: LocalDateTime? = null,
    @Column(name = "external_id")
    val externalId: String? = null, // For Firebase or external notification IDs
)

/**
 * Notification types for Japanese IT vocabulary learning system
 */
enum class NotificationType {
    STUDY_REMINDER, // General study reminder
    REVIEW_DUE, // FSRS-calculated flashcard review due
    SYSTEM_ANNOUNCEMENT, // System announcements
}

/**
 * Notification delivery channels
 */
enum class NotificationChannel {
    APP, // In-app notification
    EMAIL, // Email notification
    PUSH, // Push notification via Firebase
}
