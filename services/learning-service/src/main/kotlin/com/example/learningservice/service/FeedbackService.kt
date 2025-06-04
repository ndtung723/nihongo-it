package com.example.learningservice.service

import com.example.learningservice.dto.FeedbackDTO
import com.example.learningservice.entity.FeedbackEntity
import com.example.learningservice.repository.FeedbackRepository
import com.example.learningservice.repository.UserRepository
import com.example.learningservice.exception.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val userRepository: UserRepository
) {
    @Transactional
    fun saveFeedback(feedbackDTO: FeedbackDTO): FeedbackDTO {
        // Find the user entity
        val userEntity = userRepository.findById(feedbackDTO.userId)
            .orElseThrow { BusinessException("User not found with id: ${feedbackDTO.userId}") }

        // Create feedback entity
        val feedbackEntity = FeedbackEntity(
            feedbackId = null,  // Let the database generate the ID
            user = userEntity,
            contentType = feedbackDTO.contentType,
            contentId = feedbackDTO.contentId,
            content = feedbackDTO.content,
            createdAt = LocalDateTime.now()
        )

        // Save to repository
        val savedEntity = feedbackRepository.save(feedbackEntity)

        // Convert back to DTO
        return FeedbackDTO(
            feedbackId = savedEntity.feedbackId,
            userId = savedEntity.user.userId!!,
            contentType = savedEntity.contentType,
            contentId = savedEntity.contentId,
            content = savedEntity.content,
            createdAt = savedEntity.createdAt
        )
    }
} 