package com.example.learningservice.dto

import com.example.common.dto.ResponseDto

import com.fasterxml.jackson.annotation.JsonProperty

data class DeleteFlashcardResponseDto(
    @JsonProperty("result")
    val result: ResponseDto
) 