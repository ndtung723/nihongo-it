package com.example.userservice.dto

import com.example.userservice.entity.CategoryEntity
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CategoryDTO(
    val categoryId: UUID?,
    val name: String,
    val meaning: String? = null,
    val displayOrder: Int = 0,
    val topicCount: Int? = null,
    val isActive: Boolean = true,
    @JsonProperty("createdAt")
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @JsonProperty("updatedAt")
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime? = null,
)

// Extension function to convert entity to DTO
fun CategoryEntity.toDTO(topicCount: Int? = null): CategoryDTO =
    CategoryDTO(
        categoryId = this.categoryId,
        name = this.name,
        meaning = this.meaning,
        displayOrder = this.displayOrder,
        topicCount = topicCount ?: this.topics.size,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

// Data class for creating a new category
data class CreateCategoryRequest(
    val name: String,
    val meaning: String,
    val description: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
)

// Data class for updating an existing category
data class UpdateCategoryRequest(
    val name: String? = null,
    val meaning: String? = null,
    val description: String? = null,
    val displayOrder: Int? = null,
    val isActive: Boolean? = null,
)
