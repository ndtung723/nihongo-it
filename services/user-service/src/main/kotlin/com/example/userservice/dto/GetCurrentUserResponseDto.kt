package com.example.userservice.dto

import com.example.common.dto.ResponseDto
import com.example.common.dto.ResponseType

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GetCurrentUserResponseDto(
    val status: ResponseType,
    val userInfo: UserDto? = null,
    val message: String? = null
) {
    constructor(responseDto: ResponseDto, userInfo: UserDto? = null) : this(
        status = responseDto.status, 
        userInfo = userInfo,
        message = responseDto.message
    )
}
