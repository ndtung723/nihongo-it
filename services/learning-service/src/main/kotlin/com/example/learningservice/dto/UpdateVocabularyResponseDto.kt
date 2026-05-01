package com.example.learningservice.dto

import com.example.common.dto.ResponseDto

import com.fasterxml.jackson.annotation.JsonProperty

class UpdateVocabularyResponseDto (
    @JsonProperty("result")
    val result: ResponseDto,
    @JsonProperty("data")
    val data: VocabularyDto
)