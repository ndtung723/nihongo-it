package com.example.userservice.controller

import com.example.userservice.feign.models.AnalyzeSpeechResponse
import com.example.userservice.feign.models.TranslationResponse
import com.example.userservice.feign.models.VocabularyInfo
import com.example.userservice.service.AiService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/ai")
class AiController(private val aiService: AiService) {

    @PostMapping("/tts/generate")
    fun generateSpeech(
        @RequestBody text: String,
        @RequestHeader(value = "X-Speech-Speed", required = false, defaultValue = "1.0") speedStr: String,
        @RequestHeader(value = "X-Content-Type", required = false, defaultValue = "vocabulary") contentType: String,
        @RequestHeader(value = "X-Content-Language", required = false, defaultValue = "ja") language: String,
        @RequestHeader(value = "X-Save-Audio", required = false, defaultValue = "false") saveAudio: Boolean,
    ): ResponseEntity<ByteArray> {
        return aiService.generateSpeech(text, speedStr, contentType, language, saveAudio)
    }

    @GetMapping("/tts/check")
    fun checkAudioExists(
        @RequestParam text: String,
        @RequestParam(value = "contentType", required = false, defaultValue = "vocabulary") contentType: String,
    ): ResponseEntity<Map<String, Boolean>> {
        return aiService.checkAudioExists(text, contentType)
    }

    @GetMapping("/tts/audio")
    fun getAudio(
        @RequestParam text: String,
        @RequestParam(value = "contentType", required = false, defaultValue = "vocabulary") contentType: String,
    ): ResponseEntity<ByteArray> {
        return aiService.getAudio(text, contentType)
    }

    @PostMapping("/analyze-speech")
    fun analyzeSpeech(
        @RequestPart("audio") audio: MultipartFile,
        @RequestPart("text") text: String,
        @RequestPart("userId", required = false) userId: String?,
    ): ResponseEntity<AnalyzeSpeechResponse> {
        return aiService.analyzeSpeech(audio, text, userId)
    }

    @PostMapping("/speech/analyze-sample")
    fun analyzeSample(
        @RequestParam("sentence") sentence: String,
        @RequestParam("sampleId") sampleId: String,
    ): ResponseEntity<AnalyzeSpeechResponse> {
        return aiService.analyzeSample(sentence, sampleId)
    }

    @GetMapping("/speech/sample-audio/{sampleId}")
    fun getSampleAudio(
        @PathVariable sampleId: String,
        @RequestParam(required = false, defaultValue = "wav") format: String,
    ): ResponseEntity<ByteArray> {
        return aiService.getSampleAudio(sampleId, format)
    }

    @PostMapping("/speech/analyze-audio-enhanced")
    fun analyzeAudioEnhanced(
        @RequestPart("file") audio: MultipartFile,
        @RequestPart("reference_text") referenceText: String,
        @RequestPart("sample_id", required = false) sampleId: String?,
    ): ResponseEntity<Any> {
        return aiService.analyzeAudioEnhanced(audio, referenceText, sampleId)
    }

    @GetMapping("/speech/health")
    fun healthCheck(): ResponseEntity<Map<String, Any>> {
        return aiService.healthCheck()
    }

    @GetMapping("/ask-ai")
    fun askAi(@RequestParam(value = "message") prompt: String): String {
        return aiService.askAI(prompt)
    }

    @GetMapping("/ask-ai-options")
    fun getResponseOptions(@RequestParam(value = "message") prompt: String): String {
        return aiService.getResponseOptions(prompt)
    }

    @PostMapping("/translate")
    fun translate(
        @RequestBody text: String,
        @RequestParam("direction") direction: String,
    ): TranslationResponse {
        return aiService.translate(text, direction)
    }

    @PostMapping("/translate/economy")
    fun translateEconomy(
        @RequestBody text: String,
        @RequestParam("direction") direction: String,
    ): TranslationResponse {
        return aiService.translateEconomy(text, direction)
    }

    @PostMapping("/vocabulary/list")
    fun getVocabularyList(
        @RequestParam("category") category: String,
        @RequestParam(value = "level", required = false, defaultValue = "N5") level: String,
    ): List<VocabularyInfo> {
        return aiService.getVocabularyList(category, level)
    }

    @PostMapping("/vocabulary/explain")
    fun explainVocabulary(
        @RequestParam("term") term: String,
        @RequestParam(value = "pronunciation", required = false) pronunciation: String?,
        @RequestParam("meaning") meaning: String,
        @RequestParam(value = "topicName", required = false) topicName: String?,
        @RequestParam(value = "example", required = false) example: String?,
    ): String {
        return aiService.explainVocabulary(term, pronunciation, meaning, topicName, example)
    }

    @PostMapping("/vocabulary/chat")
    fun vocabularyChat(
        @RequestParam("vocabWord") vocabWord: String,
        @RequestParam("userMessage") userMessage: String,
    ): String {
        return aiService.vocabularyChat(vocabWord, userMessage)
    }

    @PostMapping("/list-output")
    fun getListResponse(
        @RequestParam("category") category: String,
        @RequestParam("year") year: String,
    ): MutableList<String>? {
        return aiService.getListResponse(category, year)
    }

    @PostMapping("/advisor")
    fun getResponseAdvisor(@RequestParam("message") message: String): String? {
        return aiService.getResponseAdvisor(message)
    }

    @PostMapping("/map-output")
    fun getMapResponse(
        @RequestParam("category") category: String,
        @RequestParam("year") year: String,
    ): Map<String, Any>? {
        return aiService.getMapResponse(category, year)
    }
}
