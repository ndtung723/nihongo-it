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
import com.example.userservice.dto.SignupRequest
import com.example.userservice.dto.SignupResponseDto
import com.example.userservice.dto.UpdateProfileRequestDto
import com.example.userservice.service.AuthService
import com.example.userservice.service.PasswordService
import com.example.userservice.service.UserService
import com.example.userservice.util.UserAuthUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

@RestController
@RequestMapping("/api/v1/user/auth")
@Tag(name = "Authentication", description = "API endpoints for user authentication, registration and profile management")
@Suppress("TooManyFunctions")
class AuthController(
    private val authService: AuthService,
    private val passwordService: PasswordService,
    private val userService: UserService,
    private val userAuthUtil: UserAuthUtil,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
    @Value("\${app.cookie.secure:false}") private val cookieSecure: Boolean,
) {

    @PostMapping("/login", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Authenticate user", description = "Authenticates a user with email and password, returns JWT token on success")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Authentication successful",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = LoginResponseDto::class))],
            ),
            ApiResponse(responseCode = "401", description = "Authentication failed - invalid credentials"),
        ],
    )
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): LoginResponseDto {
        val result = authService.login(
            request,
            httpRequest.getHeader("X-Forwarded-For") ?: httpRequest.remoteAddr,
        )
        result.refreshToken?.let { setRefreshTokenCookie(httpResponse, it) }
        return result.copy(refreshToken = null)
    }

    @PostMapping("/signup", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Register new user", description = "Creates a new user account with the provided details")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Registration successful",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = SignupResponseDto::class))],
            ),
            ApiResponse(responseCode = "400", description = "Invalid request data or email already in use"),
        ],
    )
    fun register(@Valid @RequestBody request: SignupRequest): SignupResponseDto =
        authService.register(request)

    @GetMapping("/verify-email", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Verify email address", description = "Confirms the user's email using the token sent during registration")
    fun verifyEmail(@RequestParam token: String): Map<String, String> {
        authService.verifyEmail(token)
        return mapOf("message" to "Email verified successfully")
    }

    @GetMapping("/current", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get current user information", description = "Retrieves detailed information about the currently authenticated user")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "User information retrieved successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetCurrentUserResponseDto::class))],
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            ApiResponse(responseCode = "404", description = "User not found"),
        ],
    )
    fun getCurrentUser(): GetCurrentUserResponseDto =
        authService.getCurrentUser()

    @PostMapping("/google-login", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Google OAuth login", description = "Authenticates a user with Google OAuth, returns JWT token on success")
    fun googleLogin(
        @Valid @RequestBody request: GoogleLoginRequest,
        httpResponse: HttpServletResponse,
    ): LoginResponseDto {
        val result = authService.googleLogin(request)
        result.refreshToken?.let { setRefreshTokenCookie(httpResponse, it) }
        return result.copy(refreshToken = null)
    }

    @PostMapping("/refresh-token", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Refresh access token", description = "Issues a new access token using the refresh token cookie")
    fun refreshToken(
        @RequestBody(required = false) request: RefreshTokenRequest?,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): LoginResponseDto {
        val cookieToken = getCookieValue(httpRequest, "refresh_token")
        val effectiveToken = cookieToken ?: request?.refreshToken
            ?: throw com.example.common.exception.UnauthorizedException("Refresh token is required")
        val result = authService.refreshToken(RefreshTokenRequest(refreshToken = effectiveToken))
        result.refreshToken?.let { setRefreshTokenCookie(httpResponse, it) }
        return result.copy(refreshToken = null)
    }

    @PostMapping("/logout", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Logout", description = "Invalidates the refresh token cookie")
    fun logout(
        @RequestBody(required = false) request: RefreshTokenRequest?,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): Map<String, String> {
        val cookieToken = getCookieValue(httpRequest, "refresh_token")
        val effectiveToken = cookieToken ?: request?.refreshToken ?: ""
        clearRefreshTokenCookie(httpResponse)
        authService.logout(RefreshTokenRequest(refreshToken = effectiveToken))
        return mapOf("message" to "Logged out successfully")
    }

    @PostMapping("/logout-all", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Logout all devices", description = "Revokes all refresh tokens for the current user, signing out all sessions")
    fun logoutAll(httpResponse: HttpServletResponse): Map<String, String> {
        clearRefreshTokenCookie(httpResponse)
        authService.logoutAll()
        return mapOf("message" to "Logged out from all devices successfully")
    }

    @PostMapping("/change-password", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Change password", description = "Changes the user's password using their current password")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Password changed successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = PasswordResetResponseDto::class))],
            ),
            ApiResponse(responseCode = "400", description = "Invalid current password"),
        ],
    )
    fun changePassword(@Valid @RequestBody request: ChangePasswordRequestDto): PasswordResetResponseDto =
        passwordService.changePassword(request)

    @PostMapping("/forgot-password", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Request password reset", description = "Initiates the password reset process by sending an email with reset instructions")
    fun requestPasswordReset(@Valid @RequestBody request: PasswordResetRequestDto): PasswordResetResponseDto =
        passwordService.requestPasswordReset(request)

    @PostMapping("/set-new-password", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "Reset password with token", description = "Resets the user's password using the token sent via email")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Password reset successful",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = PasswordResetResponseDto::class))],
            ),
            ApiResponse(responseCode = "400", description = "Invalid or expired token, or passwords don't match"),
        ],
    )
    fun resetPassword(@Valid @RequestBody request: ResetPasswordDto): PasswordResetResponseDto =
        passwordService.resetPassword(request)

    @PostMapping("/update-profile", produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Update user profile", description = "Updates the user's profile information (fullName, currentLevel, jlptGoal)")
    fun updateProfile(@Valid @RequestBody request: UpdateProfileRequestDto): Map<String, String> {
        val userId = userAuthUtil.getCurrentUserId()
            ?: throw com.example.common.exception.BusinessException("Cannot extract userId: Invalid token")
        userService.updateProfile(userId, request)
        return mapOf("message" to "Profile updated successfully")
    }

    private fun setRefreshTokenCookie(response: HttpServletResponse, token: String) {
        val cookie = ResponseCookie.from("refresh_token", token)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/api/v1/user/auth")
            .maxAge(Duration.ofSeconds(refreshExpiration / 1000))
            .sameSite("Lax")
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun clearRefreshTokenCookie(response: HttpServletResponse) {
        val cookie = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/api/v1/user/auth")
            .maxAge(0)
            .sameSite("Lax")
            .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun getCookieValue(request: HttpServletRequest, name: String): String? =
        request.cookies?.firstOrNull { it.name == name }?.value
}
