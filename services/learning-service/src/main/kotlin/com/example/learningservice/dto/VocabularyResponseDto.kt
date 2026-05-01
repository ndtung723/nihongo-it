package com.example.learningservice.dto

import com.example.learningservice.entity.JlptLevel
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VocabularyResponseDto(
    @JsonProperty("vocabId")
    val vocabId: UUID,

    @JsonProperty("hiragana")
    val hiragana: String,

    @JsonProperty("meaning")
    val meaning: String,

    @JsonProperty("kanji")
    val kanji: String?,

    @JsonProperty("katakana")
    val katakana: String?,

    @JsonProperty("exampleSentence")
    val exampleSentence: String?,

    @JsonProperty("exampleSentenceTranslation")
    val exampleSentenceTranslation: String?,

    @JsonProperty("audioPath")
    val audioPath: String?,

    @JsonProperty("category")
    val category: String?,

    @JsonProperty("jlptLevel")
    val jlptLevel: JlptLevel,

    @JsonProperty("createdById")
    val createdById: UUID?,

    @JsonProperty("createdByEmail")
    val createdByEmail: String?,

    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    val createdAt: LocalDateTime,

    @JsonProperty("isSaved")
    val isSaved: Boolean = false,
)
