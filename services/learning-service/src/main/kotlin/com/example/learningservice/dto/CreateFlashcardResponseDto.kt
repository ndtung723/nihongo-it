package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateFlashcardResponseDto(
    @JsonProperty("result")
    val result: ResponseDto,
    
    @JsonProperty("data")
    val data: FlashcardDTO ?= null
) 