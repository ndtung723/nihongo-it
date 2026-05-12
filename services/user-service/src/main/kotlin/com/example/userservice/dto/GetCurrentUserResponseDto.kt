package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GetCurrentUserResponseDto(
    val userInfo: UserDto? = null,
)
