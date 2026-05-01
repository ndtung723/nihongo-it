package com.example.userservice.dto

import com.example.common.dto.ResponseDto

import com.fasterxml.jackson.annotation.JsonProperty

data class ReviewFlashcardResponseDto(
    @JsonProperty("result")
    val result: ResponseDto,
    
    @JsonProperty("data")
    val data: FlashcardDTO
) 