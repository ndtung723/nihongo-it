package com.example.userservice.feign

import com.example.userservice.config.FeignConfig
import com.example.userservice.config.FeignEncoderConfig
import com.example.userservice.config.FeignErrorDecoderConfig
import com.example.userservice.feign.models.AnalyzeSpeechResponse
import com.example.userservice.feign.models.TranslationResponse
import com.example.userservice.feign.models.VocabularyInfo
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@FeignClient(
    name = "ai-service",
    configuration = [FeignConfig::class, FeignEncoderConfig::class, FeignErrorDecoderConfig::class],

)
interface AiServiceClient {

    // Text-to-speech endpoints
    @PostMapping("/api/v1/ai/tts/generate")
    fun generateSpeech(
        @RequestBody text: String,
        @RequestHeader(value = "X-Speech-Speed", required = false, defaultValue = "1.0") speedStr: String,
        @RequestHeader(value = "X-Content-Type", required = false, defaultValue = "vocabulary") contentType: String,
        @RequestHeader(value = "X-Content-Language", required = false, defaultValue = "ja") language: String,
        @RequestHeader(value = "X-Save-Audio", required = false, defaultValue = "false") saveAudio: Boolean,
    ): ResponseEntity<ByteArray>

    @GetMapping("/api/v1/ai/tts/check")
    fun checkAudioExists(
        @RequestParam("text") text: String,
        @RequestParam(value = "contentType", required = false, defaultValue = "vocabulary") contentType: String,
    ): ResponseEntity<Map<String, Boolean>>

    @GetMapping("/api/v1/ai/tts/audio")
    fun getAudio(
        @RequestParam("text") text: String,
        @RequestParam(value = "contentType", required = false, defaultValue = "vocabulary") contentType: String,
    ): ResponseEntity<ByteArray>

    // Speech analysis endpoints
    @PostMapping(value = ["/api/v1/ai/speech/analyze"], consumes = ["multipart/form-data"])
    fun analyzeSpeech(
        @RequestPart("audio") audio: MultipartFile,
        @RequestPart("sentence") text: String,
        @RequestPart("userId", required = false) userId: String?,
    ): ResponseEntity<AnalyzeSpeechResponse>

    @PostMapping("/api/v1/ai/speech/analyze-sample")
    fun analyzeSample(
        @RequestParam("sentence") sentence: String,
        @RequestParam("sampleId") sampleId: String,
    ): ResponseEntity<AnalyzeSpeechResponse>

    @GetMapping("/api/v1/ai/speech/sample-audio/{sampleId}")
    fun getSampleAudio(
        @PathVariable sampleId: String,
        @RequestParam(required = false, defaultValue = "wav") format: String,
    ): ResponseEntity<ByteArray>

    @PostMapping(value = ["/api/v1/ai/speech/analyze-audio-enhanced"], consumes = ["multipart/form-data"])
    fun analyzeAudioEnhanced(
        @RequestPart("file") audio: MultipartFile,
        @RequestPart("reference_text") referenceText: String,
        @RequestPart("sample_id", required = false) sampleId: String?,
    ): ResponseEntity<Any>

    @GetMapping("/api/v1/ai/speech/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>>

    // Chat endpoints
    @GetMapping("/api/v1/ai/chat/ask-ai")
    fun askAI(@RequestParam("message") prompt: String): String

    @GetMapping("/api/v1/ai/chat/ask-ai-options")
    fun getResponseOptions(@RequestParam("message") prompt: String): String

    // Translation endpoints
    @PostMapping("/api/v1/ai/chat/translate")
    fun translate(
        @RequestBody text: String,
        @RequestParam("direction") direction: String,
    ): TranslationResponse

    @PostMapping("/api/v1/ai/chat/translate/economy")
    fun translateEconomy(
        @RequestBody text: String,
        @RequestParam("direction") direction: String,
    ): TranslationResponse

    // Vocabulary endpoints
    @PostMapping("/api/v1/ai/chat/vocabulary/list")
    fun getVocabularyList(
        @RequestParam("category") category: String,
        @RequestParam("level") level: String = "N5",
    ): List<VocabularyInfo>

    @PostMapping("/api/v1/ai/chat/vocabulary/explain")
    fun explainVocabulary(
        @RequestParam("term") term: String,
        @RequestParam("pronunciation") pronunciation: String?,
        @RequestParam("meaning") meaning: String,
        @RequestParam("topicName") topicName: String?,
        @RequestParam("example") example: String?,
    ): String

    @PostMapping("/api/v1/ai/chat/vocabulary/chat")
    fun vocabularyChat(
        @RequestParam("vocabWord") vocabWord: String,
        @RequestParam("userMessage") userMessage: String,
    ): String

    @PostMapping("/api/v1/ai/chat/list-output")
    fun getListResponse(
        @RequestParam("category") category: String,
        @RequestParam("year") year: String,
    ): MutableList<String>?

    @PostMapping("/api/v1/ai/chat/advisor")
    fun getResponseAdvisor(@RequestParam("message") message: String): String?

    @PostMapping("/api/v1/ai/chat/map-output")
    fun getMapResponse(
        @RequestParam("category") category: String,
        @RequestParam("year") year: String,
    ): Map<String, Any>?
}
