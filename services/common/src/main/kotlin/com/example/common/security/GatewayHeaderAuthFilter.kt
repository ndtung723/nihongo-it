package com.example.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Reads X-User-Id and X-User-Role headers injected by API Gateway after JWT validation.
 * Downstream services trust these headers — the gateway is the single validation point.
 */
@Component
class GatewayHeaderAuthFilter : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(GatewayHeaderAuthFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val userId = request.getHeader("X-User-Id")
        val userRole = request.getHeader("X-User-Role")

        if (!userId.isNullOrBlank() && !userRole.isNullOrBlank()) {
            val authority = SimpleGrantedAuthority("ROLE_$userRole")
            val auth = UsernamePasswordAuthenticationToken(userId, null, listOf(authority))
            SecurityContextHolder.getContext().authentication = auth
            logger.debug("Authenticated via gateway header: userId=$userId role=$userRole")
        }

        chain.doFilter(request, response)
    }
}
