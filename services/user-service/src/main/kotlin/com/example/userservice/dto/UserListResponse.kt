package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class UserListResponse(
    @JsonProperty("users")
    val users: List<UserDto>,

    @JsonProperty("totalItems")
    val totalItems: Long,

    @JsonProperty("totalPages")
    val totalPages: Int,

    @JsonProperty("currentPage")
    val currentPage: Int,
)
