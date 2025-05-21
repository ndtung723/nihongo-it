package com.example.userservice.controller

import com.example.userservice.dto.ChangePasswordRequestDto
import com.example.userservice.dto.GetCurrentUserResponseDto
import com.example.userservice.dto.GoogleLoginRequest
import com.example.userservice.dto.LoginRequest
import com.example.userservice.dto.LoginResponseDto
import com.example.userservice.dto.PasswordResetRequestDto
import com.example.userservice.dto.PasswordResetResponseDto
import com.example.userservice.dto.ResetPasswordDto
import com.example.userservice.dto.ResponseDto
import com.example.userservice.dto.ResponseType
import com.example.userservice.dto.SignupRequest
import com.example.userservice.dto.SignupResponseDto
import com.example.userservice.dto.UpdateProfileRequestDto
import com.example.userservice.dto.UserDto
import com.example.userservice.entity.UserEntity
import com.example.userservice.exception.BusinessException
import com.example.userservice.repository.RoleRepository
import com.example.userservice.repository.UserRepository
import com.example.userservice.security.JwtTokenUtil
import com.example.userservice.security.PreAuthFilter
import com.example.userservice.service.GoogleAuthService
import com.example.userservice.service.NotificationService
import com.example.userservice.util.UserAuthUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID
import java.security.Principal

@RestController
@RequestMapping("/user-service-api/v1/auth")
@Tag(name = "Authentication", description = "API endpoints for user authentication, registration and profile management")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenUtil: JwtTokenUtil,
    private val userAuthUtil: UserAuthUtil,
    private val googleAuthService: GoogleAuthService,
    private val notificationService: NotificationService,
) {
    private val logger = LoggerFactory.getLogger(AuthController::class.java)

    @PostMapping("/login", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Authenticate user",
        description = "Authenticates a user with email and password, returns JWT token on success"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = LoginResponseDto::class))]
            ),
            ApiResponse(responseCode = "401", description = "Authentication failed - invalid credentials")
        ]
    )
    fun login(
        @Parameter(description = "Login credentials", required = true)
        @Valid @RequestBody request: LoginRequest
    ): LoginResponseDto {
        logger.debug("Login attempt for email: ${request.email}")

        val user = userRepository.findByEmail(request.email)
        if (user == null) {
            logger.debug("Login failed: User with email ${request.email} not found")
            return LoginResponseDto(
                result = ResponseType.NG,
            )
        }

        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password),
        )

        SecurityContextHolder.getContext().authentication = authentication

        // Update lastLogin time
        val updatedUser = user.copy(lastLogin = LocalDateTime.now())
        userRepository.save(updatedUser)
        logger.debug("Updated lastLogin for user with email: ${request.email}")

        val token = jwtTokenUtil.generateToken(updatedUser)
        logger.debug("Login successful for user with email: ${request.email}")

        return LoginResponseDto(
            result = ResponseType.OK,
            token,
        )
    }

    @PostMapping("/signup", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Register new user",
        description = "Creates a new user account with the provided details"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Registration successful",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = SignupResponseDto::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid request data or email already in use")
        ]
    )
    fun register(
        @Parameter(description = "User registration details", required = true)
        @Valid @RequestBody request: SignupRequest
    ): SignupResponseDto {
        try {
            // Check if email already exists
            if (userRepository.existsByEmail(request.email)) {
                logger.debug("Signup failed: Email ${request.email} already exists")
                throw BusinessException("Email is already in use")
            }

            // Find the user role
            val role = roleRepository.findByRoleId(2)
                ?: throw BusinessException("Role not found")

            // Create user entity
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

            // Save user
            val savedUser = userRepository.save(user)
            logger.debug("User registered successfully with ID: ${savedUser.userId}")

            return SignupResponseDto(
                status = ResponseType.OK,
                message = "Registration successful"
            )
        } catch (ex: Exception) {
            logger.error("Registration error: ${ex.message}", ex)
            throw ex
        }
    }

    @GetMapping("/current", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthFilter(hasAnyRole = ["user", "admin"])
    @Operation(
        summary = "Get current user information",
        description = "Retrieves detailed information about the currently authenticated user"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User information retrieved successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetCurrentUserResponseDto::class))]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            ApiResponse(responseCode = "404", description = "User not found")
        ]
    )
    fun getCurrentUser(): GetCurrentUserResponseDto {
        try {
            // Get current user ID
            val userId = userAuthUtil.getCurrentUserId()
                ?: throw BusinessException("Cannot extract userId: Invalid token")

            // Fetch full user details from database
            val user = userRepository.findById(userId)
                .orElseThrow { BusinessException("User not found") }

            // Map to response DTO
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

            logger.debug("Successfully retrieved user info for user ID: {}", userId)

            return GetCurrentUserResponseDto(
                ResponseDto(
                    status = ResponseType.OK,
                ),
                userInfo = userInfo,
            )
        } catch (e: Exception) {
            logger.error("Error retrieving user info: ${e.message}", e)
            throw e
        }
    }

    @PostMapping("/google-login", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Google OAuth login",
        description = "Authenticates a user with Google OAuth, returns JWT token on success"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = LoginResponseDto::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid Google token")
        ]
    )
    fun googleLogin(
        @Parameter(description = "Google token ID", required = true)
        @Valid @RequestBody request: GoogleLoginRequest
    ): LoginResponseDto {
        logger.debug("Google login attempt with token")

        try {
            // Verify and process the Google token
            val token = googleAuthService.handleGoogleLogin(request.tokenId)

            logger.debug("Google login successful")
            return LoginResponseDto(
                result = ResponseType.OK,
                token = token
            )
        } catch (e: Exception) {
            logger.error("Google login error: ${e.message}", e)
            return LoginResponseDto(
                result = ResponseType.NG,
            )
        }
    }

    @PostMapping("/change-password", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthFilter(hasAnyRole = ["user", "admin"])
    @Operation(
        summary = "Change password",
        description = "Changes the user's password using their current password"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Password changed successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = PasswordResetResponseDto::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid current password")
        ]
    )
    fun changePassword(
        @Parameter(description = "Password change request", required = true)
        @Valid @RequestBody request: ChangePasswordRequestDto
    ): PasswordResetResponseDto {
        // Get user email from the JWT token or principal
        val email = userAuthUtil.getCurrentEmail()
        
        logger.debug("Change password request for user: $email")

        if (email == null) {
            logger.error("Authentication token is missing or invalid")
            return PasswordResetResponseDto(
                status = ResponseType.NG,
                message = "Authentication required. Please log in again."
            )
        }

        try {
            // Get the authenticated user
            val user = userRepository.findByEmail(email)
                ?: return PasswordResetResponseDto(
                    status = ResponseType.NG,
                    message = "User not found"
                )

            // Verify that the current password is correct
            if (!passwordEncoder.matches(request.currentPassword, user.password)) {
                return PasswordResetResponseDto(
                    status = ResponseType.NG,
                    message = "Current password is incorrect"
                )
            }

            // Update the user's password
            val updatedUser = user.copy(
                userId = user.userId,
                email = user.email,
                password = passwordEncoder.encode(request.newPassword),
                fullName = user.fullName,
                profilePicture = user.profilePicture,
                currentLevel = user.currentLevel,
                jlptGoal = user.jlptGoal,
                isActive = user.isActive,
                isEmailVerified = user.isEmailVerified,
                verificationToken = user.verificationToken,
                resetPasswordToken = user.resetPasswordToken,
                resetPasswordExpires = user.resetPasswordExpires,
                lastLogin = user.lastLogin,
                streakCount = user.streakCount,
                lastStudyDate = user.lastStudyDate,
                points = user.points,
                dailyGoalMinutes = user.dailyGoalMinutes,
                reminderEnabled = user.reminderEnabled,
                reminderTime = user.reminderTime,
                notificationPreferences = user.notificationPreferences,
                firebaseToken = user.firebaseToken,
                role = user.role,
                flashcards = user.flashcards,
                createdAt = user.createdAt,
                updatedAt = LocalDateTime.now()
            )
            userRepository.save(updatedUser)

            logger.debug("Password changed successfully for user ID: ${user.userId}")

            return PasswordResetResponseDto(
                status = ResponseType.OK,
                message = "Your password has been changed successfully"
            )
        } catch (e: Exception) {
            logger.error("Error changing password: ${e.message}", e)
            return PasswordResetResponseDto(
                status = ResponseType.NG,
                message = "An unexpected error occurred. Please try again."
            )
        }
    }

    @PostMapping("/forgot-password", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Request password reset",
        description = "Initiates the password reset process by sending an email with reset instructions"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Password reset email sent if email exists",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = PasswordResetResponseDto::class))]
            )
        ]
    )
    fun requestPasswordReset(
        @Parameter(description = "Password reset request", required = true)
        @Valid @RequestBody request: PasswordResetRequestDto
    ): PasswordResetResponseDto {
        logger.debug("Password reset request for email: ${request.email}")
        
        try {
            // Find user by email
            val user = userRepository.findByEmail(request.email)
            
            // If user exists, generate reset token and send email
            if (user != null) {
                // Generate reset token (UUID)
                val resetToken = UUID.randomUUID().toString()
                
                // Set token expiration (30 minutes from now)
                val tokenExpiration = LocalDateTime.now().plusMinutes(30)
                
                // Update user with reset token and expiration
                val updatedUser = user.copy(
                    resetPasswordToken = resetToken,
                    resetPasswordExpires = tokenExpiration
                )
                userRepository.save(updatedUser)
                
                // Send password reset email with token
                notificationService.sendPasswordResetEmail(request.email, resetToken)
                
                logger.debug("Password reset token generated for user: ${user.userId}")
            } else {
                logger.debug("Password reset requested for non-existent email: ${request.email}")
                // Don't reveal that the email doesn't exist for security reasons
            }
            
            // Always return success even if email doesn't exist (security best practice)
            return PasswordResetResponseDto(
                status = ResponseType.OK,
                message = "If an account exists with that email, we've sent password reset instructions."
            )
        } catch (e: Exception) {
            logger.error("Error processing password reset request: ${e.message}", e)
            return PasswordResetResponseDto(
                status = ResponseType.NG,
                message = "An unexpected error occurred. Please try again."
            )
        }
    }
    
    @PostMapping("/set-new-password", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Reset password with token",
        description = "Resets the user's password using the token sent via email"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Password reset successful",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = PasswordResetResponseDto::class))]
            ),
            ApiResponse(responseCode = "400", description = "Invalid or expired token, or passwords don't match")
        ]
    )
    fun resetPassword(
        @Parameter(description = "Reset password details", required = true)
        @Valid @RequestBody request: ResetPasswordDto
    ): PasswordResetResponseDto {
        logger.debug("Reset password request with token")
        
        try {
            // Validate password and confirmation match
            if (request.password != request.confirmPassword) {
                return PasswordResetResponseDto(
                    status = ResponseType.NG,
                    message = "Passwords do not match"
                )
            }
            
            // Find user by reset token
            val user = userRepository.findByResetPasswordToken(request.token)
                ?: return PasswordResetResponseDto(
                    status = ResponseType.NG,
                    message = "Invalid or expired reset token"
                )
            
            // Check if token is expired
            if (user.resetPasswordExpires == null || user.resetPasswordExpires!!.isBefore(LocalDateTime.now())) {
                return PasswordResetResponseDto(
                    status = ResponseType.NG,
                    message = "Reset token has expired. Please request a new password reset."
                )
            }
            
            // Update user's password and clear reset token
            val updatedUser = user.copy(
                password = passwordEncoder.encode(request.password),
                resetPasswordToken = null,
                resetPasswordExpires = null,
                updatedAt = LocalDateTime.now()
            )
            userRepository.save(updatedUser)
            
            logger.debug("Password reset successfully for user ID: ${user.userId}")
            
            return PasswordResetResponseDto(
                status = ResponseType.OK,
                message = "Your password has been reset successfully. You can now log in with your new password."
            )
        } catch (e: Exception) {
            logger.error("Error resetting password: ${e.message}", e)
            return PasswordResetResponseDto(
                status = ResponseType.NG,
                message = "An unexpected error occurred. Please try again."
            )
        }
    }

    @PostMapping("/update-profile", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthFilter(hasAnyRole = ["USER", "ADMIN"])
    @Operation(
        summary = "Update user profile",
        description = "Updates the user's profile information (fullName, currentLevel, jlptGoal)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Profile updated successfully",
                content = [Content(mediaType = "application/json")]
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            ApiResponse(responseCode = "404", description = "User not found")
        ]
    )
    fun updateProfile(
        @Parameter(description = "Profile update data", required = true)
        @Valid @RequestBody request: UpdateProfileRequestDto
    ): ResponseDto {
        try {
            // Get current user ID
            val userId = userAuthUtil.getCurrentUserId()
                ?: throw BusinessException("Cannot extract userId: Invalid token")

            // Fetch full user details from database
            val user = userRepository.findById(userId)
                .orElseThrow { BusinessException("User not found") }


            // Validate JLPT levels (Current level should be equal to or lower than goal level)
            // For JLPT, N5 is lowest, N1 is highest (typically ordinals: N5=4, N4=3, N3=2, N2=1, N1=0)
            // Current level ordinal should be >= goal level ordinal to ensure current level ≤ goal level
            if (request.currentLevel.ordinal > request.jlptGoal.ordinal) {
                throw BusinessException("Current level cannot be higher than goal level")
            }

            // Update user fields
            val updatedUser = user.copy(
                fullName = request.fullName,
                currentLevel = request.currentLevel,
                jlptGoal = request.jlptGoal,
                updatedAt = LocalDateTime.now()
            )
            
            userRepository.save(updatedUser)

            logger.debug("Successfully updated profile for user ID: {}", userId)

            return ResponseDto(
                status = ResponseType.OK,
                message = "Profile updated successfully"
            )
        } catch (e: Exception) {
            logger.error("Error updating user profile: ${e.message}", e)
            throw e
        }
    }
}
