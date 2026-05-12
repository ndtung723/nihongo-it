package com.example.userservice.repository

import com.example.userservice.entity.AuditLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AuditLogRepository : JpaRepository<AuditLogEntity, UUID>
