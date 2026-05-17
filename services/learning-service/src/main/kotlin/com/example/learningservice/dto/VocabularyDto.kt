package com.example.learningservice.dto

import com.example.learningservice.entity.JlptLevel
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant
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
    val createdAt: Instant? = null,
    @JsonProperty("isSaved")
    val isSaved: Boolean = false,
)
