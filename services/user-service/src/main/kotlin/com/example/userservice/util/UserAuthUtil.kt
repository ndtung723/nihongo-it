package com.example.userservice.util

import com.example.userservice.entity.JlptLevel
import com.example.userservice.entity.RoleEntity
import com.example.userservice.entity.UserEntity
import com.example.userservice.security.JwtTokenUtil
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

    // Get token from Authorization header
    fun getTokenFromRequest(): String? {
        val request = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
        val authHeader = request?.getHeader("Authorization")
        return if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authHeader.substring(7) // Remove "Bearer "
        } else {
            logger.warn("No valid Authorization header found")
            null
        }
    }

    /**
     * Extract email from token
     */
    fun getEmailFromToken(token: String): String? =
        try {
            jwtTokenUtil.extractEmail(token)
        } catch (e: Exception) {
            logger.error("Failed to extract email from token: ${e.message}")
            null
        }

    /**
     * Extract full name from token
     */
    fun getFullNameFromToken(token: String): String? =
        try {
            jwtTokenUtil.extractClaim(token) { claims -> claims["fullName"] as? String }
        } catch (e: Exception) {
            logger.error("Failed to extract fullName from token: ${e.message}")
            null
        }

    /**
     * Extract profile picture from token
     */
    fun getProfilePictureFromToken(token: String): String? =
        try {
            jwtTokenUtil.extractClaim(token) { claims -> claims["profilePicture"] as? String }
        } catch (e: Exception) {
            logger.error("Failed to extract profilePicture from token: ${e.message}")
            null
        }

    /**
     * Extract current level from token
     */
    fun getCurrentLevelFromToken(token: String): JlptLevel? =
        try {
            jwtTokenUtil.extractClaim(token) { claims ->
                (claims["currentLevel"] as? String)?.let { JlptLevel.valueOf(it) }
            }
        } catch (e: Exception) {
            logger.error("Failed to extract currentLevel from token: ${e.message}")
            null
        }

    /**
     * Extract JLPT goal from token
     */
    fun getJlptGoalFromToken(token: String): JlptLevel? =
        try {
            jwtTokenUtil.extractClaim(token) { claims ->
                (claims["jlptGoal"] as? String)?.let { JlptLevel.valueOf(it) }
            }
        } catch (e: Exception) {
            logger.error("Failed to extract jlptGoal from token: ${e.message}")
            null
        }

    /**
     * Extract last login from token
     */
    fun getLastLoginFromToken(token: String): LocalDateTime? =
        try {
            jwtTokenUtil.extractClaim(token) { claims ->
                (claims["lastLogin"] as? String)?.let { LocalDateTime.parse(it) }
            }
        } catch (e: Exception) {
            logger.error("Failed to extract lastLogin from token: ${e.message}")
            null
        }

    /**
     * Extract id from token
     */
    fun getIdFromToken(token: String): UUID? =
        try {
            val userId = jwtTokenUtil.extractClaim(token) { claims -> claims["userId"] as String }
            UUID.fromString(userId)
        } catch (e: Exception) {
            logger.error("Failed to extract userId from token: ${e.message}", e)
            null
        }

    /**
     * Extract roleId from token
     */
    fun getRoleIdFromToken(token: String): Int? =
        try {
            jwtTokenUtil.extractRoleId(token)
        } catch (e: Exception) {
            logger.error("Failed to extract roleId from token: ${e.message}")
            null
        }

    /**
     * Create a UserEntity from token (if we need the entire user information)
     */
    fun getUserFromToken(token: String): UserEntity? {
        return try {
            val userId = getIdFromToken(token)
            val email = getEmailFromToken(token)
            val roleId = getRoleIdFromToken(token)
            val fullName = getFullNameFromToken(token).orEmpty()
            val profilePicture = getProfilePictureFromToken(token)
            val currentLevel = getCurrentLevelFromToken(token)
            val jlptGoal = getJlptGoalFromToken(token)
            val lastLogin = getLastLoginFromToken(token) ?: LocalDateTime.now()

            if (userId == null || email == null || roleId == null) {
                logger.warn("Incomplete user data extracted from token: id=$userId, email=$email, roleId=$roleId")
                return null
            }

            // Create a complete UserEntity with all fields
            UserEntity(
                userId = userId,
                email = email,
                password = "", // Empty password as we don't store it in the token
                fullName = fullName,
                profilePicture = profilePicture,
                currentLevel = currentLevel,
                jlptGoal = jlptGoal,
                lastLogin = lastLogin,
                role = RoleEntity(roleId, ""), // RoleEntity should be fetched from DB in real use case
            )
        } catch (e: Exception) {
            logger.error("Failed to create UserEntity from token: ${e.message}")
            null
        }
    }

    /**
     * Get user information from Authorization header
     */
    fun getCurrentUser(): UserEntity? {
        val token = getTokenFromRequest() ?: return null
        return getUserFromToken(token)
    }

    /**
     * Get email from Authorization header
     */
    fun getCurrentEmail(): String? {
        val token = getTokenFromRequest() ?: return null
        return getEmailFromToken(token)
    }

    /**
     * Get full name from Authorization header
     */
    fun getCurrentFullName(): String? {
        val token = getTokenFromRequest() ?: return null
        return getFullNameFromToken(token)
    }

    /**
     * Get profile picture from Authorization header
     */
    fun getCurrentProfilePicture(): String? {
        val token = getTokenFromRequest() ?: return null
        return getProfilePictureFromToken(token)
    }

    /**
     * Get current level from Authorization header
     */
    fun getCurrentLevel(): JlptLevel? {
        val token = getTokenFromRequest() ?: return null
        return getCurrentLevelFromToken(token)
    }

    /**
     * Get JLPT goal from Authorization header
     */
    fun getCurrentGoal(): JlptLevel? {
        val token = getTokenFromRequest() ?: return null
        return getJlptGoalFromToken(token)
    }

    /**
     * Get last login from Authorization header
     */
    fun getCurrentLastLogin(): LocalDateTime? {
        val token = getTokenFromRequest() ?: return null
        return getLastLoginFromToken(token)
    }

    /**
     * Get roleId from Authorization header
     */
    fun getCurrentRoleId(): Int? {
        val token = getTokenFromRequest() ?: return null
        return getRoleIdFromToken(token)
    }

    /**
     * Get userId from Authorization header or security context
     */
    fun getCurrentUserId(): UUID? {
        // First try to get from token
        val token = getTokenFromRequest()
        if (token != null) {
            logger.debug("Attempting to extract userId from token")
            val userId = getIdFromToken(token)

            if (userId != null) {
                logger.debug("Successfully extracted userId from token: $userId")
                return userId
            } else {
                logger.warn("Failed to extract userId from token, falling back to security context")
            }
        }

        // If token extraction failed, try from security context
        try {
            val authentication =
                SecurityContextHolder.getContext().authentication
                    ?: throw RuntimeException("User not authenticated")

            val principal = authentication.principal
            if (principal is CustomUserDetails) {
                return principal.userId
            }

            // Try to get from authorities
            val userIdString =
                authentication.authorities
                    .find { it.authority?.startsWith("USER_ID:") == true }
                    ?.authority
                    ?.substring("USER_ID:".length)

            return userIdString?.let { UUID.fromString(it) }
                ?: throw RuntimeException("User ID not found in authentication")
        } catch (e: Exception) {
            logger.error("Failed to get userId from security context: ${e.message}")
            return null
        }
    }

    /**
     * Check if the current user is authenticated
     * @return true if user is authenticated, false otherwise
     */
    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null &&
            authentication.isAuthenticated &&
            authentication.principal != "anonymousUser"
    }
}

/**
 * CustomUserDetails interface - should match your application's user details implementation
 */
interface CustomUserDetails {
    val userId: UUID
}
