package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class PagedFlashcardsResponseDto(
    @JsonProperty("data")
    val data: List<FlashcardDTO>,

    @JsonProperty("page")
    val page: Int,

    @JsonProperty("size")
    val size: Int,

    @JsonProperty("totalElements")
    val totalElements: Long,

    @JsonProperty("totalPages")
    val totalPages: Int,
)
