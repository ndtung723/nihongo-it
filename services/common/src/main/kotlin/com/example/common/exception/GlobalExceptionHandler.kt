package com.example.common.exception

import com.example.common.dto.ErrorResponseDto
import com.example.common.dto.FieldErrorDto
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.SignatureException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<*> {
        val errors = ex.bindingResult.allErrors.map { error ->
            FieldErrorDto(
                field = (error as FieldError).field,
                message = error.defaultMessage.orEmpty(),
            )
        }
        logger.warn("Validation failed: $errors")
        return ResponseEntity(
            ErrorResponseDto(errors = errors),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<*> {
        logger.warn("Malformed request body: ${ex.message}")

        val cause = ex.cause
        if (cause is MismatchedInputException) {
            val fieldName = cause.path?.firstOrNull()?.fieldName ?: "unknown"
            val errors = listOf(FieldErrorDto(field = fieldName, message = "$fieldName is required"))
            return ResponseEntity(
                ErrorResponseDto(errors = errors),
                HttpStatus.BAD_REQUEST,
            )
        }

        return ResponseEntity(
            ErrorResponseDto(message = "Invalid request format."),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(ex: MissingServletRequestParameterException): ResponseEntity<*> {
        val errors = listOf(FieldErrorDto(field = ex.parameterName, message = "${ex.parameterName} is required"))
        logger.warn("Missing parameter: ${ex.message}")
        return ResponseEntity(
            ErrorResponseDto(message = "Missing required parameter", errors = errors),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(ex: UnauthorizedException): ResponseEntity<*> {
        logger.warn("Unauthorized: ${ex.message}")
        return ResponseEntity(
            ErrorResponseDto(message = ex.message ?: "Unauthorized"),
            HttpStatus.UNAUTHORIZED,
        )
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<*> {
        logger.warn("Business exception: ${ex.message}")
        return ResponseEntity(
            ErrorResponseDto(message = ex.message ?: "An error occurred"),
            HttpStatus.BAD_REQUEST,
        )
    }

    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredJwtException(ex: ExpiredJwtException): ResponseEntity<*> {
        logger.warn("JWT token expired: ${ex.message}")
        return ResponseEntity(
            ErrorResponseDto(message = "Authentication token has expired"),
            HttpStatus.UNAUTHORIZED,
        )
    }

    @ExceptionHandler(SignatureException::class)
    fun handleJwtSignatureException(ex: SignatureException): ResponseEntity<*> {
        logger.warn("Invalid JWT signature: ${ex.message}")
        return ResponseEntity(
            ErrorResponseDto(message = "Invalid authentication token signature"),
            HttpStatus.UNAUTHORIZED,
        )
    }

    @ExceptionHandler(MalformedJwtException::class)
    fun handleMalformedJwtException(ex: MalformedJwtException): ResponseEntity<*> {
        logger.warn("Malformed JWT token: ${ex.message}")
        return ResponseEntity(
            ErrorResponseDto(message = "Malformed authentication token"),
            HttpStatus.UNAUTHORIZED,
        )
    }

    @ExceptionHandler(UnsupportedJwtException::class)
    fun handleUnsupportedJwtException(ex: UnsupportedJwtException): ResponseEntity<*> {
        logger.warn("Unsupported JWT token: ${ex.message}")
        return ResponseEntity(
            ErrorResponseDto(message = "Unsupported authentication token format"),
            HttpStatus.UNAUTHORIZED,
        )
    }

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<*> {
        logger.warn("Access denied: ${ex.message}")
        return ResponseEntity(
            ErrorResponseDto(message = "Access denied!"),
            HttpStatus.FORBIDDEN,
        )
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<*> {
        val message = when (ex) {
            is BadCredentialsException -> "Invalid username or password"
            is InsufficientAuthenticationException -> "Authentication required"
            else -> "Authentication failed"
        }
        logger.warn("Authentication exception: ${ex.message}")
        return ResponseEntity(
            ErrorResponseDto(message = message),
            HttpStatus.UNAUTHORIZED,
        )
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException): ResponseEntity<*> {
        val resourcePath = ex.message?.replace("No static resource ", "") ?: "unknown"
        val message = "Resource not found: $resourcePath"
        logger.warn(message)
        return ResponseEntity(
            ErrorResponseDto(message = message),
            HttpStatus.NOT_FOUND,
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupportedException(ex: HttpRequestMethodNotSupportedException): ResponseEntity<*> {
        val supportedMethods = ex.supportedMethods?.joinToString(", ") ?: "none"
        val message = "Request method '${ex.method}' is not supported. Supported methods: $supportedMethods"
        logger.warn(message)
        return ResponseEntity(
            ErrorResponseDto(message = message),
            HttpStatus.METHOD_NOT_ALLOWED,
        )
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<*> {
        logger.error("Unexpected error occurred: ${ex.message}", ex)
        return ResponseEntity(
            ErrorResponseDto(message = "An unexpected error occurred"),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}
