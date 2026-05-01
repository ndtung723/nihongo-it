package com.example.userservice.service

import com.example.userservice.repository.NotificationRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * Service for managing notifications for the Japanese IT vocabulary learning application
 * Currently implements email notifications with in-app notifications stored in database
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val javaMailSender: JavaMailSender
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
}