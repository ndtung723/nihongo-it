package com.example.learningservice.repository

import com.example.learningservice.entity.FeedbackEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FeedbackRepository : JpaRepository<FeedbackEntity, UUID> {
    fun findByContentTypeAndContentId(contentType: String, contentId: UUID): List<FeedbackEntity>
    
    fun findByUserUserIdAndContentType(userId: UUID, contentType: String): List<FeedbackEntity>
    
    fun findByContentTypeAndContentIdAndUserUserId(contentType: String, contentId: UUID, userId: UUID): List<FeedbackEntity>
} 