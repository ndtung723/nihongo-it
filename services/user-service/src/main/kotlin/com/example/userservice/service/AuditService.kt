package com.example.userservice.service

import com.example.userservice.entity.AuditAction
import com.example.userservice.entity.AuditLogEntity
import com.example.userservice.repository.AuditLogRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
) {
    private val logger = LoggerFactory.getLogger(AuditService::class.java)

    fun log(
        action: AuditAction,
        userId: UUID? = null,
        actorId: UUID? = null,
        targetType: String? = null,
        targetId: String? = null,
        ip: String? = null,
        details: String? = null,
    ) {
        try {
            auditLogRepository.save(
                AuditLogEntity(
                    action = action.name,
                    userId = userId,
                    actorId = actorId,
                    targetType = targetType,
                    targetId = targetId,
                    ip = ip,
                    details = details,
                ),
            )
        } catch (e: Exception) {
            logger.error("Failed to persist audit log action={} userId={}: {}", action, userId, e.message)
        }
    }
}
