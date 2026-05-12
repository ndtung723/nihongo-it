package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetStatisticsResponseDto(
    @JsonProperty("data")
    val data: Map<String, Any>,
)
