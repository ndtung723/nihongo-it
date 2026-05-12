package com.example.learningservice.dto

import com.example.learningservice.entity.ConversationEntity
import com.example.learningservice.entity.ConversationLineEntity
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.UUID

/**
 * DTO for conversation line
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConversationLineDTO(
    @JsonProperty("lineId")
    val lineId: UUID? = null,

    @JsonProperty("speaker")
    val speaker: String,

    @JsonProperty("japaneseText")
    val japaneseText: String,

    @JsonProperty("vietnameseTranslation")
    val vietnameseTranslation: String? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("importantVocab")
    val importantVocab: String? = null,

    @JsonProperty("orderIndex")
    val orderIndex: Int = 0,
) {
    companion object {
        fun fromEntity(entity: ConversationLineEntity): ConversationLineDTO {
            return ConversationLineDTO(
                lineId = entity.lineId,
                speaker = entity.speaker,
                japaneseText = entity.japaneseText,
                vietnameseTranslation = entity.vietnameseTranslation,
                notes = entity.notes,
                importantVocab = entity.importantVocab,
                orderIndex = entity.orderIndex,
            )
        }
    }
}

/**
 * DTO for conversation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ConversationDTO(
    @JsonProperty("conversationId")
    val conversationId: UUID? = null,

    @JsonProperty("title")
    val title: String,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("jlptLevel")
    val jlptLevel: String? = null,

    @JsonProperty("unit")
    val unit: Int? = null,

    @JsonProperty("lines")
    val lines: List<ConversationLineDTO> = emptyList(),

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
) {
    companion object {
        fun fromEntity(entity: ConversationEntity): ConversationDTO {
            return ConversationDTO(
                conversationId = entity.convId,
                title = entity.title,
                description = entity.description,
                jlptLevel = entity.jlptLevel,
                unit = entity.unit,
                lines = entity.lines.map { ConversationLineDTO.fromEntity(it) }.sortedBy { it.orderIndex },
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
        }
    }
}

/**
 * Request to create a new conversation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateConversationRequest(
    @JsonProperty("title")
    val title: String,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("jlptLevel")
    val jlptLevel: String? = null,

    @JsonProperty("unit")
    val unit: Int? = null,

    @JsonProperty("lines")
    val lines: List<CreateConversationLineRequest> = emptyList(),
)

/**
 * Request to create a new conversation line
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateConversationLineRequest(
    @JsonProperty("speaker")
    val speaker: String,

    @JsonProperty("japaneseText")
    val japaneseText: String,

    @JsonProperty("vietnameseTranslation")
    val vietnameseTranslation: String? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("importantVocab")
    val importantVocab: String? = null,

    @JsonProperty("orderIndex")
    val orderIndex: Int = 0,
)

/**
 * Request to update an existing conversation
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateConversationRequest(
    @JsonProperty("title")
    val title: String? = null,

    @JsonProperty("description")
    val description: String? = null,

    @JsonProperty("jlptLevel")
    val jlptLevel: String? = null,

    @JsonProperty("unit")
    val unit: Int? = null,

    @JsonProperty("lines")
    val lines: List<UpdateConversationLineRequest>? = null,
)

/**
 * Request to update an existing conversation line
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateConversationLineRequest(
    @JsonProperty("lineId")
    val lineId: UUID? = null,

    @JsonProperty("speaker")
    val speaker: String,

    @JsonProperty("japaneseText")
    val japaneseText: String,

    @JsonProperty("vietnameseTranslation")
    val vietnameseTranslation: String? = null,

    @JsonProperty("notes")
    val notes: String? = null,

    @JsonProperty("importantVocab")
    val importantVocab: String? = null,

    @JsonProperty("orderIndex")
    val orderIndex: Int,
)

/**
 * Paged response for conversations
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PagedResponse<T>(
    @JsonProperty("content")
    val content: List<T>,

    @JsonProperty("totalPages")
    val totalPages: Int,

    @JsonProperty("totalElements")
    val totalElements: Long,

    @JsonProperty("currentPage")
    val currentPage: Int,

    @JsonProperty("size")
    val size: Int,
)
