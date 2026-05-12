package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetDueCardsResponseDto(
    @JsonProperty("data")
    val data: List<FlashcardDTO>,
)
