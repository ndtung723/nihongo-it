package com.example.learningservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class GetStatisticsResponseDto(
    @JsonProperty("data")
    val data: StudyStatisticsDto,
)
