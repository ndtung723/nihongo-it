package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetVocabularyResponseDto(
    @JsonProperty("data")
    val data: VocabularyDto,
)
