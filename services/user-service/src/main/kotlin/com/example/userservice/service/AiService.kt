package com.example.userservice.service

import com.example.userservice.feign.AiServiceClient
import com.example.userservice.feign.models.*
import feign.FeignException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class AiService(
    private val aiServiceClient: AiServiceClient,
) {
    private val logger = LoggerFactory.getLogger(AiService::class.java)

    fun generateSpeech(
        text: String,
        speedStr: String = "1.0",
        contentType: String = "vocabulary",
        language: String = "ja",
        saveAudio: Boolean = false,
    ): ResponseEntity<ByteArray> {
        try {
            return aiServiceClient.generateSpeech(text, speedStr, contentType, language, saveAudio)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for text-to-speech: ${e.message}")
            throw RuntimeException("Failed to convert text to speech", e)
        }
    }

    fun checkAudioExists(
        text: String,
        contentType: String = "vocabulary",
    ): ResponseEntity<Map<String, Boolean>> {
        try {
            return aiServiceClient.checkAudioExists(text, contentType)
        } catch (e: FeignException) {
            logger.error("Error calling AI service to check audio existence: ${e.message}")
            throw RuntimeException("Failed to check if audio exists", e)
        }
    }

    fun getAudio(
        text: String,
        contentType: String = "vocabulary",
    ): ResponseEntity<ByteArray> {
        try {
            return aiServiceClient.getAudio(text, contentType)
        } catch (e: FeignException) {
            logger.error("Error calling AI service to get audio: ${e.message}")
            throw RuntimeException("Failed to get audio", e)
        }
    }

    fun analyzeSpeech(
        audio: MultipartFile,
        text: String,
        userId: String? = null,
    ): ResponseEntity<AnalyzeSpeechResponse> {
        try {
            return aiServiceClient.analyzeSpeech(audio, text, userId)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for speech analysis: ${e.message}")
            throw RuntimeException("Failed to analyze speech", e)
        }
    }

    fun analyzeSample(
        sentence: String,
        sampleId: String,
    ): ResponseEntity<AnalyzeSpeechResponse> {
        try {
            return aiServiceClient.analyzeSample(sentence, sampleId)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for sample analysis: ${e.message}")
            throw RuntimeException("Failed to analyze speech sample", e)
        }
    }

    fun getSampleAudio(
        sampleId: String,
        format: String = "wav",
    ): ResponseEntity<ByteArray> {
        try {
            return aiServiceClient.getSampleAudio(sampleId, format)
        } catch (e: FeignException) {
            logger.error("Error calling AI service to get sample audio: ${e.message}")
            throw RuntimeException("Failed to get sample audio", e)
        }
    }

    fun analyzeAudioEnhanced(
        audio: MultipartFile,
        referenceText: String,
        sampleId: String? = null,
    ): ResponseEntity<Any> {
        try {
            return aiServiceClient.analyzeAudioEnhanced(audio, referenceText, sampleId)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for enhanced audio analysis: ${e.message}")
            throw RuntimeException("Failed to analyze audio with enhanced method", e)
        }
    }

    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        try {
            return aiServiceClient.healthCheck()
        } catch (e: FeignException) {
            logger.error("Error calling AI service health check: ${e.message}")
            throw RuntimeException("Failed to check AI service health", e)
        }
    }

    fun askAI(prompt: String): String {
        try {
            return aiServiceClient.askAI(prompt)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for askAI: ${e.message}")
            throw RuntimeException("Failed to get AI response", e)
        }
    }

    fun getResponseOptions(prompt: String): String {
        try {
            return aiServiceClient.getResponseOptions(prompt)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for response options: ${e.message}")
            throw RuntimeException("Failed to get response options", e)
        }
    }

    fun translate(
        text: String,
        direction: String,
    ): TranslationResponse {
        try {
            return aiServiceClient.translate(text, direction)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for translation: ${e.message}")
            throw RuntimeException("Failed to translate text", e)
        }
    }

    fun translateEconomy(
        text: String,
        direction: String,
    ): TranslationResponse {
        try {
            return aiServiceClient.translateEconomy(text, direction)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for economy translation: ${e.message}")
            throw RuntimeException("Failed to translate text", e)
        }
    }

    fun getVocabularyList(
        category: String,
        level: String = "N5",
    ): List<VocabularyInfo> {
        try {
            return aiServiceClient.getVocabularyList(category, level)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for vocabulary list: ${e.message}")
            throw RuntimeException("Failed to get vocabulary list", e)
        }
    }

    fun explainVocabulary(
        term: String,
        pronunciation: String?,
        meaning: String,
        topicName: String?,
        example: String?,
    ): String {
        try {
            return aiServiceClient.explainVocabulary(term, pronunciation, meaning, topicName, example)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for vocabulary explanation: ${e.message}")
            throw RuntimeException("Failed to explain vocabulary", e)
        }
    }

    fun vocabularyChat(
        vocabWord: String,
        userMessage: String,
    ): String {
        try {
            return aiServiceClient.vocabularyChat(vocabWord, userMessage)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for vocabulary chat: ${e.message}")
            throw RuntimeException("Failed to get vocabulary chat response", e)
        }
    }

    fun getListResponse(
        category: String,
        year: String,
    ): MutableList<String>? {
        try {
            return aiServiceClient.getListResponse(category, year)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for list response: ${e.message}")
            throw RuntimeException("Failed to get list response", e)
        }
    }

    fun getResponseAdvisor(message: String): String? {
        try {
            return aiServiceClient.getResponseAdvisor(message)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for advisor response: ${e.message}")
            throw RuntimeException("Failed to get advisor response", e)
        }
    }

    fun getMapResponse(
        category: String,
        year: String,
    ): Map<String, Any>? {
        try {
            return aiServiceClient.getMapResponse(category, year)
        } catch (e: FeignException) {
            logger.error("Error calling AI service for map response: ${e.message}")
            throw RuntimeException("Failed to get map response", e)
        }
    }
}
