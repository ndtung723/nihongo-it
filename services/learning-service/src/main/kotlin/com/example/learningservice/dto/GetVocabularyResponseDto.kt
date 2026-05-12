package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetVocabularyResponseDto(
    @JsonProperty("data")
    val data: VocabularyDto,
)
