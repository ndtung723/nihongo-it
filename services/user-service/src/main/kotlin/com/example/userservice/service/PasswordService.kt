package com.example.userservice.service

import com.example.common.exception.BusinessException
import com.example.common.exception.UnauthorizedException
import com.example.userservice.dto.ChangePasswordRequestDto
import com.example.userservice.dto.PasswordResetRequestDto
import com.example.userservice.dto.PasswordResetResponseDto
import com.example.userservice.dto.ResetPasswordDto
import com.example.userservice.entity.AuditAction
import com.example.userservice.repository.UserRepository
import com.example.userservice.security.EmailRateLimiter
import com.example.userservice.util.UserAuthUtil
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class PasswordService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val notificationService: NotificationService,
    private val userAuthUtil: UserAuthUtil,
    private val emailRateLimiter: EmailRateLimiter,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(PasswordService::class.java)

    fun changePassword(request: ChangePasswordRequestDto): PasswordResetResponseDto {
        val email =
            userAuthUtil.getCurrentEmail()
                ?: throw UnauthorizedException("Authentication required. Please log in again.")

        val user =
            userRepository.findByEmail(email)
                ?: throw BusinessException("User not found")

        if (!passwordEncoder.matches(request.currentPassword, user.password)) {
            throw BusinessException("Current password is incorrect")
        }

        userRepository.save(
            user.copy(
                password = passwordEncoder.encode(request.newPassword)!!,
                updatedAt = LocalDateTime.now(),
            ),
        )

        auditService.log(AuditAction.PASSWORD_CHANGED, userId = user.userId, actorId = user.userId)
        logger.debug("Password changed for user: $email")
        return PasswordResetResponseDto(message = "Your password has been changed successfully")
    }

    fun requestPasswordReset(request: PasswordResetRequestDto): PasswordResetResponseDto {
        emailRateLimiter.checkAndRecord(request.email)
        val user = userRepository.findByEmail(request.email)
        if (user != null) {
            val resetToken = UUID.randomUUID().toString()
            userRepository.save(
                user.copy(
                    resetPasswordToken = resetToken,
                    resetPasswordExpires = LocalDateTime.now().plusMinutes(30),
                ),
            )
            notificationService.sendPasswordResetEmail(request.email, resetToken)
            auditService.log(AuditAction.PASSWORD_RESET_REQUESTED, userId = user.userId)
            logger.debug("Password reset token generated for: ${user.userId}")
        }
        // Always return OK — don't reveal whether email exists
        return PasswordResetResponseDto(
            message = "If an account exists with that email, we've sent password reset instructions.",
        )
    }

    fun resetPassword(request: ResetPasswordDto): PasswordResetResponseDto {
        if (request.password != request.confirmPassword) {
            throw BusinessException("Passwords do not match")
        }

        val user =
            userRepository.findByResetPasswordToken(request.token)
                ?: throw BusinessException("Invalid or expired reset token")

        val expires = user.resetPasswordExpires
        if (expires == null || expires.isBefore(LocalDateTime.now())) {
            throw BusinessException("Reset token has expired. Please request a new password reset.")
        }

        userRepository.save(
            user.copy(
                password = passwordEncoder.encode(request.password)!!,
                resetPasswordToken = null,
                resetPasswordExpires = null,
                updatedAt = LocalDateTime.now(),
            ),
        )

        auditService.log(AuditAction.PASSWORD_RESET_COMPLETED, userId = user.userId)
        logger.debug("Password reset for user: ${user.userId}")
        return PasswordResetResponseDto(
            message = "Your password has been reset successfully. You can now log in with your new password.",
        )
    }
}
