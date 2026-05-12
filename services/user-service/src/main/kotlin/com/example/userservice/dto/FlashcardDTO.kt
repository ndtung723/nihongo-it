package com.example.userservice.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import java.time.LocalDateTime
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class FlashcardDTO(
    @JsonProperty("id")
    val id: UUID? = null,

    @JsonProperty("frontText")
    val frontText: String? = null,

    @JsonProperty("backText")
    val backText: String? = null,

    @JsonProperty("vocabularyId")
    val vocabularyId: UUID? = null,

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonProperty("due")
    val due: LocalDateTime? = null,

    @JsonProperty("reps")
    val reps: Int? = null,

    @JsonProperty("lapses")
    val lapses: Int? = null,

    @JsonProperty("state")
    val state: String? = null,

    @JsonProperty("difficulty")
    val difficulty: Double? = null,

    @JsonProperty("stability")
    val stability: Double? = null,

    @JsonProperty("interval")
    val interval: Double? = null,

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonProperty("createdAt")
    val createdAt: LocalDateTime? = null,

    @JsonDeserialize(using = LocalDateTimeDeserializer::class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    @JsonSerialize(using = LocalDateTimeSerializer::class)
    @JsonProperty("updatedAt")
    val updatedAt: LocalDateTime? = null,
)
