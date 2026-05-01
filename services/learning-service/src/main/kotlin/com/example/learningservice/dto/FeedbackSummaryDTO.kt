package com.example.learningservice.dto

import java.util.UUID

data class FeedbackSummaryDTO(
    val contentId: UUID,
    val contentType: String,
    val summary: String,
    val commonErrors: List<String>,
    val improvementTips: List<String>,
    val attempts: Int,
    val avgScore: Double,
    val maxScore: Int
) 