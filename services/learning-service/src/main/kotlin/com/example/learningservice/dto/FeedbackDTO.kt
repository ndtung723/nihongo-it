package com.example.learningservice.dto

import java.time.LocalDateTime
import java.util.UUID

data class FeedbackDTO(
    val feedbackId: UUID? = null,
    val userId: UUID,
    val contentType: String, // 'conversation', 'vocabulary'
    val contentId: UUID,
    val content: String?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
