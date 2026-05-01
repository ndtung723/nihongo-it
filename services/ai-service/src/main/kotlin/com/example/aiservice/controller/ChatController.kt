package com.example.aiservice.controller

import com.example.aiservice.service.ChatService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.converter.ListOutputConverter
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestBody
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

@RestController
@RequestMapping("/api/v1/ai/chat")
class ChatController(
    private val chatService: ChatService,
    private val chatClient: ChatClient,
) {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    @GetMapping("/ask-ai")
    fun generate(@RequestParam(value = "message") prompt: String): String {
        return chatService.getResponse(prompt)
    }

    @GetMapping("/ask-ai-options")
    fun getResponseOptions(@RequestParam(value = "message") prompt: String): String {
        return chatService.getResponseOptions(prompt)
    }

    /**
     * Translate text between Vietnamese and Japanese
     * This endpoint requires authentication
     */
    @PostMapping("/translate")
    fun translate(
        @RequestBody text: String,
        @RequestParam direction: String // "vn-to-jp" or "jp-to-vn"
    ): TranslationResponse {
        val sourceLang = if (direction == "vn-to-jp") "Vietnamese" else "Japanese"
        val targetLang = if (direction == "vn-to-jp") "Japanese" else "Vietnamese"
        
        val prompt = """
            Act as a professional translator from $sourceLang to $targetLang.
            Translate the following text accurately and naturally:
            
            $text
            
            Only provide the translation without any explanations or notes.
            If the text contains specialized IT terminology, ensure those terms are translated correctly using appropriate industry terms.
            
            Return the response as JSON in this format:
            {"translation":"<translated text here>"}
        """.trimIndent()
        
        val response = chatService.getResponseOptions(prompt)
        
        // Clean the response by removing markdown code blocks
        val cleanedResponse = response
            .replace("```json", "")
            .replace("```", "")
            .trim()
        
        return try {
            // Parse the JSON response
            objectMapper.readValue(cleanedResponse, TranslationResponse::class.java)
        } catch (e: Exception) {
            // If the response is not valid JSON, create a response with the raw text
            TranslationResponse(cleanedResponse)
        }
    }
    
    /**
     * Economy translation endpoint using GPT-3.5 Turbo for lower cost translations
     * This endpoint requires authentication
     */
    @PostMapping("/translate/economy")
    fun translateEconomy(
        @RequestBody text: String,
        @RequestParam direction: String // "vn-to-jp" or "jp-to-vn"
    ): TranslationResponse {
        val sourceLang = if (direction == "vn-to-jp") "Vietnamese" else "Japanese"
        val targetLang = if (direction == "vn-to-jp") "Japanese" else "Vietnamese"
        
        val prompt = """
            Act as a professional translator from $sourceLang to $targetLang.
            Translate the following text accurately and naturally:
            
            $text
            
            Only provide the translation without any explanations or notes.
            If the text contains specialized IT terminology, ensure those terms are translated correctly using appropriate industry terms.
            
            Return the response as JSON in this format:
            {"translation":"<translated text here>"}
        """.trimIndent()
        
        val response = chatService.getEconomyResponse(prompt)
        
        // Clean the response by removing markdown code blocks
        val cleanedResponse = response
            .replace("```json", "")
            .replace("```", "")
            .trim()
        
        return try {
            // Parse the JSON response
            objectMapper.readValue(cleanedResponse, TranslationResponse::class.java)
        } catch (e: Exception) {
            // If the response is not valid JSON, create a response with the raw text
            TranslationResponse(cleanedResponse)
        }
    }

    @PostMapping("/vocabulary/list")
    fun getListVocabulary(
        @RequestParam category: String,
        @RequestParam(required = false, defaultValue = "N5") level: String,
    ): List<VocabularyInfo>? {
        val vocabularyList = chatClient.prompt()
            .user { u ->
                u.text("Vui lòng cung cấp 2 từ vựng tiếng Nhật thuộc chủ đề {category} ở cấp độ JLPT {level}. Bao gồm từ, cách đọc, ý nghĩa và câu ví dụ.")
                    .param("category", category)
                    .param("level", level)
            }
            .call()
            .entity(object : ParameterizedTypeReference<List<VocabularyInfo>>() {})

        return vocabularyList
    }

    @PostMapping("/vocabulary/explain")
    fun explainVocabulary(
        @RequestParam term: String,
        @RequestParam(required = false) pronunciation: String?,
        @RequestParam meaning: String,
        @RequestParam(required = false) topicName: String?,
        @RequestParam(required = false) example: String?
    ): String {
        val wordDisplay = if (pronunciation.isNullOrBlank()) term else "$term ($pronunciation)"
        val prompt = """
        Hãy đóng vai trò như một giáo viên tiếng Nhật cho học sinh Việt Nam. Tạo một lời giải thích bằng tiếng Việt cho từ vựng này:
        Từ: $wordDisplay
        Ý nghĩa bằng tiếng Việt: $meaning
        Vui lòng cung cấp:
        1. Một lời giải thích ngắn gọn bằng tiếng Việt
        2. Hai câu ví dụ kèm bản dịch tiếng Việt
        Định dạng theo kiểu JSON như ví dụ này:
        {"explanation":"Unit testing là việc kiểm tra các thành phần hoặc module riêng lẻ của phần mềm một cách độc lập để xác minh chúng hoạt động chính xác.","examples":[{"japanese":"単体テストを行うことで、バグを早期に発見できます。","vietnamese":"Bằng cách thực hiện kiểm thử đơn vị, có thể phát hiện lỗi sớm."},{"japanese":"プログラムの各モジュールに対して単体テストを作成しました。","vietnamese":"Tôi đã tạo các bài kiểm tra đơn vị cho mỗi module của chương trình."}]}
    """.trimIndent()
        val response = chatService.getResponseOptions(prompt)
        // Clean the response by removing markdown code blocks
        val cleanedResponse = response
            .replace("```json", "")
            .replace("```", "")
            .trim()
        // Try to validate the response as JSON
        return try {
            // If it's valid JSON, return it as is
            objectMapper.readTree(cleanedResponse)
            cleanedResponse
        } catch (e: Exception) {
            // If not valid JSON, wrap it in a proper JSON structure
            """
        {
            "explanation": "Không thể phân tích cú pháp phản hồi AI. Phản hồi gốc là: ${
                cleanedResponse.replace(
                    "\"",
                    "\\\""
                ).replace("\n", "\\n")
            }",
            "examples": []
        }
        """.trimIndent()
        }
    }

    /**
     * Respond to user messages about vocabulary
     */
    @PostMapping("/vocabulary/chat")
    fun vocabularyChat(
        @RequestParam vocabWord: String,
        @RequestParam userMessage: String
    ): String {
        val prompt = """
        Hãy đóng vai trò như một giáo viên tiếng Nhật cho học sinh Việt Nam liên quan đến từ vựng "$vocabWord". Học sinh đã hỏi về từ "$vocabWord".:
        "$userMessage"
        Vui lòng cung cấp một phản hồi hữu ích, giới hạn tối đa 150 từ bằng tiếng Việt với các ví dụ theo định dạng JSON như sau:
        {"message":"Động từ 思う (omou) có nghĩa là 'nghĩ' trong tiếng Việt."}
    """.trimIndent()
        val response = chatService.getResponseOptions(prompt)
        // Clean the response by removing markdown code blocks
        val cleanedResponse = response
            .replace("```json", "")
            .replace("```", "")
            .trim()
        // Try to validate the response as JSON
        return try {
            // If it's valid JSON, return it as is
            objectMapper.readTree(cleanedResponse)
            cleanedResponse
        } catch (e: Exception) {
            // If not valid JSON, wrap it in a proper JSON structure
            """{"message": "${cleanedResponse.replace("\"", "\\\"").replace("\n", "\\n")}"}"""
        }
    }

    // using ListOutputConverter
    @PostMapping("/list-output")
    fun getListResponse(@RequestParam category: String, year: String): MutableList<String>? {
        return chatClient.prompt()
            .user { u ->
                u.text("Please provide the names of 3 best books for the given {category} and the {year}")
                    .param("category", category)
                    .param("year", year)
            }
            .call()
            .entity(ListOutputConverter(DefaultConversionService()))
    }

    @PostMapping("/advisor")
    fun getResponseAdvisor(@RequestParam message: String): String? {
        return chatClient.prompt()
            .user(message)
            .call()
            .content()
    }

    // using MapOutputConverter
    @PostMapping("/map-output")
    fun getMapResponse(@RequestParam category: String, year: String): Map<String, Any>? {
        return chatClient.prompt()
            .user { u ->
                u.text(
                    """
                Please provide me best book for the given {category} and the {year}.
                Please do provide a summary of the book as well, the information should be 
                limited and not much in depth. The response should be in the JSON format 
                containing this information:
                category, book, year, review, author, summary
                Please remove ```json from the final output
                    """.trimIndent(),
                )
                    .param("category", category)
                    .param("year", year)
            }
            .call()
            .entity(object : ParameterizedTypeReference<Map<String, Any>>() {})
    }
}

/**
 * Data class for vocabulary information
 */
data class VocabularyInfo(
    val word: String,
    val reading: String,
    val meaning: String,
    val example: String
)

/**
 * Data class for translation response
 */
data class TranslationResponse(
    val translation: String
)
