package com.example.userservice.service

import com.example.userservice.dto.GetCurrentUserResponseDto
import com.example.userservice.dto.GoogleLoginRequest
import com.example.userservice.dto.LoginRequest
import com.example.userservice.dto.LoginResponseDto
import com.example.userservice.dto.RefreshTokenRequest
import com.example.common.dto.ResponseDto
import com.example.common.dto.ResponseType
import com.example.userservice.dto.SignupRequest
import com.example.userservice.dto.SignupResponseDto
import com.example.userservice.dto.UserDto
import com.example.userservice.entity.RefreshTokenEntity
import com.example.userservice.entity.UserEntity
import com.example.common.exception.BusinessException
import com.example.common.exception.UnauthorizedException
import com.example.userservice.repository.RefreshTokenRepository
import com.example.userservice.repository.RoleRepository
import com.example.userservice.repository.UserRepository
import com.example.userservice.security.JwtTokenUtil
import com.example.userservice.util.UserAuthUtil
import com.example.common.logging.SecurityEventLogger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenUtil: JwtTokenUtil,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val googleAuthService: GoogleAuthService,
    private val userAuthUtil: UserAuthUtil,
    private val securityEventLogger: SecurityEventLogger,
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Value("\${jwt.refresh-expiration}")
    private val refreshExpiration: Long = 1209600000L

    fun login(request: LoginRequest, ip: String = "unknown"): LoginResponseDto {
        val user = userRepository.findByEmail(request.email)
            ?: run {
                securityEventLogger.loginFailure(request.email, ip, "user_not_found")
                throw UnauthorizedException("Invalid email or password")
            }

        try {
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password)
            )
            SecurityContextHolder.getContext().authentication = authentication
        } catch (e: Exception) {
            securityEventLogger.loginFailure(request.email, ip, "bad_credentials")
            throw UnauthorizedException("Invalid email or password")
        }

        val updatedUser = user.copy(lastLogin = LocalDateTime.now())
        userRepository.save(updatedUser)

        val token = jwtTokenUtil.generateToken(updatedUser)
        val refreshToken = createRefreshToken(updatedUser.userId!!)

        securityEventLogger.loginSuccess(request.email, ip)
        return LoginResponseDto(token = token, refreshToken = refreshToken)
    }

    fun register(request: SignupRequest): SignupResponseDto {
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException("Email is already in use")
        }

        val role = roleRepository.findByRoleId(2) ?: throw BusinessException("Role not found")

        val user = UserEntity(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            profilePicture = request.profilePicture,
            currentLevel = request.currentLevel,
            jlptGoal = request.jlptGoal,
            lastLogin = LocalDateTime.now(),
            role = role,
        )
        userRepository.save(user)

        logger.debug("User registered: ${request.email}")
        return SignupResponseDto(message = "Registration successful")
    }

    fun getCurrentUser(): GetCurrentUserResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
            ?: throw BusinessException("Cannot extract userId: Invalid token")

        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException("User not found") }

        val userInfo = UserDto(
            userId = user.userId!!,
            email = user.email,
            fullName = user.fullName,
            roleId = user.role.roleId,
            profilePicture = user.profilePicture,
            currentLevel = user.currentLevel,
            jlptGoal = user.jlptGoal,
            lastLogin = user.lastLogin,
        )

        return GetCurrentUserResponseDto(
            ResponseDto(status = ResponseType.OK),
            userInfo = userInfo,
        )
    }

    fun googleLogin(request: GoogleLoginRequest): LoginResponseDto {
        val token = googleAuthService.handleGoogleLogin(request.tokenId)
        return LoginResponseDto(token = token)
    }

    fun refreshToken(request: RefreshTokenRequest): LoginResponseDto {
        val stored = refreshTokenRepository.findByToken(request.refreshToken)
            ?: throw UnauthorizedException("Invalid refresh token")

        if (stored.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored)
            throw UnauthorizedException("Refresh token expired")
        }

        val user = userRepository.findById(stored.userId).orElse(null)
            ?: throw UnauthorizedException("User not found")

        refreshTokenRepository.delete(stored)

        val newAccessToken = jwtTokenUtil.generateToken(user)
        val newRefreshToken = createRefreshToken(user.userId!!)

        return LoginResponseDto(token = newAccessToken, refreshToken = newRefreshToken)
    }

    fun logout(request: RefreshTokenRequest): ResponseDto {
        refreshTokenRepository.deleteByToken(request.refreshToken)
        return ResponseDto(status = ResponseType.OK)
    }

    fun createRefreshToken(userId: UUID): String {
        val token = UUID.randomUUID().toString()
        val expiresAt = LocalDateTime.now().plusSeconds(refreshExpiration / 1000)
        refreshTokenRepository.save(RefreshTokenEntity(userId = userId, token = token, expiresAt = expiresAt))
        return token
    }
}
