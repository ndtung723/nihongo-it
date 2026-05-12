package com.example.userservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "audit_logs")
data class AuditLogEntity(
    @Id
    @Column(updatable = false, nullable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id")
    val userId: UUID? = null,

    @Column(name = "actor_id")
    val actorId: UUID? = null,

    @Column(name = "action", nullable = false, length = 100)
    val action: String,

    @Column(name = "target_type", length = 50)
    val targetType: String? = null,

    @Column(name = "target_id", length = 255)
    val targetId: String? = null,

    @Column(name = "ip", length = 45)
    val ip: String? = null,

    @Column(name = "details", length = 1000)
    val details: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
