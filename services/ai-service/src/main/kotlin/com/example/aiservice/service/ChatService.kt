package com.example.aiservice.service

import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ChatService(
    private val chatModel: ChatModel,
) {
    companion object {
        private const val DEFAULT_TEMPERATURE = 0.4
        private const val VOCAB_CHAT_MAX_WORDS = 150
    }

    @Value("\${ai-service.openai.default-model:gpt-4o}")
    private lateinit var defaultModel: String

    @Value("\${ai-service.openai.economy-model:gpt-3.5-turbo}")
    private lateinit var economyModel: String

    fun getResponse(prompt: String): String {
        return chatModel.call(prompt)
    }

    fun getResponseOptions(prompt: String): String {
        val response: ChatResponse = chatModel.call(
            Prompt(
                prompt,
                OpenAiChatOptions.builder()
                    .model(defaultModel)
                    .temperature(DEFAULT_TEMPERATURE)
                    .build(),
            ),
        )
        return response.result.output.text
    }

    fun getEconomyResponse(prompt: String): String {
        val response: ChatResponse = chatModel.call(
            Prompt(
                prompt,
                OpenAiChatOptions.builder()
                    .model(economyModel)
                    .temperature(DEFAULT_TEMPERATURE)
                    .build(),
            ),
        )
        return response.result.output.text
    }

    fun streamVocabularyChatResponse(vocabWord: String, userMessage: String): Flux<String> =
        chatModel.stream(
            Prompt(
                buildVocabChatPrompt(vocabWord, userMessage),
                OpenAiChatOptions.builder()
                    .model(defaultModel)
                    .temperature(DEFAULT_TEMPERATURE)
                    .build(),
            ),
        ).mapNotNull { it.result?.output?.text }

    private fun buildVocabChatPrompt(vocabWord: String, userMessage: String) =
        """
        Hãy đóng vai trò như một giáo viên tiếng Nhật cho học sinh Việt Nam liên quan đến từ vựng "$vocabWord". Học sinh đã hỏi về từ "$vocabWord".:
        "$userMessage"
        Vui lòng cung cấp một phản hồi hữu ích, giới hạn tối đa $VOCAB_CHAT_MAX_WORDS từ bằng tiếng Việt với các ví dụ.
        """.trimIndent()
}
