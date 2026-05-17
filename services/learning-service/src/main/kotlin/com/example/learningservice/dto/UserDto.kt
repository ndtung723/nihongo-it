package com.example.learningservice.dto

import com.example.learningservice.entity.JlptLevel
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.UUID

data class UserDto(
    @JsonProperty("userId")
    val userId: UUID,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("fullName")
    val fullName: String,

    @JsonProperty("roleId")
    val roleId: Int,

    @JsonProperty("profilePicture")
    val profilePicture: String?,

    @JsonProperty("currentLevel")
    val currentLevel: JlptLevel?,

    @JsonProperty("jlptGoal")
    val jlptGoal: JlptLevel?,

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    val lastLogin: LocalDateTime?,

    @JsonProperty("isActive")
    val isActive: Boolean = true,

    @JsonProperty("isEmailVerified")
    val isEmailVerified: Boolean = false,

    @JsonProperty("streakCount")
    val streakCount: Int = 0,

    @JsonProperty("points")
    val points: Int = 0,

    @JsonProperty("reminderEnabled")
    val reminderEnabled: Boolean = true,

    @JsonProperty("minCardThreshold")
    val minCardThreshold: Int? = null,
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AdminUserDetailDto(
    @JsonProperty("userId")
    val userId: UUID,

    @JsonProperty("email")
    val email: String,

    @JsonProperty("fullName")
    val fullName: String,

    @JsonProperty("roleId")
    val roleId: Int,

    @JsonProperty("profilePicture")
    val profilePicture: String?,

    @JsonProperty("currentLevel")
    val currentLevel: JlptLevel?,

    @JsonProperty("jlptGoal")
    val jlptGoal: JlptLevel?,

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    val lastLogin: LocalDateTime?,

    @JsonProperty("isActive")
    val isActive: Boolean = true,

    @JsonProperty("isEmailVerified")
    val isEmailVerified: Boolean = false,

    @JsonProperty("streakCount")
    val streakCount: Int = 0,

    @JsonProperty("points")
    val points: Int = 0,

    @JsonProperty("reminderEnabled")
    val reminderEnabled: Boolean = true,

    @JsonProperty("reminderTime")
    val reminderTime: String? = null,

    @JsonProperty("minCardThreshold")
    val minCardThreshold: Int? = null,

    // Flashcard stats — populated by FlashcardStatisticsService
    @JsonProperty("flashcardCount")
    val flashcardCount: Int? = null,

    @JsonProperty("newCards")
    val newCards: Int? = null,

    @JsonProperty("learningCards")
    val learningCards: Int? = null,

    @JsonProperty("masteredCards")
    val masteredCards: Int? = null,
)
