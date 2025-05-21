package com.example.learningservice.security

import com.example.learningservice.dto.ResponseDto
import com.example.learningservice.dto.ResponseType
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExecutionChain
import org.springframework.web.servlet.HandlerMapping

@Component
class PreAuthFilterAspect(
    private val jwtTokenUtil: JwtTokenUtil,
    private val handlerMappings: List<HandlerMapping>,
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(PreAuthFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // Lấy annotation @PreAuthFilter từ handler method
        val preAuthFilter = getPreAuthFilterAnnotation(request)

        // Nếu không có annotation, tiếp tục xử lý
        if (preAuthFilter == null) {
            filterChain.doFilter(request, response)
            return
        }

        // Lấy thông tin từ token
        val authHeader = request.getHeader("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendErrorResponse(response, "Missing or invalid Authorization header", HttpStatus.FORBIDDEN)
            return
        }

        try {
            val jwt = authHeader.substring(7)
            val role = when (val roleId = jwtTokenUtil.extractRoleId(jwt)) {
                1 -> "ROLE_ADMIN"
                2 -> "ROLE_USER"
                else -> throw IllegalArgumentException("Invalid roleId: $roleId")
            }

            // Kiểm tra hasRole
            if (preAuthFilter.hasRole.isNotEmpty()) {
                val requiredRole = when (preAuthFilter.hasRole.lowercase()) {
                    "user" -> "ROLE_USER"
                    "admin" -> "ROLE_ADMIN"
                    else -> throw IllegalArgumentException("Invalid hasRole value: ${preAuthFilter.hasRole}")
                }
                if (role != requiredRole) {
                    sendErrorResponse(response, "Access Denied!", HttpStatus.FORBIDDEN)
                    return
                }
            }

            // Kiểm tra hasAnyRole
            if (preAuthFilter.hasAnyRole.isNotEmpty()) {
                val allowedRoles = preAuthFilter.hasAnyRole.map { roleName ->
                    when (roleName.lowercase()) {
                        "user" -> "ROLE_USER"
                        "admin" -> "ROLE_ADMIN"
                        else -> throw IllegalArgumentException("Invalid hasAnyRole value: $roleName")
                    }
                }
                if (role !in allowedRoles) {
                    sendErrorResponse(response, "Access Denied!", HttpStatus.FORBIDDEN)
                    return
                }
            }

            // Nếu qua được kiểm tra, tiếp tục xử lý
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            logger.error("Error in PreAuthFilter: ${e.message}")
            sendErrorResponse(response, "Error processing role check: ${e.message}", HttpStatus.FORBIDDEN)
            return
        }
    }

    private fun getPreAuthFilterAnnotation(request: HttpServletRequest): PreAuthFilter? {
        for (handlerMapping in handlerMappings) {
            try {
                val handlerExecutionChain: HandlerExecutionChain? = handlerMapping.getHandler(request)
                if (handlerExecutionChain != null) {
                    val handler = handlerExecutionChain.handler
                    if (handler is HandlerMethod) {
                        // Kiểm tra annotation trên method
                        val methodAnnotation = handler.getMethodAnnotation(PreAuthFilter::class.java)
                        if (methodAnnotation != null) {
                            return methodAnnotation
                        }
                        // Kiểm tra annotation trên class
                        val classAnnotation = handler.beanType.getAnnotation(PreAuthFilter::class.java)
                        if (classAnnotation != null) {
                            return classAnnotation
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Could not resolve handler for request: ${e.message}")
            }
        }
        return null
    }

    private fun sendErrorResponse(response: HttpServletResponse, message: String, status: HttpStatus) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        val responseDto = ResponseDto(
            status = ResponseType.NG,
            message = message,
        )
        val mapper = ObjectMapper()
        mapper.writeValue(response.outputStream, responseDto)
    }
}