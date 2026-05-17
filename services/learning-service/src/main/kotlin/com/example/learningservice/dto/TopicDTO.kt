package com.example.learningservice.dto

import com.example.learningservice.entity.JlptLevel
import com.example.learningservice.entity.TopicEntity
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
data class TopicDTO(
    val topicId: UUID?,
    val name: String,
    val meaning: String,
    val displayOrder: Int = 0,
    val jlptLevel: JlptLevel? = null,
    val categoryId: UUID?,
    val categoryName: String?,
    val vocabularyCount: Int? = null,
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
fun TopicEntity.toDTO(vocabularyCount: Int? = null): TopicDTO =
    TopicDTO(
        topicId = this.topicId,
        name = this.name,
        meaning = this.meaning,
        displayOrder = this.displayOrder,
        categoryId = this.category.categoryId,
        categoryName = this.category.name,
        vocabularyCount = vocabularyCount ?: this.vocabularyItems.size,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

// Data class for creating a new topic
data class CreateTopicRequest(
    val name: String,
    val meaning: String,
    val description: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val categoryId: UUID,
)

// Data class for updating an existing topic
data class UpdateTopicRequest(
    val name: String? = null,
    val meaning: String? = null,
    val description: String? = null,
    val displayOrder: Int? = null,
    val isActive: Boolean? = null,
    val categoryId: UUID? = null,
)
