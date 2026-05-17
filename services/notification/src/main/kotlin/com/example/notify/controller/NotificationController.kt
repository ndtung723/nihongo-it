package com.example.notify.controller

import com.example.notify.entity.NotificationEntity
import com.example.notify.service.NotificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notify/notifications")
@Tag(name = "Notifications", description = "User notification management")
class NotificationController(
    private val notificationService: NotificationService,
) {
    private fun currentUserId(): UUID = UUID.fromString(SecurityContextHolder.getContext().authentication?.principal as String)

    @GetMapping
    @Operation(summary = "Get paginated notifications for current user")
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Map<String, Any>> {
        val result = notificationService.listForUser(currentUserId(), page, size)
        return ResponseEntity.ok(
            mapOf(
                "content" to result.content.map { it.toDto() },
                "totalElements" to result.totalElements,
                "totalPages" to result.totalPages,
                "page" to result.number,
                "size" to result.size,
            ),
        )
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    fun getUnreadCount(): ResponseEntity<Map<String, Long>> =
        ResponseEntity.ok(mapOf("count" to notificationService.countUnreadForUser(currentUserId())))

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    fun markAsRead(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> =
        if (notificationService.markAsReadForUser(currentUserId(), id)) {
            ResponseEntity.ok(mapOf("message" to "Notification marked as read"))
        } else {
            ResponseEntity.notFound().build()
        }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    fun markAllAsRead(): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(mapOf("updated" to notificationService.markAllAsReadForUser(currentUserId())))

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    fun deleteNotification(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> =
        if (notificationService.deleteForUser(currentUserId(), id)) {
            ResponseEntity.ok(mapOf("message" to "Notification deleted"))
        } else {
            ResponseEntity.notFound().build()
        }
}

private fun NotificationEntity.toDto() =
    mapOf(
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
