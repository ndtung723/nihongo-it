package com.example.userservice.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenUtil: JwtTokenUtil,
    private val userDetailsService: UserDetailsService,
) : OncePerRequestFilter() {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val requestURI = request.requestURI

            if (shouldNotFilter(request)) {
                filterChain.doFilter(request, response)
                return
            }

            val authHeader = request.getHeader("Authorization")
            if (authHeader?.startsWith("Bearer ") == true) {
                val jwt = authHeader.substring(7)
                val email = jwtTokenUtil.extractEmail(jwt)

                if (email.isNotEmpty() && SecurityContextHolder.getContext().authentication == null) {
                    val userDetails = userDetailsService.loadUserByUsername(email)

                    if (jwtTokenUtil.validateToken(jwt, userDetails)) {
                        val roleId = jwtTokenUtil.extractRoleId(jwt)
                        val authentication = UsernamePasswordAuthenticationToken(
                            userDetails,
                            roleId,
                            userDetails.authorities,
                        )
                        authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = authentication
                        logger.debug("Authenticated user $email with role ${userDetails.authorities}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Cannot set user authentication: ${e.message}")
            SecurityContextHolder.clearContext()
        }

        filterChain.doFilter(request, response)
    }

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/user-service-api/v1/auth/") ||
                return path.startsWith("/ai-service-api/v1/speech/") ||
                path.startsWith("/ai-service-api/v1/tts/") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui")
    }
}