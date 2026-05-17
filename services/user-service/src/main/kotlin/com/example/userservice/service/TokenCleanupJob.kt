package com.example.userservice.service

import com.example.userservice.repository.RefreshTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class TokenCleanupJob(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    private val logger = LoggerFactory.getLogger(TokenCleanupJob::class.java)

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    fun cleanupExpiredTokens() {
        refreshTokenRepository.deleteAllExpired(LocalDateTime.now())
        logger.debug("Expired refresh tokens cleaned up")
    }
}
