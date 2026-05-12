package com.example.userservice.service

import com.example.common.exception.BusinessException
import com.example.common.exception.UnauthorizedException
import com.example.userservice.dto.ChangePasswordRequestDto
import com.example.userservice.dto.PasswordResetRequestDto
import com.example.userservice.dto.ResetPasswordDto
import com.example.userservice.entity.RoleEntity
import com.example.userservice.entity.UserEntity
import com.example.userservice.repository.UserRepository
import com.example.userservice.security.EmailRateLimiter
import com.example.userservice.util.UserAuthUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var notificationService: NotificationService
    private lateinit var userAuthUtil: UserAuthUtil
    private lateinit var emailRateLimiter: EmailRateLimiter
    private lateinit var auditService: AuditService
    private lateinit var passwordService: PasswordService

    private val userId = UUID.randomUUID()
    private val userRole = RoleEntity(roleId = RoleEntity.ROLE_USER, roleName = "ROLE_USER")
    private val testUser = UserEntity(
        userId = userId,
        email = "test@example.com",
        password = "encoded_old_password",
        fullName = "Test User",
        profilePicture = null,
        currentLevel = null,
        jlptGoal = null,
        lastLogin = LocalDateTime.now(),
        role = userRole,
    )

    @BeforeEach
    fun setup() {
        userRepository = mock()
        passwordEncoder = mock()
        notificationService = mock()
        userAuthUtil = mock()
        emailRateLimiter = mock()
        auditService = mock()

        passwordService = PasswordService(
            userRepository, passwordEncoder, notificationService, userAuthUtil, emailRateLimiter, auditService,
        )
    }

    @Nested
    @DisplayName("changePassword()")
    inner class ChangePassword {

        @Test
        @DisplayName("thành công — cập nhật password mới")
        fun success() {
            val request = ChangePasswordRequestDto(currentPassword = "old_pass", newPassword = "new_pass", confirmPassword = "new_pass")
            whenever(userAuthUtil.getCurrentEmail()).thenReturn("test@example.com")
            whenever(userRepository.findByEmail("test@example.com")).thenReturn(testUser)
            whenever(passwordEncoder.matches("old_pass", testUser.password)).thenReturn(true)
            whenever(passwordEncoder.encode("new_pass")).thenReturn("encoded_new")
            whenever(userRepository.save(any<UserEntity>())).thenReturn(testUser)

            val result = passwordService.changePassword(request)

            assertEquals("Your password has been changed successfully", result.message)
            verify(userRepository).save(argThat<UserEntity> { password == "encoded_new" })
        }

        @Test
        @DisplayName("current password sai — ném BusinessException")
        fun wrongCurrentPassword() {
            val request = ChangePasswordRequestDto(currentPassword = "wrong", newPassword = "new_pass", confirmPassword = "new_pass")
            whenever(userAuthUtil.getCurrentEmail()).thenReturn("test@example.com")
            whenever(userRepository.findByEmail("test@example.com")).thenReturn(testUser)
            whenever(passwordEncoder.matches("wrong", testUser.password)).thenReturn(false)

            assertThrows<BusinessException> {
                passwordService.changePassword(request)
            }
            verify(userRepository, never()).save(any())
        }

        @Test
        @DisplayName("chưa đăng nhập — ném UnauthorizedException")
        fun notAuthenticated() {
            whenever(userAuthUtil.getCurrentEmail()).thenReturn(null)

            assertThrows<UnauthorizedException> {
                passwordService.changePassword(ChangePasswordRequestDto("old", "new", "new"))
            }
        }
    }

    @Nested
    @DisplayName("requestPasswordReset()")
    inner class RequestPasswordReset {

        @Test
        @DisplayName("email tồn tại — gửi email và lưu token")
        fun emailExists() {
            whenever(userRepository.findByEmail("test@example.com")).thenReturn(testUser)
            whenever(userRepository.save(any<UserEntity>())).thenReturn(testUser)

            val result = passwordService.requestPasswordReset(PasswordResetRequestDto(email = "test@example.com"))

            assertTrue(result.message.contains("If an account exists"))
            verify(userRepository).save(argThat<UserEntity> { resetPasswordToken != null })
            verify(notificationService).sendPasswordResetEmail(eq("test@example.com"), any())
        }

        @Test
        @DisplayName("email không tồn tại — vẫn trả OK (không lộ thông tin)")
        fun emailNotFound() {
            whenever(userRepository.findByEmail("nobody@example.com")).thenReturn(null)

            val result = passwordService.requestPasswordReset(PasswordResetRequestDto(email = "nobody@example.com"))

            assertTrue(result.message.contains("If an account exists"))
            verify(userRepository, never()).save(any())
            verify(notificationService, never()).sendPasswordResetEmail(any(), any())
        }
    }

    @Nested
    @DisplayName("resetPassword()")
    inner class ResetPassword {

        @Test
        @DisplayName("token hợp lệ — cập nhật password")
        fun validToken() {
            val resetToken = "valid_token"
            val userWithToken = testUser.copy(
                resetPasswordToken = resetToken,
                resetPasswordExpires = LocalDateTime.now().plusMinutes(15),
            )
            val request = ResetPasswordDto(token = resetToken, password = "new_pass", confirmPassword = "new_pass")
            whenever(userRepository.findByResetPasswordToken(resetToken)).thenReturn(userWithToken)
            whenever(passwordEncoder.encode("new_pass")).thenReturn("encoded_new")
            whenever(userRepository.save(any<UserEntity>())).thenReturn(userWithToken)

            val result = passwordService.resetPassword(request)

            assertTrue(result.message.contains("reset successfully"))
            verify(userRepository).save(
                argThat<UserEntity> {
                    password == "encoded_new" && resetPasswordToken == null
                },
            )
        }

        @Test
        @DisplayName("password và confirmPassword không khớp — ném BusinessException")
        fun passwordMismatch() {
            val request = ResetPasswordDto(token = "tok", password = "pass1", confirmPassword = "pass2")

            assertThrows<BusinessException> {
                passwordService.resetPassword(request)
            }
        }

        @Test
        @DisplayName("token đã hết hạn — ném BusinessException")
        fun expiredToken() {
            val resetToken = "expired_token"
            val userWithExpiredToken = testUser.copy(
                resetPasswordToken = resetToken,
                resetPasswordExpires = LocalDateTime.now().minusMinutes(5),
            )
            whenever(userRepository.findByResetPasswordToken(resetToken)).thenReturn(userWithExpiredToken)

            assertThrows<BusinessException> {
                passwordService.resetPassword(ResetPasswordDto(token = resetToken, password = "new", confirmPassword = "new"))
            }
        }

        @Test
        @DisplayName("token không tồn tại — ném BusinessException")
        fun invalidToken() {
            whenever(userRepository.findByResetPasswordToken("bad")).thenReturn(null)

            assertThrows<BusinessException> {
                passwordService.resetPassword(ResetPasswordDto(token = "bad", password = "new", confirmPassword = "new"))
            }
        }
    }
}
