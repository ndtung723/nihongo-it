package com.example.learningservice.service

import com.example.learningservice.dto.FlashcardDTO
import com.example.learningservice.entity.FlashcardEntity

fun FlashcardEntity.toDto(): FlashcardDTO = FlashcardDTO(
    id = flashcardId,
    frontText = frontText,
    backText = backText,
    vocabularyId = vocabulary?.vocabId,
    due = due,
    reps = reps,
    lapses = lapses,
    state = FSRSService.State.entries.find { it.value == state }?.name?.lowercase() ?: "new",
    difficulty = difficulty,
    stability = stability,
    interval = scheduledDays,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
