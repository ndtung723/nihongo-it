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

@Service
class SpeechAnalysisService {

    private val logger = LoggerFactory.getLogger(SpeechAnalysisService::class.java)
    private val SAMPLES_DIR = "src/main/resources/samples/"
    private val webClient: WebClient

    init {
        this.webClient = WebClient.builder()
                .baseUrl("http://localhost:8000")
                .build()
    }


    /**
     * Process and enhance the analysis results if needed
     */
    private fun processAnalysisResults(response: Map<String, Any>): Map<String, Any> {
        try {
            // Ensure all required fields are present
            val processedResponse = response.toMutableMap()
            
            // Ensure words array has expected properties
            @Suppress("UNCHECKED_CAST")
            val words = processedResponse["words"] as? List<Map<String, Any>> ?: emptyList()
            
            // Log summary of word analysis
            val correctWords = words.count { it["isCorrect"] as? Boolean ?: false }
            val incorrectWords = words.size - correctWords
            logger.info("Word analysis summary: ${words.size} total words, $correctWords correct, $incorrectWords incorrect")
            
            // Log personalized feedback if present
            val personalizedFeedback = processedResponse["personalizedFeedback"] as? String
            if (personalizedFeedback != null) {
                logger.info("Personalized feedback provided: ${personalizedFeedback.take(50)}...")
            } else {
                // Add default personalized feedback if not present
                val score = processedResponse["score"] as? Number ?: 0
                val defaultFeedback = if (score.toInt() > 80) {
                    "Phát âm tốt! Tiếp tục luyện tập để hoàn thiện hơn."
                } else {
                    "Cần cải thiện phát âm của một số từ. Hãy tập trung vào các từ được đánh dấu đỏ."
                }
                processedResponse["personalizedFeedback"] = defaultFeedback
                logger.info("Added default personalized feedback")
            }
            
            return processedResponse
        } catch (e: Exception) {
            logger.error("Error processing analysis results", e)
            return response // Return original response if processing fails
        }
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
            val response = webClient.post()
                .uri("/analyze-audio-enhanced")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .retrieve()
                .bodyToMono(Map::class.java)
                .blockOptional()
                .orElseThrow { RuntimeException("Không có phản hồi từ dịch vụ Python") } as Map<String, Any>

            logger.info("Received enhanced analysis response from Python service")
            
            // Process the results if needed
            return processAnalysisResults(response)
        } catch (e: Exception) {
            logger.error("Error in enhanced analysis: ${e.message}")
            throw RuntimeException("Lỗi khi thực hiện phân tích nâng cao: ${e.message}")
        }

    }

} 