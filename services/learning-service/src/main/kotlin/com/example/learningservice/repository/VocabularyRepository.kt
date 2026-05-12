package com.example.learningservice.repository

import com.example.learningservice.entity.JlptLevel
import com.example.learningservice.entity.VocabularyEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface VocabularyRepository : JpaRepository<VocabularyEntity, UUID> {

    // Basic operations
    @Query("SELECT v FROM VocabularyEntity v WHERE v.term = :term")
    fun findByTerm(@Param("term") term: String): Optional<VocabularyEntity>

    @Query("SELECT COUNT(v) > 0 FROM VocabularyEntity v WHERE v.term = :term")
    fun existsByTerm(@Param("term") term: String): Boolean

    // Find by JLPT level
    @Query("SELECT v FROM VocabularyEntity v WHERE v.jlptLevel = :level")
    fun findByJlptLevel(@Param("level") level: JlptLevel, pageable: Pageable): Page<VocabularyEntity>

    // Find by topic ID
    @Query("SELECT v FROM VocabularyEntity v WHERE v.topic.topicId = :topicId")
    fun findByTopicId(@Param("topicId") topicId: UUID, pageable: Pageable): Page<VocabularyEntity>

    // Find by topic name
    @Query("SELECT v FROM VocabularyEntity v WHERE v.topic.name = :topicName")
    fun findByTopicName(@Param("topicName") topicName: String, pageable: Pageable): Page<VocabularyEntity>

    // Search by keyword in term, pronunciation, or meaning
    @Query(
        """
        SELECT v FROM VocabularyEntity v
        WHERE LOWER(v.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(v.pronunciation) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(v.meaning) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """,
    )
    fun searchVocabulary(@Param("keyword") keyword: String, pageable: Pageable): Page<VocabularyEntity>

    // Find saved vocabulary for a specific user
    @Query("SELECT v FROM VocabularyEntity v JOIN v.savedByUsers u WHERE u.userId = :userId")
    fun findSavedByUser(@Param("userId") userId: UUID, pageable: Pageable): Page<VocabularyEntity>

    // Find saved vocabulary with keyword search
    @Query(
        """
        SELECT v FROM VocabularyEntity v
        JOIN v.savedByUsers u
        WHERE u.userId = :userId
        AND (
            LOWER(v.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.pronunciation) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(v.meaning) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    """,
    )
    fun findSavedByUserAndKeyword(
        @Param("userId") userId: UUID,
        @Param("keyword") keyword: String,
        pageable: Pageable,
    ): Page<VocabularyEntity>

    // Find by topic ID
    @Query("SELECT v FROM VocabularyEntity v WHERE v.topic.topicId = :topicId")
    fun findByTopic_TopicId(@Param("topicId") topicId: UUID, pageable: Pageable): Page<VocabularyEntity>

    // Find by topic ID and keyword in term or meaning
    @Query(
        """
        SELECT v FROM VocabularyEntity v
        WHERE v.topic.topicId = :topicId
        AND (LOWER(v.term) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(v.meaning) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """,
    )
    fun findByTopicIdAndKeyword(
        @Param("topicId") topicId: UUID,
        @Param("keyword") keyword: String,
        pageable: Pageable,
    ): Page<VocabularyEntity>

    // Find by topic ID and JLPT level
    @Suppress("FunctionNaming")
    @Query("SELECT v FROM VocabularyEntity v WHERE v.topic.topicId = :topicId AND v.jlptLevel = :level")
    fun findByTopic_TopicIdAndJlptLevel(
        @Param("topicId") topicId: UUID,
        @Param("level") level: JlptLevel,
        pageable: Pageable,
    ): Page<VocabularyEntity>
}
