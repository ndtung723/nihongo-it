package com.example.userservice.service

import com.example.common.exception.BusinessException
import com.example.common.exception.UnauthorizedException
import com.example.common.logging.SecurityEventLogger
import com.example.userservice.dto.GetCurrentUserResponseDto
import com.example.userservice.dto.GoogleLoginRequest
import com.example.userservice.dto.LoginRequest
import com.example.userservice.dto.LoginResponseDto
import com.example.userservice.dto.RefreshTokenRequest
import com.example.userservice.dto.SignupRequest
import com.example.userservice.dto.SignupResponseDto
import com.example.userservice.dto.UserDto
import com.example.userservice.entity.AuditAction
import com.example.userservice.entity.RefreshTokenEntity
import com.example.userservice.entity.RoleEntity
import com.example.userservice.entity.UserEntity
import com.example.userservice.repository.RefreshTokenRepository
import com.example.userservice.repository.RoleRepository
import com.example.userservice.repository.UserRepository
import com.example.userservice.security.JwtTokenUtil
import com.example.userservice.util.UserAuthUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenUtil: JwtTokenUtil,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val googleAuthService: GoogleAuthService,
    private val userAuthUtil: UserAuthUtil,
    private val securityEventLogger: SecurityEventLogger,
    private val auditService: AuditService,
    private val notificationService: NotificationService,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Value("\${jwt.refresh-expiration}")
    private val refreshExpiration: Long = 1209600000L

    fun login(
        request: LoginRequest,
        ip: String = "unknown",
    ): LoginResponseDto {
        val user =
            userRepository.findByEmail(request.email)
                ?: run {
                    securityEventLogger.loginFailure(request.email, ip, "user_not_found")
                    auditService.log(AuditAction.LOGIN_FAILURE, ip = ip, details = "user_not_found email=${request.email}")
                    throw UnauthorizedException("Invalid email or password")
                }

        if (!passwordEncoder.matches(request.password, user.password)) {
            securityEventLogger.loginFailure(request.email, ip, "bad_credentials")
            auditService.log(AuditAction.LOGIN_FAILURE, userId = user.userId, ip = ip, details = "bad_credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        val updatedUser = user.copy(lastLogin = LocalDateTime.now())
        userRepository.save(updatedUser)

        val token = jwtTokenUtil.generateToken(updatedUser)
        val refreshToken = createRefreshToken(requireNotNull(updatedUser.userId) { "User ID missing after login" })

        securityEventLogger.loginSuccess(request.email, ip)
        auditService.log(AuditAction.LOGIN_SUCCESS, userId = updatedUser.userId, ip = ip)
        return LoginResponseDto(token = token, refreshToken = refreshToken)
    }

    fun register(request: SignupRequest): SignupResponseDto {
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException("Email is already in use")
        }

        val role = roleRepository.findByRoleId(RoleEntity.ROLE_USER) ?: throw BusinessException("Role not found")

        val verificationToken = UUID.randomUUID().toString()
        val user =
            UserEntity(
                email = request.email,
                password = passwordEncoder.encode(request.password)!!,
                fullName = request.fullName,
                profilePicture = request.profilePicture,
                currentLevel = request.currentLevel,
                jlptGoal = request.jlptGoal,
                lastLogin = LocalDateTime.now(),
                role = role,
                verificationToken = verificationToken,
                isEmailVerified = false,
            )
        userRepository.save(user)
        notificationService.sendVerificationEmail(request.email, verificationToken)

        logger.debug("User registered: ${request.email}")
        return SignupResponseDto(message = "Registration successful. Please check your email to verify your account.")
    }

    fun verifyEmail(token: String) {
        val user =
            userRepository.findByVerificationToken(token)
                ?: throw BusinessException("Invalid or expired verification token")

        userRepository.save(
            user.copy(
                isEmailVerified = true,
                verificationToken = null,
                updatedAt = LocalDateTime.now(),
            ),
        )
    }

    fun getCurrentUser(): GetCurrentUserResponseDto {
        val userId =
            userAuthUtil.getCurrentUserId()
                ?: throw BusinessException("Cannot extract userId: Invalid token")

        val user =
            userRepository
                .findById(userId)
                .orElseThrow { BusinessException("User not found") }

        val userInfo =
            UserDto(
                userId = requireNotNull(user.userId) { "User ID missing" },
                email = user.email,
                fullName = user.fullName,
                roleId = user.role.roleId,
                profilePicture = user.profilePicture,
                currentLevel = user.currentLevel,
                jlptGoal = user.jlptGoal,
                lastLogin = user.lastLogin,
            )

        return GetCurrentUserResponseDto(userInfo = userInfo)
    }

    fun googleLogin(request: GoogleLoginRequest): LoginResponseDto {
        val token = googleAuthService.handleGoogleLogin(request.tokenId)
        return LoginResponseDto(token = token)
    }

    fun refreshToken(request: RefreshTokenRequest): LoginResponseDto {
        val stored =
            refreshTokenRepository.findByToken(request.refreshToken)
                ?: throw UnauthorizedException("Invalid refresh token")

        if (stored.isRevoked) {
            // A previously rotated token is being reused — indicates token theft.
            // Revoke the entire session family to protect the legitimate user.
            refreshTokenRepository.deleteByFamilyId(stored.familyId)
            logger.warn("Refresh token reuse detected: familyId=${stored.familyId} userId=${stored.userId}")
            throw UnauthorizedException("Invalid or expired refresh token")
        }

        if (stored.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored)
            throw UnauthorizedException("Refresh token expired")
        }

        val user =
            userRepository.findById(stored.userId).orElse(null)
                ?: throw UnauthorizedException("User not found")

        // Mark the old token as revoked (kept for theft detection) rather than deleting it
        refreshTokenRepository.save(stored.copy(isRevoked = true, revokedAt = LocalDateTime.now()))

        val newAccessToken = jwtTokenUtil.generateToken(user)
        val newRefreshToken =
            createRefreshToken(
                requireNotNull(user.userId) { "User ID missing after refresh" },
                stored.familyId,
            )

        return LoginResponseDto(token = newAccessToken, refreshToken = newRefreshToken)
    }

    fun logout(request: RefreshTokenRequest) {
        refreshTokenRepository.deleteByToken(request.refreshToken)
        auditService.log(AuditAction.LOGOUT)
    }

    fun logoutAll() {
        val userId =
            userAuthUtil.getCurrentUserId()
                ?: throw UnauthorizedException("Authentication required")
        refreshTokenRepository.deleteByUserId(userId)
        auditService.log(AuditAction.LOGOUT_ALL, userId = userId, actorId = userId)
    }

    fun createRefreshToken(
        userId: UUID,
        familyId: UUID = UUID.randomUUID(),
    ): String {
        val token = UUID.randomUUID().toString()
        val expiresAt = LocalDateTime.now().plusSeconds(refreshExpiration / 1000)
        refreshTokenRepository.save(
            RefreshTokenEntity(userId = userId, token = token, expiresAt = expiresAt, familyId = familyId),
        )
        return token
    }
}
