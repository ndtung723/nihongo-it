package com.example.common.security

import com.example.common.dto.ResponseDto
import com.example.common.dto.ResponseType
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerExecutionChain
import org.springframework.web.servlet.HandlerMapping

@Component
class PreAuthFilterAspect(
    private val handlerMappings: List<HandlerMapping>,
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(PreAuthFilterAspect::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val preAuthFilter = getPreAuthFilterAnnotation(request)

        if (preAuthFilter == null) {
            filterChain.doFilter(request, response)
            return
        }

        val auth = SecurityContextHolder.getContext().authentication
        if (auth == null || !auth.isAuthenticated || auth.principal == "anonymousUser") {
            sendErrorResponse(response, "Unauthorized", HttpStatus.UNAUTHORIZED)
            return
        }

        val role = auth.authorities.firstOrNull()?.authority ?: ""

        if (preAuthFilter.hasRole.isNotEmpty()) {
            val requiredRole = "ROLE_${preAuthFilter.hasRole.uppercase()}"
            if (role != requiredRole) {
                sendErrorResponse(response, "Access Denied!", HttpStatus.FORBIDDEN)
                return
            }
        }

        if (preAuthFilter.hasAnyRole.isNotEmpty()) {
            val allowedRoles = preAuthFilter.hasAnyRole.map { "ROLE_${it.uppercase()}" }
            if (role !in allowedRoles) {
                sendErrorResponse(response, "Access Denied!", HttpStatus.FORBIDDEN)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun getPreAuthFilterAnnotation(request: HttpServletRequest): PreAuthFilter? {
        for (handlerMapping in handlerMappings) {
            try {
                val chain: HandlerExecutionChain? = handlerMapping.getHandler(request)
                if (chain != null) {
                    val handler = chain.handler
                    if (handler is HandlerMethod) {
                        val methodAnnotation = handler.getMethodAnnotation(PreAuthFilter::class.java)
                        if (methodAnnotation != null) return methodAnnotation
                        val classAnnotation = handler.beanType.getAnnotation(PreAuthFilter::class.java)
                        if (classAnnotation != null) return classAnnotation
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
        ObjectMapper().writeValue(
            response.outputStream,
            ResponseDto(status = ResponseType.NG, message = message),
        )
    }
}
