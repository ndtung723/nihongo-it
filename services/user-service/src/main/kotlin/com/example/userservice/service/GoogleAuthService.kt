package com.example.userservice.service

import com.example.userservice.dto.GoogleUserInfo
import com.example.userservice.entity.JlptLevel
import com.example.userservice.entity.UserEntity
import com.example.common.exception.BusinessException
import com.example.userservice.repository.RoleRepository
import com.example.userservice.repository.UserRepository
import com.example.userservice.security.JwtTokenUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class GoogleAuthService(
    private val webClient: WebClient,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenUtil: JwtTokenUtil
) {
    private val logger = LoggerFactory.getLogger(GoogleAuthService::class.java)

    @Value("\${spring.security.oauth2.client.registration.google.client-id}")
    private lateinit var clientId: String

    /**
     * Verify Google ID token and extract user information
     */
    fun verifyGoogleToken(tokenId: String): GoogleUserInfo {
        try {
            logger.debug("Verifying Google token with Google's token info endpoint...")
            logger.debug("Using client ID: $clientId")
            
            // Use Google's token info endpoint to verify the token
            val response = webClient.get()
                .uri("https://oauth2.googleapis.com/tokeninfo?id_token=$tokenId")
                .retrieve()
                .bodyToMono<Map<String, Any>>()
                .block() ?: throw BusinessException("Failed to verify Google token")

            logger.debug("Token verification response received from Google: $response")
            
            // Check if token is issued for our app
            val audience = response["aud"] as? String
            if (audience != clientId) {
                logger.error("Token was not issued for this application. Expected: $clientId, Got: $audience")
                throw BusinessException("Invalid token audience")
            }

            // Extract user information
            val email = response["email"] as? String ?: throw BusinessException("Email not found in token")
            val name = response["name"] as? String ?: email.substringBefore("@")
            val picture = response["picture"] as? String
            val sub = response["sub"] as? String ?: throw BusinessException("Subject identifier not found in token")

            logger.debug("Successfully extracted user info from Google token: email=$email, name=$name")
            
            return GoogleUserInfo(
                email = email,
                name = name,
                picture = picture,
                sub = sub
            )
        } catch (e: Exception) {
            logger.error("Error verifying Google token: ${e.message}", e)
            throw BusinessException("Failed to verify Google token: ${e.message}")
        }
    }

    /**
     * Find or create a user based on Google information
     */
    fun findOrCreateUser(googleUserInfo: GoogleUserInfo): UserEntity {
        val existingUser = userRepository.findByEmail(googleUserInfo.email)
        
        if (existingUser != null) {
            logger.info("User with email ${googleUserInfo.email} already exists, returning existing user")
            
            // Update picture if necessary
            if (existingUser.profilePicture != googleUserInfo.picture && googleUserInfo.picture != null) {
                val updatedUser = existingUser.copy(
                    profilePicture = googleUserInfo.picture,
                    lastLogin = LocalDateTime.now()
                )
                return userRepository.save(updatedUser)
            }
            
            // Update last login time
            val updatedUser = existingUser.copy(lastLogin = LocalDateTime.now())
            return userRepository.save(updatedUser)
        }
        
        // Create a new user
        logger.info("Creating new user from Google account: ${googleUserInfo.email}")

        // Find the user role
        val role = roleRepository.findByRoleId(2)
            ?: throw BusinessException("Role not found")
            
        // Create a random, strong password for the user (they will log in via Google, not using this password)
        val randomPassword = UUID.randomUUID().toString() + UUID.randomUUID().toString()
        
        val newUser = UserEntity(
            email = googleUserInfo.email,
            password = passwordEncoder.encode(randomPassword),
            fullName = googleUserInfo.name,
            profilePicture = googleUserInfo.picture,
            currentLevel = JlptLevel.N5, // Default level for new users
            lastLogin = LocalDateTime.now(),
            role = role,
            jlptGoal = JlptLevel.N3, // Default goal for new users
        )
        
        return userRepository.save(newUser)
    }
    
    /**
     * Handle Google OAuth login, verifying token and generating JWT
     */
    fun handleGoogleLogin(tokenId: String): String {
        val googleUserInfo = verifyGoogleToken(tokenId)
        val user = findOrCreateUser(googleUserInfo)
        return jwtTokenUtil.generateToken(user)
    }
} 