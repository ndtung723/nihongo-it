package com.example.learningservice.util

import com.example.learningservice.entity.JlptLevel
import com.example.learningservice.entity.RoleEntity
import com.example.learningservice.entity.UserEntity
import com.example.learningservice.security.JwtTokenUtil
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDateTime
import java.util.UUID

@Component
class UserAuthUtil(
    private val jwtTokenUtil: JwtTokenUtil,
) {
    private val logger = LoggerFactory.getLogger(UserAuthUtil::class.java)

    private fun currentRequest() = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request

    /**
     * Primary: read X-User-Id header injected by API Gateway.
     * Fallback: parse JWT directly (for local dev without gateway).
     */
    fun getCurrentUserId(): UUID? {
        val request = currentRequest()

        val headerUserId = request?.getHeader("X-User-Id")
        if (!headerUserId.isNullOrBlank()) {
            return try {
                UUID.fromString(headerUserId)
            } catch (e: IllegalArgumentException) {
                logger.warn("Invalid X-User-Id header value: $headerUserId")
                null
            }
        }

        // Fallback: JWT (local dev / direct service call)
        val token = getTokenFromRequest() ?: return null
        return getIdFromToken(token)
    }

    fun getCurrentRoleId(): Int? {
        val request = currentRequest()
        val headerRole = request?.getHeader("X-User-Role")
        if (!headerRole.isNullOrBlank()) {
            return if (headerRole == "ADMIN") 1 else 2
        }
        val token = getTokenFromRequest() ?: return null
        return getRoleIdFromToken(token)
    }

    fun getCurrentEmail(): String? {
        val request = currentRequest()
        val headerEmail = request?.getHeader("X-User-Email")
        if (!headerEmail.isNullOrBlank()) return headerEmail
        val token = getTokenFromRequest() ?: return null
        return getEmailFromToken(token)
    }

    fun isAuthenticated(): Boolean {
        val auth = SecurityContextHolder.getContext().authentication
        return auth != null && auth.isAuthenticated && auth.principal != "anonymousUser"
    }

    // ── JWT fallback helpers ───────────────────────────────────────────────

    fun getTokenFromRequest(): String? {
        val authHeader = currentRequest()?.getHeader("Authorization")
        return if (authHeader?.startsWith("Bearer ") == true) authHeader.substring(7) else null
    }

    fun getEmailFromToken(token: String): String? =
        try {
            jwtTokenUtil.extractEmail(token)
        } catch (e: Exception) {
            null
        }

    fun getIdFromToken(token: String): UUID? =
        try {
            val userId = jwtTokenUtil.extractClaim(token) { claims -> claims["userId"] as String }
            UUID.fromString(userId)
        } catch (e: Exception) {
            null
        }

    fun getRoleIdFromToken(token: String): Int? =
        try {
            jwtTokenUtil.extractRoleId(token)
        } catch (e: Exception) {
            null
        }

    fun getCurrentUser(): UserEntity? {
        val userId = getCurrentUserId() ?: return null
        val email = getCurrentEmail() ?: return null
        val roleId = getCurrentRoleId() ?: 2
        return UserEntity(
            userId = userId,
            email = email,
            password = "",
            fullName = "",
            role = RoleEntity(roleId, ""),
        )
    }

    fun getFullNameFromToken(token: String): String? =
        try {
            jwtTokenUtil.extractClaim(token) { claims -> claims["fullName"] as? String }
        } catch (e: Exception) {
            null
        }

    fun getCurrentLevel(): JlptLevel? {
        val token = getTokenFromRequest() ?: return null
        return try {
            jwtTokenUtil.extractClaim(token) { claims ->
                (claims["currentLevel"] as? String)?.let { JlptLevel.valueOf(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentGoal(): JlptLevel? {
        val token = getTokenFromRequest() ?: return null
        return try {
            jwtTokenUtil.extractClaim(token) { claims ->
                (claims["jlptGoal"] as? String)?.let { JlptLevel.valueOf(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentLastLogin(): LocalDateTime? {
        val token = getTokenFromRequest() ?: return null
        return try {
            jwtTokenUtil.extractClaim(token) { claims ->
                (claims["lastLogin"] as? String)?.let { LocalDateTime.parse(it) }
            }
        } catch (e: Exception) {
            null
        }
    }
}

interface CustomUserDetails {
    val userId: UUID
}
