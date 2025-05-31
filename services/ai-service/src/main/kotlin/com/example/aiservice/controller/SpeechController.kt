package com.example.aiservice.controller

import com.example.aiservice.service.SpeechAnalysisService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/ai-service-api/v1/speech")
class SpeechController {

    private val logger = LoggerFactory.getLogger(SpeechController::class.java)

    @Autowired
    private lateinit var speechAnalysisService: SpeechAnalysisService

    @PostMapping("/analyze-audio-enhanced")
    fun analyzeAudioEnhanced(
            @RequestParam("file") audio: MultipartFile,
            @RequestParam("reference_text") referenceText: String,
            @RequestParam("type", required = true) type: String
    ): ResponseEntity<Any> {
        
        return try {
            val analysis = speechAnalysisService.analyzeEnhanced(audio, referenceText, type)
            logger.info("Enhanced analysis completed successfully")
            ResponseEntity.ok(analysis)
        } catch (e: Exception) {
            logger.error("Error processing enhanced analysis request", e)
            ResponseEntity.badRequest().body(mapOf(
                "error" to e.message,
                "message" to "Lỗi khi phân tích nâng cao: ${e.message}"
            ))
        }
    }
} 