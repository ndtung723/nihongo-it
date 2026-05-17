package com.example.common.logging

import ch.qos.logback.classic.pattern.MessageConverter
import ch.qos.logback.classic.spi.ILoggingEvent

class SensitiveMaskingConverter : MessageConverter() {
    override fun convert(event: ILoggingEvent): String {
        var message = event.formattedMessage
        for ((pattern, replacement) in MASKS) {
            message = pattern.replace(message, replacement)
        }
        return message
    }

    companion object {
        private val MASKS =
            listOf(
                // JWT tokens: eyJxxx.yyy.zzz
                Regex("""eyJ[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+\.[A-Za-z0-9_\-]+""") to "[JWT-REDACTED]",
                // password values in JSON: "password":"any"
                Regex(""""(?:password|currentPassword|newPassword|confirmPassword)"\s*:\s*"[^"]*"""") to "\"password\":\"[REDACTED]\"",
                // DB connection string passwords: password=xxx in jdbc url or query strings
                Regex("""(?i)(?:password|pwd)=[^&\s;,'"]+""") to "password=[REDACTED]",
            )
    }
}
