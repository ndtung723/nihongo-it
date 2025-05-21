package com.example.notificationservice.repository

import com.example.notificationservice.entity.NotificationEntity
import com.example.notificationservice.entity.NotificationType
import com.example.notificationservice.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<NotificationEntity, UUID> {
    // Find the most recent notification by user and type
    fun findFirstByUserAndTypeOrderBySentAtDesc(user: UserEntity, type: NotificationType): NotificationEntity?
} 