package com.example.learningservice.exception

import com.example.learningservice.dto.ResponseDto
import com.example.learningservice.dto.ResponseType
import com.example.learningservice.exception.BusinessException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
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

    // Handle validation errors (e.g., invalid DTO fields)
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<*> {
        val errors = mutableMapOf<String, String?>()
        ex.bindingResult.allErrors.forEach { error ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.defaultMessage
            errors[fieldName] = errorMessage
        }
        logger.warn("Validation failed: $errors")
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                errors = errors,
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    // Handle JSON parsing errors in request body
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<*> {
        logger.warn("Malformed request body: ${ex.message}")

        val errors = mutableMapOf<String, String?>()

        // Check if it's a MissingKotlinParameterException (missing required field)
        val cause = ex.cause
        if (cause is MissingKotlinParameterException) {
            val fieldName = cause.parameter.name ?: "unknown"
            errors[fieldName] = "$fieldName is required"

            return ResponseEntity(
                ResponseDto(
                    status = ResponseType.NG,
                    errors = errors,
                ),
                HttpStatus.BAD_REQUEST,
            )
        }

        // Other JSON parsing errors
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = "Invalid request format.",
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    // Handle missing request parameters in URL
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(ex: MissingServletRequestParameterException): ResponseEntity<*> {
        val errors = mutableMapOf<String, String?>()
        errors[ex.parameterName] = "${ex.parameterName} is required"

        logger.warn("Missing parameter: ${ex.message}")
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = "Missing required parameter",
                errors = errors,
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    // Handle business exceptions
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<*> {
        logger.warn("Business exception: ${ex.message}")
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = ex.message ?: "An error occurred",
            ),
            HttpStatus.BAD_REQUEST,
        )
    }

    // Handle JWT expired exception
    @ExceptionHandler(ExpiredJwtException::class)
    fun handleExpiredJwtException(ex: ExpiredJwtException): ResponseEntity<*> {
        logger.warn("JWT token expired: ${ex.message}")
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = "Authentication token has expired",
            ),
            HttpStatus.FORBIDDEN,
        )
    }

    // Handle JWT signature exception
    @ExceptionHandler(SignatureException::class)
    fun handleJwtSignatureException(ex: SignatureException): ResponseEntity<*> {
        logger.warn("Invalid JWT signature: ${ex.message}")
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = "Invalid authentication token signature",
            ),
            HttpStatus.FORBIDDEN,
        )
    }

    // Handle malformed JWT exception
    @ExceptionHandler(MalformedJwtException::class)
    fun handleMalformedJwtException(ex: MalformedJwtException): ResponseEntity<*> {
        logger.warn("Malformed JWT token: ${ex.message}")
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = "Malformed authentication token",
            ),
            HttpStatus.FORBIDDEN,
        )
    }

    // Handle unsupported JWT exception
    @ExceptionHandler(UnsupportedJwtException::class)
    fun handleUnsupportedJwtException(ex: UnsupportedJwtException): ResponseEntity<*> {
        logger.warn("Unsupported JWT token: ${ex.message}")
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = "Unsupported authentication token format",
            ),
            HttpStatus.FORBIDDEN,
        )
    }

    // Handle access denied exception (insufficient privileges)
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<*> {
        logger.warn("Access denied: ${ex.message}")
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = "Access denied!",
            ),
            HttpStatus.FORBIDDEN,
        )
    }

    // Handle authentication exceptions
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<*> {
        val message = when (ex) {
            is BadCredentialsException -> "Invalid username or password"
            is InsufficientAuthenticationException -> "Authentication required"
            else -> "Authentication failed"
        }

        logger.warn("Authentication exception: ${ex.message}")
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = message,
            ),
            HttpStatus.UNAUTHORIZED,
        )
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException): ResponseEntity<*> {
        val resourcePath = ex.message?.replace("No static resource ", "") ?: "unknown"
        val message = "Resource not found: $resourcePath"

        logger.warn(message)
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = message,
            ),
            HttpStatus.NOT_FOUND,
        )
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupportedException(ex: HttpRequestMethodNotSupportedException): ResponseEntity<*> {
        val supportedMethods = ex.supportedMethods?.joinToString(", ") ?: "none"
        val message = "Request method '${ex.method}' is not supported. Supported methods: $supportedMethods"

        logger.warn(message)
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = message,
            ),
            HttpStatus.METHOD_NOT_ALLOWED,
        )
    }

    // Handle all other exceptions
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<*> {
        logger.error("Unexpected error occurred: ${ex.message}", ex)
        return ResponseEntity(
            ResponseDto(
                status = ResponseType.NG,
                message = "An unexpected error occurred",
            ),
            HttpStatus.INTERNAL_SERVER_ERROR,
        )
    }
}
