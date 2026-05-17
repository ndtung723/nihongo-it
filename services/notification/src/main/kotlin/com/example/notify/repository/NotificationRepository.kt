package com.example.notify.repository

import com.example.notify.entity.NotificationEntity
import com.example.notify.entity.NotificationType
import com.example.notify.entity.UserEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface NotificationRepository : JpaRepository<NotificationEntity, UUID> {
    fun findFirstByUserAndTypeOrderBySentAtDesc(
        user: UserEntity,
        type: NotificationType,
    ): NotificationEntity?

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    fun findByUser_UserIdOrderBySentAtDesc(
        userId: UUID,
        pageable: Pageable,
    ): Page<NotificationEntity>

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    fun countByUser_UserIdAndIsReadFalse(userId: UUID): Long

    @Suppress("FunctionNaming", "ktlint:standard:function-naming")
    fun findByNotificationIdAndUser_UserId(
        notificationId: UUID,
        userId: UUID,
    ): NotificationEntity?

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    fun markAllReadByUserId(
        @Param("userId") userId: UUID,
    ): Int

    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.notificationId = :id AND n.user.userId = :userId")
    fun deleteByIdAndUserId(
        @Param("id") id: UUID,
        @Param("userId") userId: UUID,
    ): Int
}
