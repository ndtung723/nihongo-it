package com.example.userservice.dto

import com.example.common.dto.ResponseDto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetStatisticsResponseDto(
    @JsonProperty("result")
    val result: ResponseDto,
    
    @JsonProperty("data")
    val data: Map<String, Any>
) 