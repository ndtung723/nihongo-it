package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ReviewFlashcardResponseDto(
    @JsonProperty("result")
    val result: ResponseDto,
    
    @JsonProperty("data")
    val data: FlashcardDTO
) 