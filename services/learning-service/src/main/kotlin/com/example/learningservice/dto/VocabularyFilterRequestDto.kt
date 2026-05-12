package com.example.learningservice.dto

import com.example.learningservice.entity.JlptLevel
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.util.UUID

// DTO for filtering vocabulary
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VocabularyFilterRequestDto(
    @get:Size(max = 200, message = "Keyword must not exceed 200 characters")
    @JsonProperty("keyword")
    val keyword: String? = null,

    @JsonProperty("jlptLevel")
    val jlptLevel: JlptLevel? = null,

    @get:Size(max = 100, message = "Topic name must not exceed 100 characters")
    @JsonProperty("topicName")
    val topicName: String? = null,

    @JsonProperty("topicId")
    val topicId: UUID? = null,

    @JsonProperty("categoryId")
    val categoryId: UUID? = null,

    @get:Min(value = 0, message = "Page must be 0 or greater")
    @JsonProperty("page")
    val page: Int = 0,

    @get:Min(value = 1, message = "Size must be at least 1")
    @get:Max(value = 100, message = "Size must not exceed 100")
    @JsonProperty("size")
    val size: Int = 10,

    @JsonProperty("sort")
    val sort: String? = null,
)
