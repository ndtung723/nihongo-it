package com.example.learningservice.repository

import com.example.learningservice.entity.ConversationLineEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ConversationLineRepository : JpaRepository<ConversationLineEntity, UUID> {
    /**
     * Delete lines by conversation ID
     */
    @Modifying
    @Query("DELETE FROM ConversationLineEntity c WHERE c.conversation.convId = :conversationId")
    fun deleteByConversationId(conversationId: UUID)
}
