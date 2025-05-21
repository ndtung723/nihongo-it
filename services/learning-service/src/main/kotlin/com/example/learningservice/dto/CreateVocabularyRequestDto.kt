package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateVocabularyRequestDto(
    @get:NotBlank(message = "Term is required")
    @get:Size(max = 100, message = "Term cannot exceed 100 characters")
    @JsonProperty("term")
    val term: String,

    @get:NotBlank(message = "Meaning is required")
    @get:Size(max = 255, message = "Meaning cannot exceed 255 characters")
    @JsonProperty("meaning")
    val meaning: String,

    @get:Size(max = 100, message = "Pronunciation cannot exceed 100 characters")
    @JsonProperty("pronunciation")
    val pronunciation: String? = null,

    @get:Size(max = 500, message = "Example cannot exceed 500 characters")
    @JsonProperty("example")
    val example: String? = null, //example sentence

    @get:Size(max = 500, message = "Example meaning cannot exceed 500 characters")
    @JsonProperty("exampleMeaning")
    val exampleMeaning: String? = null, //vietnamese meaning

    @JsonProperty("audioPath")
    val audioPath: String? = null,

    @get:NotBlank(message = "Topic Name is required")
    @JsonProperty("topicName")
    val topicName: String,

    @get:NotNull(message = "JLPT level is required")
    @get:Pattern(regexp = "^(N5|N4|N3|N2|N1)$", message = "JLPT level must be one of: N5, N4, N3, N2, N1")
    @JsonProperty("jlptLevel")
    val jlptLevel: String,
)
