package com.example.learningservice.service

import com.example.common.exception.BusinessException
import com.example.learningservice.dto.FeedbackDTO
import com.example.learningservice.entity.FeedbackEntity
import com.example.learningservice.repository.FeedbackRepository
import com.example.learningservice.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun saveFeedback(feedbackDTO: FeedbackDTO): FeedbackDTO {
        // Find the user entity
        val userEntity =
            userRepository
                .findById(feedbackDTO.userId)
                .orElseThrow { BusinessException("User not found with id: ${feedbackDTO.userId}") }

        // Create feedback entity
        val feedbackEntity =
            FeedbackEntity(
                feedbackId = null, // Let the database generate the ID
                user = userEntity,
                contentType = feedbackDTO.contentType,
                contentId = feedbackDTO.contentId,
                content = feedbackDTO.content,
                createdAt = LocalDateTime.now(),
            )

        // Save to repository
        val savedEntity = feedbackRepository.save(feedbackEntity)

        // Convert back to DTO
        return FeedbackDTO(
            feedbackId = savedEntity.feedbackId,
            userId = requireNotNull(savedEntity.user.userId) { "User ID missing" },
            contentType = savedEntity.contentType,
            contentId = savedEntity.contentId,
            content = savedEntity.content,
            createdAt = savedEntity.createdAt,
        )
    }
}
