package com.example.common.logging

import com.example.common.metrics.AppMetrics
import net.logstash.logback.argument.StructuredArguments.kv
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SecurityEventLogger(
    private val metrics: AppMetrics,
) {
    private val log = LoggerFactory.getLogger("security.events")

    fun loginSuccess(
        email: String,
        ip: String,
    ) {
        metrics.loginSuccess.increment()
        log.info("auth.login.success", kv("email", email), kv("ip", ip))
    }

    fun loginFailure(
        email: String,
        ip: String,
        reason: String,
    ) {
        metrics.loginFailure.increment()
        log.warn("auth.login.failure", kv("email", email), kv("ip", ip), kv("reason", reason))
    }

    fun tokenInvalid(
        path: String,
        ip: String,
        reason: String,
    ) = log.warn("auth.token.invalid", kv("path", path), kv("ip", ip), kv("reason", reason))

    fun roleDenied(
        userId: String,
        path: String,
        requiredRole: String,
    ) = log.warn("auth.role.denied", kv("userId", userId), kv("path", path), kv("requiredRole", requiredRole))

    fun rateLimitExceeded(
        key: String,
        path: String,
    ) = log.warn("auth.rate_limit.exceeded", kv("key", key), kv("path", path))
}
