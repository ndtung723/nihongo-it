package com.example.userservice.dto

import com.example.common.dto.ResponseDto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetDueCardsResponseDto(
    @JsonProperty("result")
    val result: ResponseDto,
    
    @JsonProperty("data")
    val data: List<FlashcardDTO>
) 