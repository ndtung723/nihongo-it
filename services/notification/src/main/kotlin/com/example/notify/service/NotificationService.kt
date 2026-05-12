package com.example.notify.service

import com.example.notify.entity.NotificationChannel
import com.example.notify.entity.NotificationEntity
import com.example.notify.entity.NotificationType
import com.example.notify.entity.UserEntity
import com.example.notify.repository.NotificationRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

/**
 * Service for managing notifications for the Japanese IT vocabulary learning application
 * Currently implements email notifications with in-app notifications stored in database
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val javaMailSender: JavaMailSender,
) {
    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    @Value("\${spring.mail.username:no.reply.nihongo.it@gmail.com}")
    private lateinit var senderEmail: String

    @Value("\${app.frontend.url:http://localhost:5173}")
    private lateinit var frontendUrl: String

    // Remove the property initialization here
    private lateinit var disableNotificationActionUrl: String

    // Initialize in a PostConstruct method
    @PostConstruct
    fun init() {
        disableNotificationActionUrl = "$frontendUrl/account/notifications"
    }

    /**
     * Send a notification to a user through their preferred channels
     */
    @Transactional
    @Suppress("LongParameterList")
    fun sendNotification(
        user: UserEntity,
        title: String,
        message: String,
        type: NotificationType,
        actionUrl: String? = null,
        reviewCount: Int? = null,
        reviewCategory: String? = null,
        priorityLevel: Int = 0,
    ): NotificationEntity {
        logger.debug("Sending notification to user ${user.userId} - Type: $type, Title: $title")

        // Create notification record
        val notification = NotificationEntity(
            user = user,
            title = title,
            message = message,
            type = type,
            actionUrl = actionUrl,
            sentAt = LocalDateTime.now(),
            reviewCount = reviewCount,
            reviewCategory = reviewCategory,
            priorityLevel = priorityLevel,
            notificationChannel = NotificationChannel.APP,
        )

        // Save to database first (in-app notification)
        val savedNotification = notificationRepository.save(notification)

        // Get user preferences
        val channels = user.notificationPreferences

        // Send email notification if enabled
        if (channels.contains("email")) {
            sendEmailNotification(user.email, title, message, actionUrl)
        }

        return savedNotification
    }

    /**
     * Sends an HTML email notification to the specified email address
     *
     * @param to The recipient email address
     * @param subject The email subject
     * @param content The email content
     * @param actionUrl Optional URL for action buttons
     * @param actionText Optional text for the action button (defaults to "Xem ngay")
     */
    @Async
    fun sendEmailNotification(to: String, subject: String, content: String, actionUrl: String? = null, actionText: String = "Xem ngay") {
        try {
            logger.debug("Sending email to $to - Subject: $subject")

            val message = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(senderEmail)
            helper.setTo(to)
            helper.setSubject(subject)

            // Build HTML email content with styling
            val htmlContent = buildHtmlEmailContent(content, actionUrl, actionText)
            helper.setText(htmlContent, true)

            javaMailSender.send(message)
            logger.debug("HTML email sent successfully to $to")
        } catch (e: Exception) {
            // In development, just log the error but don't fail
            logger.error("Failed to send email to $to: ${e.message}")
            logger.debug("Email content would have been: $content")
        }
    }

    /**
     * Builds a styled HTML email template
     */
    private fun buildHtmlEmailContent(content: String, actionUrl: String?, actionText: String): String {
        val paragraphs = content.split("\n\n").filter { it.isNotEmpty() }
        val paragraphHtml = paragraphs.joinToString("") { "<p style=\"margin: 0 0 16px 0; line-height: 1.5;\">$it</p>" }

        val buttonHtml = if (actionUrl != null) {
            """
            <div style="text-align: center; margin: 24px 0;">
                <a href="$actionUrl" 
                   style="display: inline-block; background-color: #3B82F6; color: white; 
                          font-weight: bold; padding: 12px 24px; text-decoration: none; 
                          border-radius: 4px; font-size: 16px;">
                    $actionText
                </a>
            </div>
            """
        } else {
            ""
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="text-align: center; margin-bottom: 24px;">
                    <img src="https://nihongo-it.com/logo.png" alt="Nihongo IT" width="180" style="margin-bottom: 16px;">
                    <h1 style="color: #3B82F6; margin: 0; font-size: 24px;">Nihongo IT</h1>
                    <p style="margin: 8px 0 0 0; color: #666;">Học tiếng Nhật chuyên ngành IT</p>
                </div>
                
                <div style="background-color: #f8f9fa; border-radius: 8px; padding: 24px; margin-bottom: 24px;">
                    $paragraphHtml
                    $buttonHtml
                </div>
                
                <div style="text-align: center; padding-top: 24px; border-top: 1px solid #eee; color: #888; font-size: 14px;">
                    <p>Nếu bạn không muốn nhận email này, vui lòng cập nhật <a href="$disableNotificationActionUrl" style="color: #3B82F6;">tùy chọn thông báo</a> của bạn.</p>
                </div>
            </div>
        </body>
        </html>
        """
    }

    /**
     * Sends a password change email to the user (simplified version)
     *
     * @param email The user's email address
     * @param resetToken The password reset token
     */
    @Async
    fun sendPasswordResetEmail(email: String, resetToken: String) {
        val resetUrl = "$frontendUrl/account/reset-password?token=$resetToken"

        sendPasswordResetEmail(email, resetToken, resetUrl)
    }

    /**
     * Sends a password change email to the user
     *
     * @param email The user's email address
     * @param resetToken The password reset token
     * @param resetUrl The password change URL with token
     */
    @Async
    @Suppress("UnusedParameter")
    fun sendPasswordResetEmail(email: String, resetToken: String, resetUrl: String) {
        val subject = "Password Change Request - Nihongo IT"
        val content = """
            Hello,
            
            You have requested to change your password for your Nihongo IT account.
            
            Please use the following link to change your password:
            $resetUrl
            
            This link will expire in 30 minutes.
            
            If you did not request a password change, please ignore this email.
            
            Best regards,
            The Nihongo IT Team
        """.trimIndent()

        try {
            logger.debug("Sending password change email to $email")

            val message = SimpleMailMessage()
            message.setFrom(senderEmail)
            message.setTo(email)
            message.setSubject(subject)
            message.setText(content)

            javaMailSender.send(message)

            logger.debug("Password change email sent successfully to $email")
        } catch (e: Exception) {
            logger.error("Failed to send password change email to $email: ${e.message}")
            logger.debug("Email content would have been: $content")
        }
    }

    /**
     * Get the latest notification of a specific type for a user
     *
     * @param user The user entity
     * @param type The notification type to search for
     * @return The latest notification of the specified type, or null if none found
     */
    fun getLastNotificationByType(user: UserEntity, type: NotificationType): NotificationEntity? {
        return notificationRepository.findFirstByUserAndTypeOrderBySentAtDesc(user, type)
    }

    companion object {
        private const val MAX_PAGE_SIZE = 50
    }

    fun listForUser(userId: UUID, page: Int, size: Int): Page<NotificationEntity> {
        val pageable = PageRequest.of(
            maxOf(0, page),
            size.coerceIn(1, MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "sentAt"),
        )
        return notificationRepository.findByUser_UserIdOrderBySentAtDesc(userId, pageable)
    }

    fun countUnreadForUser(userId: UUID): Long =
        notificationRepository.countByUser_UserIdAndIsReadFalse(userId)

    @Transactional
    fun markAsReadForUser(userId: UUID, notificationId: UUID): Boolean {
        val notification = notificationRepository.findByNotificationIdAndUser_UserId(notificationId, userId)
            ?: return false
        notificationRepository.save(notification.copy(isRead = true, readAt = LocalDateTime.now()))
        return true
    }

    @Transactional
    fun markAllAsReadForUser(userId: UUID): Int =
        notificationRepository.markAllReadByUserId(userId)

    @Transactional
    fun deleteForUser(userId: UUID, notificationId: UUID): Boolean =
        notificationRepository.deleteByIdAndUserId(notificationId, userId) > 0

    /**
     * Sends a flashcard review reminder email with special styling and card count information
     */
    @Async
    fun sendFlashcardReminderEmail(to: String, cardCount: Int, actionUrl: String) {
        val subject = "Nhắc nhở: $cardCount thẻ ghi nhớ cần ôn tập"

        // Build more targeted content for flashcard reminders
        val content = """
            Xin chào,
            
            Bạn có $cardCount thẻ ghi nhớ đang chờ được ôn tập.
            
            Nghiên cứu đã chỉ ra rằng việc ôn tập theo lịch trình sẽ giúp bạn ghi nhớ tốt hơn 80% so với học một lần. Hãy dành vài phút để ôn tập ngay bây giờ!
            
            Chúc bạn học tập hiệu quả,
            Đội ngũ Nihongo IT
        """.trimIndent()

        // More specific action text for flashcards
        val actionText = "Ôn tập $cardCount thẻ ngay"

        // Send the HTML email
        sendEmailNotification(to, subject, content, actionUrl, actionText)
    }
}
