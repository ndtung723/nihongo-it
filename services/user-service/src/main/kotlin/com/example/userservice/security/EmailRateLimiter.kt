package com.example.userservice.security

import com.example.common.exception.BusinessException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class EmailRateLimiter {
    companion object {
        private const val MAX_ATTEMPTS = 3
        private val WINDOW: Duration = Duration.ofMinutes(5)
    }

    // email → timestamps of recent attempts within the window
    private val attempts = ConcurrentHashMap<String, ArrayDeque<Instant>>()

    /**
     * Records an attempt for [email] and throws [BusinessException] if the limit is exceeded.
     */
    fun checkAndRecord(email: String) {
        val now = Instant.now()
        val windowStart = now.minus(WINDOW)
        val deque = attempts.computeIfAbsent(email.lowercase()) { ArrayDeque() }

        synchronized(deque) {
            // Drop expired entries from the front
            while (deque.isNotEmpty() && deque.first().isBefore(windowStart)) {
                deque.removeFirst()
            }
            if (deque.size >= MAX_ATTEMPTS) {
                throw BusinessException(
                    "Too many password reset requests for this email. Please wait a few minutes before trying again.",
                )
            }
            deque.addLast(now)
        }
    }

    @Scheduled(fixedDelay = 3_600_000)
    fun evictStale() {
        val windowStart = Instant.now().minus(WINDOW)
        attempts.entries.removeIf { (_, deque) ->
            synchronized(deque) { deque.all { it.isBefore(windowStart) } }
        }
    }
}
