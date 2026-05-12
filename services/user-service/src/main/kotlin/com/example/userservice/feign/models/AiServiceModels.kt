package com.example.userservice.feign.models

// Request models
data class TextToSpeechRequest(
    val text: String,
)

data class ChatRequest(
    val message: String,
)

data class TranslateRequest(
    val text: String,
    val direction: String,
)

// Response models
data class AnalyzeSpeechResponse(
    val accuracy: Double,
    val feedback: String,
    val issues: List<String>,
)

data class ChatResponse(
    val response: String,
)

data class TranslationResponse(
    val translation: String,
)

/**
 * Data class for vocabulary information
 */
data class VocabularyInfo(
    val word: String,
    val reading: String,
    val meaning: String,
    val example: String,
)
