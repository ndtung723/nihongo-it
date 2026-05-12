package com.example.learningservice.dto

import com.example.learningservice.entity.JlptLevel
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class UserUpdateRequest(
    @field:Email(message = "Email format is invalid")
    @field:Size(max = 50, message = "Email must be less than 50 characters")
    @JsonProperty("email")
    val email: String? = null,

    @field:Size(max = 100, message = "Full name must be less than 100 characters")
    @JsonProperty("fullName")
    val fullName: String? = null,

    @JsonProperty("profilePicture")
    val profilePicture: String? = null,

    @JsonProperty("currentLevel")
    val currentLevel: JlptLevel? = null,

    @JsonProperty("jlptGoal")
    val jlptGoal: JlptLevel? = null,

    @JsonProperty("isActive")
    val isActive: Boolean? = null,

    @JsonProperty("isEmailVerified")
    val isEmailVerified: Boolean? = null,

    @JsonProperty("password")
    val password: String? = null,

    @JsonProperty("roleId")
    val roleId: Int? = null,

    @JsonProperty("reminderEnabled")
    val reminderEnabled: Boolean? = null,

    @JsonProperty("reminderTime")
    val reminderTime: String? = null,

    @JsonProperty("notificationPreferences")
    val notificationPreferences: String? = null,

    @JsonProperty("minCardThreshold")
    val minCardThreshold: Int? = null,
)
