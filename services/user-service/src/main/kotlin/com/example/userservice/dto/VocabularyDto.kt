package com.example.userservice.dto

import com.example.userservice.entity.JlptLevel
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.*

// Response DTO for vocabulary retrieval
data class VocabularyDto(
    @JsonProperty("vocabId")
    val vocabId: UUID,
    @JsonProperty("term")
    val term: String,
    @JsonProperty("meaning")
    val meaning: String,
    @JsonProperty("pronunciation")
    val pronunciation: String?,
    @JsonProperty("example")
    val example: String?,
    @JsonProperty("exampleMeaning")
    val exampleMeaning: String?,
    @JsonProperty("audioPath")
    val audioPath: String? = null,
    @JsonProperty("jlptLevel")
    val jlptLevel: JlptLevel,
    @JsonProperty("topicId")
    val topicId: UUID? = null,
    @JsonProperty("topicName")
    val topicName: String? = null,
    @JsonProperty("createdAt")
    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    @JsonProperty("isSaved")
    val isSaved: Boolean = false,
)
