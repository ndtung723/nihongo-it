package com.example.learningservice.repository

import com.example.learningservice.entity.ConversationEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConversationRepository : JpaRepository<ConversationEntity, UUID> {
    /**
     * Find conversations by title or description containing the query (case insensitive)
     */
    fun findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        title: String,
        description: String,
        pageable: Pageable,
    ): Page<ConversationEntity>

    /**
     * Find conversations by JLPT level
     */
    fun findByJlptLevel(level: String, pageable: Pageable): Page<ConversationEntity>
}
