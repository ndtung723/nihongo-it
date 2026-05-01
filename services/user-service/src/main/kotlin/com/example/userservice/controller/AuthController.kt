package com.example.userservice.controller

import com.example.userservice.dto.ChangePasswordRequestDto
import com.example.userservice.dto.GetCurrentUserResponseDto
import com.example.userservice.dto.GoogleLoginRequest
import com.example.userservice.dto.LoginRequest
import com.example.userservice.dto.LoginResponseDto
import com.example.userservice.dto.PasswordResetRequestDto
import com.example.userservice.dto.PasswordResetResponseDto
import com.example.userservice.dto.RefreshTokenRequest
import com.example.userservice.dto.ResetPasswordDto
import com.example.common.dto.ResponseDto
import com.example.userservice.dto.SignupRequest
import com.example.userservice.dto.SignupResponseDto
import com.example.userservice.dto.UpdateProfileRequestDto
import com.example.common.security.PreAuthFilter
import com.example.userservice.service.AuthService
import com.example.userservice.service.PasswordService
import com.example.userservice.service.UserService
import com.example.userservice.util.UserAuthUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/user/auth")
@Tag(name = "Authentication", description = "API endpoints for user authentication, registration and profile management")
class AuthController(
    private val authService: AuthService,
    private val passwordService: PasswordService,
    private val userService: UserService,
    private val userAuthUtil: UserAuthUtil,
) {

    @PostMapping("/login", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Authenticate user", description = "Authenticates a user with email and password, returns JWT token on success")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Authentication successful",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = LoginResponseDto::class))]),
        ApiResponse(responseCode = "401", description = "Authentication failed - invalid credentials")
    ])
    fun login(@Valid @RequestBody request: LoginRequest, httpRequest: jakarta.servlet.http.HttpServletRequest): LoginResponseDto =
        authService.login(request, httpRequest.getHeader("X-Forwarded-For") ?: httpRequest.remoteAddr)

    @PostMapping("/signup", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Register new user", description = "Creates a new user account with the provided details")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Registration successful",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = SignupResponseDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data or email already in use")
    ])
    fun register(@Valid @RequestBody request: SignupRequest): SignupResponseDto =
        authService.register(request)

    @GetMapping("/current", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthFilter(hasAnyRole = ["user", "admin"])
    @Operation(summary = "Get current user information", description = "Retrieves detailed information about the currently authenticated user")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User information retrieved successfully",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = GetCurrentUserResponseDto::class))]),
        ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    fun getCurrentUser(): GetCurrentUserResponseDto =
        authService.getCurrentUser()

    @PostMapping("/google-login", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Google OAuth login", description = "Authenticates a user with Google OAuth, returns JWT token on success")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Authentication successful",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = LoginResponseDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid Google token")
    ])
    fun googleLogin(@Valid @RequestBody request: GoogleLoginRequest): LoginResponseDto =
        authService.googleLogin(request)

    @PostMapping("/refresh-token", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): LoginResponseDto =
        authService.refreshToken(request)

    @PostMapping("/logout", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Logout", description = "Invalidates the refresh token")
    fun logout(@Valid @RequestBody request: RefreshTokenRequest): ResponseDto =
        authService.logout(request)

    @PostMapping("/change-password", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthFilter(hasAnyRole = ["user", "admin"])
    @Operation(summary = "Change password", description = "Changes the user's password using their current password")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password changed successfully",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PasswordResetResponseDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid current password")
    ])
    fun changePassword(@Valid @RequestBody request: ChangePasswordRequestDto): PasswordResetResponseDto =
        passwordService.changePassword(request)

    @PostMapping("/forgot-password", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Request password reset", description = "Initiates the password reset process by sending an email with reset instructions")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password reset email sent if email exists",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PasswordResetResponseDto::class))])
    ])
    fun requestPasswordReset(@Valid @RequestBody request: PasswordResetRequestDto): PasswordResetResponseDto =
        passwordService.requestPasswordReset(request)

    @PostMapping("/set-new-password", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Reset password with token", description = "Resets the user's password using the token sent via email")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password reset successful",
            content = [Content(mediaType = "application/json", schema = Schema(implementation = PasswordResetResponseDto::class))]),
        ApiResponse(responseCode = "400", description = "Invalid or expired token, or passwords don't match")
    ])
    fun resetPassword(@Valid @RequestBody request: ResetPasswordDto): PasswordResetResponseDto =
        passwordService.resetPassword(request)

    @PostMapping("/update-profile", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthFilter(hasAnyRole = ["USER", "ADMIN"])
    @Operation(summary = "Update user profile", description = "Updates the user's profile information (fullName, currentLevel, jlptGoal)")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Profile updated successfully",
            content = [Content(mediaType = "application/json")]),
        ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        ApiResponse(responseCode = "404", description = "User not found")
    ])
    fun updateProfile(@Valid @RequestBody request: UpdateProfileRequestDto): ResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
            ?: throw com.example.common.exception.BusinessException("Cannot extract userId: Invalid token")
        return userService.updateProfile(userId, request)
    }
}
