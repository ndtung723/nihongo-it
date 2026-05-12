package com.example.learningservice.service

import com.example.learningservice.repository.FlashcardRepository
import com.example.learningservice.repository.ReviewLogRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class FlashcardAdminService(
    private val flashcardRepository: FlashcardRepository,
    private val reviewLogRepository: ReviewLogRepository,
) {
    /**
     * Get total count of all flashcards in the system
     */
    fun getTotalFlashcardsCount(): Int {
        return flashcardRepository.count().toInt()
    }

    /**
     * Get average retention rate across all users
     */
    @Suppress("MagicNumber")
    fun getAverageRetentionRate(): Double {
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        val recentReviews = reviewLogRepository.findByReviewTimestampAfter(thirtyDaysAgo)

        if (recentReviews.isEmpty()) {
            return 0.0
        }

        val correctReviews = recentReviews.count { it.rating >= 3 }
        return (correctReviews.toDouble() / recentReviews.size) * 100
    }
}
