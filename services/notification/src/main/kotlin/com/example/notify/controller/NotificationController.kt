package com.example.notify.controller

import com.example.notify.entity.NotificationEntity
import com.example.notify.repository.NotificationRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notify/notifications")
@Tag(name = "Notifications", description = "User notification management")
class NotificationController(
    private val notificationRepository: NotificationRepository,
) {

    private fun currentUserId(): UUID =
        UUID.fromString(SecurityContextHolder.getContext().authentication.principal as String)

    @GetMapping
    @Operation(summary = "Get paginated notifications for current user")
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Map<String, Any>> {
        val userId = currentUserId()
        val pageable = PageRequest.of(maxOf(0, page), size.coerceIn(1, 50), Sort.by(Sort.Direction.DESC, "sentAt"))
        val result = notificationRepository.findByUser_UserIdOrderBySentAtDesc(userId, pageable)
        return ResponseEntity.ok(mapOf(
            "content" to result.content.map { it.toDto() },
            "totalElements" to result.totalElements,
            "totalPages" to result.totalPages,
            "page" to result.number,
            "size" to result.size,
        ))
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    fun getUnreadCount(): ResponseEntity<Map<String, Long>> {
        val count = notificationRepository.countByUser_UserIdAndIsReadFalse(currentUserId())
        return ResponseEntity.ok(mapOf("count" to count))
    }

    @PutMapping("/{id}/read")
    @Transactional
    @Operation(summary = "Mark a notification as read")
    fun markAsRead(@PathVariable id: UUID): ResponseEntity<Any> {
        val userId = currentUserId()
        val notification = notificationRepository.findByNotificationIdAndUser_UserId(id, userId)
            ?: return ResponseEntity.notFound().build()
        notificationRepository.save(notification.copy(isRead = true, readAt = LocalDateTime.now()))
        return ResponseEntity.ok(mapOf("message" to "Notification marked as read"))
    }

    @PutMapping("/read-all")
    @Transactional
    @Operation(summary = "Mark all notifications as read")
    fun markAllAsRead(): ResponseEntity<Map<String, Any>> {
        val updated = notificationRepository.markAllReadByUserId(currentUserId())
        return ResponseEntity.ok(mapOf("updated" to updated))
    }

    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Delete a notification")
    fun deleteNotification(@PathVariable id: UUID): ResponseEntity<Any> {
        val deleted = notificationRepository.deleteByIdAndUserId(id, currentUserId())
        return if (deleted > 0) ResponseEntity.ok(mapOf("message" to "Notification deleted"))
        else ResponseEntity.notFound().build()
    }
}

private fun NotificationEntity.toDto() = mapOf(
    "id" to notificationId,
    "title" to title,
    "message" to message,
    "type" to type.name,
    "isRead" to isRead,
    "actionUrl" to actionUrl,
    "sentAt" to sentAt.toString(),
    "readAt" to readAt?.toString(),
    "reviewCount" to reviewCount,
    "priorityLevel" to priorityLevel,
)
