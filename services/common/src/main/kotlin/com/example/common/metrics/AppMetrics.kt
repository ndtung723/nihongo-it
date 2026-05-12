package com.example.common.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class AppMetrics(registry: MeterRegistry) {

    val flashcardReviews: Counter = registry.counter("nihongoit.flashcard.reviews")
    val ttsGenerated: Counter = registry.counter("nihongoit.tts.generated")
    val openAiCalls: Counter = registry.counter("nihongoit.openai.calls")
    val loginSuccess: Counter = registry.counter("nihongoit.auth.login.success")
    val loginFailure: Counter = registry.counter("nihongoit.auth.login.failure")
    val rateLimitHits: Counter = registry.counter("nihongoit.rate_limit.hits")
}
