package com.example.learningservice.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "categories")
data class CategoryEntity(
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "category_id", updatable = false, nullable = false)
    val categoryId: UUID? = null,

    @Column(name = "name", nullable = false, unique = true)
    val name: String, // japanese name

    @Column(name = "meaning")
    val meaning: String? = null, // vietnamese meaning

    @Column(name = "display_order")
    val displayOrder: Int = 0,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: LocalDateTime? = LocalDateTime.now(),
    
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = LocalDateTime.now(),
    
    @OneToMany(mappedBy = "category", cascade = [CascadeType.ALL], orphanRemoval = true)
    val topics: MutableList<TopicEntity> = mutableListOf()
) 