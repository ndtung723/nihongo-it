package com.example.userservice.service

import com.example.common.exception.BusinessException
import com.example.common.exception.UnauthorizedException
import com.example.common.logging.SecurityEventLogger
import com.example.userservice.dto.LoginRequest
import com.example.userservice.dto.RefreshTokenRequest
import com.example.userservice.dto.SignupRequest
import com.example.userservice.entity.JlptLevel
import com.example.userservice.entity.RefreshTokenEntity
import com.example.userservice.entity.RoleEntity
import com.example.userservice.entity.UserEntity
import com.example.userservice.repository.RefreshTokenRepository
import com.example.userservice.repository.RoleRepository
import com.example.userservice.repository.UserRepository
import com.example.userservice.security.JwtTokenUtil
import com.example.userservice.util.UserAuthUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthServiceTest {
    private lateinit var authenticationManager: AuthenticationManager
    private lateinit var userRepository: UserRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var jwtTokenUtil: JwtTokenUtil
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var googleAuthService: GoogleAuthService
    private lateinit var userAuthUtil: UserAuthUtil
    private lateinit var securityEventLogger: SecurityEventLogger
    private lateinit var auditService: AuditService
    private lateinit var notificationService: NotificationService
    private lateinit var authService: AuthService

    private val userId = UUID.randomUUID()
    private val userRole = RoleEntity(roleId = RoleEntity.ROLE_USER, roleName = "ROLE_USER")
    private val testUser =
        UserEntity(
            userId = userId,
            email = "test@example.com",
            password = "encoded_password",
            fullName = "Test User",
            profilePicture = null,
            currentLevel = null,
            jlptGoal = null,
            lastLogin = LocalDateTime.now(),
            role = userRole,
        )

    @BeforeEach
    fun setup() {
        authenticationManager = mock()
        userRepository = mock()
        roleRepository = mock()
        passwordEncoder = mock()
        jwtTokenUtil = mock()
        refreshTokenRepository = mock()
        googleAuthService = mock()
        userAuthUtil = mock()
        securityEventLogger = mock()
        auditService = mock()
        notificationService = mock()

        authService =
            AuthService(
                authenticationManager,
                userRepository,
                roleRepository,
                passwordEncoder,
                jwtTokenUtil,
                refreshTokenRepository,
                googleAuthService,
                userAuthUtil,
                securityEventLogger,
                auditService,
                notificationService,
            )
    }

    @Nested
    @DisplayName("login()")
    inner class Login {
        @Test
        @DisplayName("thành công — trả về token và refreshToken")
        fun success() {
            val request = LoginRequest(email = "test@example.com", password = "password123")
            val mockAuth = mock<org.springframework.security.core.Authentication>()

            whenever(userRepository.findByEmail(request.email)).thenReturn(testUser)
            whenever(authenticationManager.authenticate(any<UsernamePasswordAuthenticationToken>())).thenReturn(mockAuth)
            whenever(userRepository.save(any<UserEntity>())).thenReturn(testUser)
            whenever(jwtTokenUtil.generateToken(any<UserEntity>())).thenReturn("jwt_token")
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(
                RefreshTokenEntity(userId = userId, token = "refresh_token", expiresAt = LocalDateTime.now().plusDays(14)),
            )

            val result = authService.login(request, ip = "127.0.0.1")

            assertEquals("jwt_token", result.token)
            assertNotNull(result.refreshToken)
            verify(securityEventLogger).loginSuccess(request.email, "127.0.0.1")
        }

        @Test
        @DisplayName("email không tồn tại — ném UnauthorizedException")
        fun emailNotFound() {
            val request = LoginRequest(email = "noone@example.com", password = "pw")
            whenever(userRepository.findByEmail(request.email)).thenReturn(null)

            assertThrows<UnauthorizedException> {
                authService.login(request, ip = "127.0.0.1")
            }
            verify(securityEventLogger).loginFailure(request.email, "127.0.0.1", "user_not_found")
        }

        @Test
        @DisplayName("sai mật khẩu — ném UnauthorizedException")
        fun badCredentials() {
            val request = LoginRequest(email = "test@example.com", password = "wrong")
            whenever(userRepository.findByEmail(request.email)).thenReturn(testUser)
            whenever(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException("bad"))

            assertThrows<UnauthorizedException> {
                authService.login(request, ip = "192.168.1.1")
            }
            verify(securityEventLogger).loginFailure(request.email, "192.168.1.1", "bad_credentials")
        }
    }

    @Nested
    @DisplayName("register()")
    inner class Register {
        @Test
        @DisplayName("thành công — tạo user mới")
        fun success() {
            val request =
                SignupRequest(
                    email = "new@example.com",
                    password = "password123",
                    fullName = "New User",
                    profilePicture = null,
                    currentLevel = JlptLevel.N5,
                    jlptGoal = JlptLevel.N3,
                )
            whenever(userRepository.existsByEmail(request.email)).thenReturn(false)
            whenever(roleRepository.findByRoleId(RoleEntity.ROLE_USER)).thenReturn(userRole)
            whenever(passwordEncoder.encode(request.password)).thenReturn("encoded")
            whenever(userRepository.save(any<UserEntity>())).thenReturn(testUser)

            val result = authService.register(request)

            assertEquals("Registration successful. Please check your email to verify your account.", result.message)
            verify(userRepository).save(any())
        }

        @Test
        @DisplayName("email đã tồn tại — ném BusinessException")
        fun emailAlreadyUsed() {
            val request =
                SignupRequest(
                    email = "test@example.com",
                    password = "pw",
                    fullName = "Dup",
                    profilePicture = null,
                )
            whenever(userRepository.existsByEmail(request.email)).thenReturn(true)

            assertThrows<BusinessException> {
                authService.register(request)
            }
            verify(userRepository, never()).save(any())
        }
    }

    @Nested
    @DisplayName("refreshToken()")
    inner class RefreshToken {
        @Test
        @DisplayName("token hợp lệ — rotate và trả về access token mới")
        fun validToken() {
            val oldToken = "old_refresh"
            val storedToken =
                RefreshTokenEntity(
                    userId = userId,
                    token = oldToken,
                    expiresAt = LocalDateTime.now().plusDays(1),
                )
            whenever(refreshTokenRepository.findByToken(oldToken)).thenReturn(storedToken)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(testUser))
            whenever(jwtTokenUtil.generateToken(any<UserEntity>())).thenReturn("new_jwt")
            whenever(refreshTokenRepository.save(any<RefreshTokenEntity>())).thenReturn(
                RefreshTokenEntity(userId = userId, token = "new_refresh", expiresAt = LocalDateTime.now().plusDays(14)),
            )

            val result = authService.refreshToken(RefreshTokenRequest(refreshToken = oldToken))

            assertEquals("new_jwt", result.token)
            // Old token is marked as revoked (not deleted) to enable reuse detection
            verify(refreshTokenRepository).save(argThat<RefreshTokenEntity> { isRevoked && revokedAt != null })
        }

        @Test
        @DisplayName("token đã bị revoke — xóa family và ném UnauthorizedException")
        fun revokedTokenDetectsTheft() {
            val familyId = UUID.randomUUID()
            val revokedToken =
                RefreshTokenEntity(
                    userId = userId,
                    token = "revoked_token",
                    expiresAt = LocalDateTime.now().plusDays(1),
                    familyId = familyId,
                    isRevoked = true,
                    revokedAt = LocalDateTime.now().minusMinutes(5),
                )
            whenever(refreshTokenRepository.findByToken("revoked_token")).thenReturn(revokedToken)

            assertThrows<UnauthorizedException> {
                authService.refreshToken(RefreshTokenRequest(refreshToken = "revoked_token"))
            }
            verify(refreshTokenRepository).deleteByFamilyId(familyId)
        }

        @Test
        @DisplayName("token không tồn tại — ném UnauthorizedException")
        fun tokenNotFound() {
            whenever(refreshTokenRepository.findByToken("bad_token")).thenReturn(null)

            assertThrows<UnauthorizedException> {
                authService.refreshToken(RefreshTokenRequest(refreshToken = "bad_token"))
            }
        }

        @Test
        @DisplayName("token đã hết hạn — xóa và ném UnauthorizedException")
        fun expiredToken() {
            val expiredToken =
                RefreshTokenEntity(
                    userId = userId,
                    token = "expired",
                    expiresAt = LocalDateTime.now().minusHours(1),
                )
            whenever(refreshTokenRepository.findByToken("expired")).thenReturn(expiredToken)

            assertThrows<UnauthorizedException> {
                authService.refreshToken(RefreshTokenRequest(refreshToken = "expired"))
            }
            verify(refreshTokenRepository).delete(expiredToken)
        }
    }

    @Nested
    @DisplayName("logout()")
    inner class Logout {
        @Test
        @DisplayName("xóa refresh token khỏi DB")
        fun deletesToken() {
            val request = RefreshTokenRequest(refreshToken = "some_token")

            val result = authService.logout(request)

            verify(refreshTokenRepository).deleteByToken("some_token")
        }
    }

    @Nested
    @DisplayName("logoutAll()")
    inner class LogoutAll {
        @Test
        @DisplayName("xóa tất cả refresh token của user")
        fun deletesAllTokens() {
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(userId)

            authService.logoutAll()

            verify(refreshTokenRepository).deleteByUserId(userId)
        }

        @Test
        @DisplayName("chưa đăng nhập — ném UnauthorizedException")
        fun notAuthenticated() {
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(null)

            assertThrows<com.example.common.exception.UnauthorizedException> {
                authService.logoutAll()
            }
            verify(refreshTokenRepository, never()).deleteByUserId(any())
        }
    }
}
