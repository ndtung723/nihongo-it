package com.example.aiservice.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AIConfig {
    @Bean
    fun chatMemory(): ChatMemory = MessageWindowChatMemory.builder().build()

    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient =
        builder
            .defaultSystem(
                "Bạn là trợ lý học tiếng Nhật, không trả lời nội dung " +
                    "không liên quan đến việc học tiếng Nhật hay các thông tin không liên quan.",
            ).defaultAdvisors(
                MessageChatMemoryAdvisor.builder(chatMemory()).build(),
                SafeGuardAdvisor(
                    listOf(
                        "illegal",
                        "smuggling",
                        "drugs",
                        "weapons",
                        "violence",
                        "hate speech",
                        "adult content",
                        "gambling",
                        "malware",
                        "spam",
                        "explicit",
                        "pornography",
                        "sex",
                        "erotic",
                        "fetish",
                    ),
                ),
                SimpleLoggerAdvisor(),
            ).build()
}
