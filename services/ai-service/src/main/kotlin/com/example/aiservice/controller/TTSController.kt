package com.example.aiservice.controller

import org.springframework.ai.openai.OpenAiAudioSpeechModel
import org.springframework.ai.openai.OpenAiAudioSpeechOptions
import org.springframework.ai.openai.api.OpenAiAudioApi
import org.springframework.ai.openai.audio.speech.SpeechPrompt
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/ai-service-api/v1/tts")
class TTSController(
    private val openAiAudioSpeechModel: OpenAiAudioSpeechModel,
) {
    private val logger = LoggerFactory.getLogger(TTSController::class.java)
    
    // Định nghĩa các loại nội dung hợp lệ
    private val validContentTypes = setOf("vocabulary", "example", "conversation")
    private val defaultContentType = "vocabulary"

    @PostMapping("/generate")
    fun generateSpeech(
        @RequestBody text: String,
        @RequestHeader(value = "X-Speech-Speed", required = false, defaultValue = "1.0") speedStr: String,
        @RequestHeader(value = "X-Content-Type", required = false, defaultValue = "vocabulary") contentType: String,
        @RequestHeader(value = "X-Content-Language", required = false, defaultValue = "ja") language: String,
        @RequestHeader(value = "X-Save-Audio", required = false, defaultValue = "false") saveAudio: Boolean
    ): ResponseEntity<ByteArray> {
        // Validate content type
        val validatedContentType = if (contentType in validContentTypes) contentType else defaultContentType
        
        // Parse speed parameter
        val speed = try {
            speedStr.toFloat().coerceIn(0.25f, 4.0f) // Ensure speed is within OpenAI's allowed range
        } catch (e: Exception) {
            1.0f // Default to 1.0 if parsing fails
        }

        val options = OpenAiAudioSpeechOptions.builder()
            .voice(OpenAiAudioApi.SpeechRequest.Voice.NOVA) // NOVA has the best Japanese pronunciation
            .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
            .model("gpt-4o-mini-tts")
            .speed(speed)
            .build()

        val response = try {
            openAiAudioSpeechModel.call(SpeechPrompt(text, options))
        } catch (e: Exception) {
            throw RuntimeException("TTS generation failed: ${e.message}")
        }

        // Validate audio output
        require(response.result.output.isNotEmpty()) {
            "OpenAI returned empty audio data"
        }

        // Save the audio file if requested
        if (saveAudio) {
            saveGeneratedAudio(text, response.result.output, validatedContentType)
        }

        return ResponseEntity.ok()
            .contentType(MediaType("audio", "mpeg"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=speech.mp3")
            .header("X-Content-Language", language)
            .header("X-Content-Type", validatedContentType)
            .body(response.result.output)
    }
    
    /**
     * Saves the generated audio to the appropriate directory based on content type
     */
    private fun saveGeneratedAudio(text: String, audioData: ByteArray, contentType: String) {
        try {
            // Determine the appropriate directory based on content type
            val directoryPath = Paths.get("src", "main", "resources", contentType)
            
            // Create directory if it doesn't exist
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath)
                logger.info("Created directory: $directoryPath")
            }
            
            // Use exact text as filename to preserve Japanese characters
            val audioFile = directoryPath.resolve("$text.mp3")
            
            // Save the audio data to the file
            Files.write(audioFile, audioData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            logger.info("Saved generated audio to: ${audioFile.toAbsolutePath()}")
        } catch (e: Exception) {
            logger.error("Failed to save generated audio: ${e.message}", e)
            // We don't want to fail the main request if saving fails
        }
    }
    
    /**
     * Check if audio file exists for the given text
     */
    @GetMapping("/check")
    fun checkAudioExists(
        @RequestParam text: String,
        @RequestParam(required = false, defaultValue = "vocabulary") contentType: String
    ): ResponseEntity<Map<String, Boolean>> {
        // Validate content type
        val validatedContentType = if (contentType in validContentTypes) contentType else defaultContentType
        
        val directoryPath = Paths.get("src", "main", "resources", validatedContentType)
        
        // Use exact text as filename
        val audioFile = directoryPath.resolve("$text.mp3")
        
        val exists = Files.exists(audioFile)
        
        return ResponseEntity.ok(mapOf("exists" to exists))
    }
    
    /**
     * Retrieve previously generated audio file
     */
    @GetMapping("/audio")
    fun getAudio(
        @RequestParam text: String,
        @RequestParam(required = false, defaultValue = "vocabulary") contentType: String
    ): ResponseEntity<ByteArray> {
        // Validate content type
        val validatedContentType = if (contentType in validContentTypes) contentType else defaultContentType
        
        val directoryPath = Paths.get("src", "main", "resources", validatedContentType)
        
        // Use exact text as filename
        val audioFile = directoryPath.resolve("$text.mp3")
        
        if (!Files.exists(audioFile)) {
            return ResponseEntity.notFound().build()
        }
        
        val audioData = Files.readAllBytes(audioFile)
        
        return ResponseEntity.ok()
            .contentType(MediaType("audio", "mpeg"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"audio.mp3\"")
            .body(audioData)
    }
}
