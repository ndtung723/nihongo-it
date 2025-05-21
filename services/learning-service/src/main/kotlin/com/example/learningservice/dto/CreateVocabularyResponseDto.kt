package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CreateVocabularyResponseDto(
    @JsonProperty("result")
    val result: ResponseDto,
)
