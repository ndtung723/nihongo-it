package com.example.learningservice.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "topics")
data class TopicEntity(
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "topic_id", updatable = false, nullable = false)
    val topicId: UUID? = null,

    @Column(name = "name", nullable = false, unique = true)
    val name: String, // japanese name

    @Column(name = "meaning", nullable = false)
    val meaning: String, // vietnamese meaning

    @Column(name = "display_order")
    val displayOrder: Int = 0,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: CategoryEntity,

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = LocalDateTime.now(),

    @OneToMany(mappedBy = "topic", cascade = [CascadeType.ALL], orphanRemoval = true)
    val vocabularyItems: MutableList<VocabularyEntity> = mutableListOf(),
)
