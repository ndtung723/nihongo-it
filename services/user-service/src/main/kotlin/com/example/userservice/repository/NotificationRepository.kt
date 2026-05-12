package com.example.userservice.repository

import com.example.userservice.entity.NotificationEntity
import com.example.userservice.entity.NotificationType
import com.example.userservice.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<NotificationEntity, UUID> {
    fun findByUserOrderBySentAtDesc(user: UserEntity): List<NotificationEntity>
    fun findByUserAndIsReadFalseOrderBySentAtDesc(user: UserEntity): List<NotificationEntity>
    fun findByUserAndSentAtAfterOrderBySentAtDesc(user: UserEntity, afterDate: LocalDateTime): List<NotificationEntity>
    fun countByUserAndIsReadFalse(user: UserEntity): Long

    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.sentAt < :beforeDate")
    fun deleteByCreatedAtBefore(beforeDate: LocalDateTime): Int

    // Find the most recent notification by user and type
    fun findFirstByUserAndTypeOrderBySentAtDesc(user: UserEntity, type: NotificationType): NotificationEntity?
}
