package com.example.userservice.dto

import com.example.userservice.entity.JlptLevel
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

// DTO for filtering vocabulary
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VocabularyFilterRequestDto(
    @JsonProperty("keyword")
    val keyword: String? = null,
    @JsonProperty("jlptLevel")
    val jlptLevel: JlptLevel? = null,
    @JsonProperty("topicName")
    val topicName: String? = null,
    @JsonProperty("topicId")
    val topicId: UUID? = null,
    @JsonProperty("categoryId")
    val categoryId: UUID? = null,
    @JsonProperty("page")
    val page: Int = 0,
    @JsonProperty("size")
    val size: Int = 10,
    @JsonProperty("sort")
    val sort: String? = null,
)
