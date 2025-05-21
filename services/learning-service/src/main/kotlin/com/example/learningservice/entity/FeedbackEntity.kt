package com.example.learningservice.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "feedback")
data class FeedbackEntity(
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "feedback_id", updatable = false, nullable = false)
    val feedbackId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    val user: UserEntity,

    @Column(name = "content_type", nullable = false)
    val contentType: String, // 'conversation', 'grammar', 'vocabulary'

    @Column(name = "content_id", nullable = false)
    val contentId: UUID,

    @Column(name = "recording_url")
    val recordingUrl: String?,

    @Column(name = "ai_feedback", columnDefinition = "text")
    val aiFeedback: String?,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "status", nullable = false)
    val status: String = "unread" // 'unread', 'read'
) 