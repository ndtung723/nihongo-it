package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class ReviewRequest(
    @field:Min(1, message = "Rating must be at least 1")
    @field:Max(4, message = "Rating cannot be more than 4")
    @JsonProperty("rating")
    val rating: Int,
)
