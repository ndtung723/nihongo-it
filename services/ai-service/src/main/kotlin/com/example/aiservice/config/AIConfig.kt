package com.example.aiservice.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.memory.InMemoryChatMemory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AIConfig {

    @Bean
    fun chatMemory(): ChatMemory {
        return InMemoryChatMemory()
    }

    @Bean
    @Suppress("MaxLineLength")
    fun chatClient(builder: ChatClient.Builder): ChatClient {
        return builder
            .defaultSystem("Bạn là trợ lý học tiếng Nhật, không trả lời nội dung không liên quan đến việc học tiếng Nhật hay các thông tin không liên quan.")
            .defaultAdvisors(
                MessageChatMemoryAdvisor(chatMemory()),
                SafeGuardAdvisor(
                    listOf(
                        "illegal", "smuggling", "drugs", "weapons", "violence",
                        "hate speech", "adult content", "gambling", "malware", "spam",
                        "explicit", "pornography", "sex", "erotic", "fetish",
                        "tình dục", "quan hệ", "nhạy cảm", "kích thích", "dâm", "thủ dâm",
                        "nóng bỏng", "hưng phấn", "khoái cảm",
                    ),
                ),
                SimpleLoggerAdvisor(),
            )
            .build()
    }
}
