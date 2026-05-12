package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UpdateVocabularyResponseDto(
    @JsonProperty("data")
    val data: VocabularyDto,
)
