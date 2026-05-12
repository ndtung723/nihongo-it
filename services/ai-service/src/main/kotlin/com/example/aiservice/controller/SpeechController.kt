package com.example.aiservice.controller

import com.example.aiservice.service.SpeechAnalysisService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/ai/speech")
class SpeechController(
    private val speechAnalysisService: SpeechAnalysisService,
) {
    companion object {
        private const val LOG_PREVIEW_CHARS = 30
    }

    private val logger = LoggerFactory.getLogger(SpeechController::class.java)

    @PostMapping("/analyze-audio-enhanced")
    fun analyzeAudioEnhanced(
        @RequestParam("file") audio: MultipartFile,
        @RequestParam("reference_text") referenceText: String,
        @RequestParam("type", required = true) type: String,
    ): ResponseEntity<Any> {
        return try {
            val analysis = speechAnalysisService.analyzeEnhanced(audio, referenceText, type)
            logger.info("Enhanced analysis completed successfully")
            ResponseEntity.ok(analysis)
        } catch (e: Exception) {
            logger.error("Error processing enhanced analysis request", e)
            ResponseEntity.badRequest().body(
                mapOf(
                    "error" to e.message,
                    "message" to "Lỗi khi phân tích nâng cao: ${e.message}",
                ),
            )
        }
    }

    @PostMapping("/summarize-feedback")
    fun summarizeFeedback(@RequestBody request: Map<String, Any>): ResponseEntity<Any> {
        return try {
            val feedbackList = request["feedback_list"] as? List<*> ?: emptyList<Any>()
            val conversationText = (request["conversation_text"] as? String).orEmpty()

            logger.info("Summarizing feedback for conversation: ${conversationText.take(LOG_PREVIEW_CHARS)}...")

            val summary = speechAnalysisService.summarizeFeedback(feedbackList, conversationText)
            ResponseEntity.ok(summary)
        } catch (e: Exception) {
            logger.error("Error summarizing feedback", e)
            ResponseEntity.badRequest().body(
                mapOf(
                    "error" to e.message,
                    "message" to "Lỗi khi tổng hợp phản hồi: ${e.message}",
                ),
            )
        }
    }
}
