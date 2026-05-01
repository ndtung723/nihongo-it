package com.example.aiservice.service

import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.nio.file.Files
import java.nio.file.Paths
import org.springframework.beans.factory.annotation.Value

@Service
class SpeechAnalysisService(
    @Value("\${ai-service.speech-analysis.python-service-url}") private val pythonServiceUrl: String,
    @Value("\${INTERNAL_API_KEY:}") private val internalApiKey: String,
) {

    private val logger = LoggerFactory.getLogger(SpeechAnalysisService::class.java)
    private val webClient: WebClient by lazy {
        WebClient.builder()
            .baseUrl(pythonServiceUrl)
            .defaultHeader("X-Internal-Key", internalApiKey)
            .build()
    }

    /**
     * Enhanced audio analysis that forwards to Python service
     */
    fun analyzeEnhanced(audio: MultipartFile, referenceText: String, type: String): Map<String, Any> {
        // Validate input
        if (referenceText.isBlank()) {
            logger.error("Empty reference text received")
            throw IllegalArgumentException("Câu tham chiếu không hợp lệ")
        }

        // Prepare audio file
        val audioResource = object : ByteArrayResource(audio.bytes) {
            override fun getFilename(): String {
                return audio.originalFilename ?: "recording.mp3"
            }
        }

        // Prepare multipart request body
        val bodyBuilder = MultipartBodyBuilder()
        bodyBuilder.part("file", audioResource, MediaType.parseMediaType(audio.contentType ?: "audio/mp3"))
        bodyBuilder.part("reference_text", referenceText)
        bodyBuilder.part("type", type)
        
        try {
            logger.info("Sending request to Python service at: /analyze-audio-enhanced")
            
            val response = webClient.post()
                .uri("/analyze-audio-enhanced")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(Map::class.java)
                .blockOptional()
                .orElseThrow { RuntimeException("Không có phản hồi từ dịch vụ Python") } as Map<String, Any>

            logger.info("Received enhanced analysis response from Python service")
            
            // Return the response directly without any modifications
            return response
        } catch (e: Exception) {
            logger.error("Error in enhanced analysis: ${e.message}")
            throw RuntimeException("Lỗi khi thực hiện phân tích nâng cao: ${e.message}")
        }
    }
    
    /**
     * Summarize feedback from multiple attempts
     */
    fun summarizeFeedback(feedbackList: List<*>, conversationText: String): Map<String, Any> {
        try {
            logger.info("Sending feedback summary request to Python service")
            
            // Create request body
            val requestBody = mapOf(
                "feedback_list" to feedbackList,
                "conversation_text" to conversationText
            )
            
            // Send request to Python service
            val response = webClient.post()
                .uri("/summarize-feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(Map::class.java)
                .blockOptional()
                .orElseThrow { RuntimeException("Không có phản hồi từ dịch vụ Python") } as Map<String, Any>
                
            logger.info("Received feedback summary response from Python service")
            
            return response
        } catch (e: Exception) {
            logger.error("Error in feedback summary: ${e.message}")
            throw RuntimeException("Lỗi khi tổng hợp phản hồi: ${e.message}")
        }
    }
} 