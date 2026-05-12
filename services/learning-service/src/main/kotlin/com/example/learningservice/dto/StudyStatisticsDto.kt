package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class StudySummaryDto(
    @JsonProperty("totalCards") val totalCards: Int,
    @JsonProperty("dueCardsNow") val dueCardsNow: Int,
    @JsonProperty("reviewsLast30Days") val reviewsLast30Days: Int,
    @JsonProperty("currentStreak") val currentStreak: Int,
    @JsonProperty("overallRetentionRate") val overallRetentionRate: Double,
)

data class ReviewTrendDto(
    @JsonProperty("trend") val trend: String,
    @JsonProperty("percentage") val percentage: Double,
)

data class StudyStatisticsDto(
    @JsonProperty("summary") val summary: StudySummaryDto,
    @JsonProperty("cardsDueByDay") val cardsDueByDay: Map<String, Int>,
    @JsonProperty("dailyReviews") val dailyReviews: Map<String, Int>,
    @JsonProperty("retentionRateByDay") val retentionRateByDay: Map<String, Double>,
    @JsonProperty("memoryStrengthDistribution") val memoryStrengthDistribution: Map<String, Int>,
    @JsonProperty("cardsByState") val cardsByState: Map<String, Int>,
    @JsonProperty("cardsByJlptLevel") val cardsByJlptLevel: Map<String, Int>,
    @JsonProperty("reviewTrend") val reviewTrend: ReviewTrendDto,
    @JsonProperty("averageRating") val averageRating: Double,
)
