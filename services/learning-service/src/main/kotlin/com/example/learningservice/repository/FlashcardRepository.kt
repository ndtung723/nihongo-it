package com.example.learningservice.repository

import com.example.learningservice.entity.FlashcardEntity
import com.example.learningservice.entity.UserEntity
import com.example.learningservice.entity.VocabularyEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface FlashcardRepository : JpaRepository<FlashcardEntity, UUID> {
    @Query("SELECT f FROM FlashcardEntity f WHERE f.user.userId = :userId AND f.due <= :now ORDER BY f.due")
    fun findDueCards(@Param("userId") userId: UUID, @Param("now") now: LocalDateTime): List<FlashcardEntity>
    
    fun findByUser_UserId(userId: UUID): List<FlashcardEntity>
    
    fun findByVocabulary_VocabId(vocabId: UUID): List<FlashcardEntity>
    
    fun findByUser_UserIdAndVocabulary_VocabId(userId: UUID, vocabId: UUID): List<FlashcardEntity>
    
    fun countByCreatedAtAfter(createdAt: LocalDateTime): Long
}

