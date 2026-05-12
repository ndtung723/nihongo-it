package com.example.apigateway.filter

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class GatewayJwtFilter(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.secret-previous:}") private val previousSecret: String,
) : GlobalFilter, Ordered {
    companion object {
        private const val FILTER_ORDER = -100
    }

    private val logger = LoggerFactory.getLogger(GatewayJwtFilter::class.java)

    private val publicPaths = setOf(
        "/api/v1/user/auth/login",
        "/api/v1/user/auth/signup",
        "/api/v1/user/auth/google-login",
        "/api/v1/user/auth/forgot-password",
        "/api/v1/user/auth/set-new-password",
        "/api/v1/user/auth/reset-password",
        "/api/v1/user/auth/verify-email",
        "/api/v1/user/auth/refresh-token",
        "/api/v1/user/auth/logout",
    )

    private val publicPrefixes = listOf(
        "/v3/api-docs",
        "/swagger-ui",
        "/api-docs",
        "/actuator",
        "/api/v1/ai/speech/",
        // /api/v1/ai/tts/ intentionally removed — TTS costs money, requires auth
    )

    @Suppress("ForbiddenVoid", "ReturnCount")
    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val path = request.path.value()

        if (request.method == HttpMethod.OPTIONS) return chain.filter(exchange)

        if (publicPaths.contains(path) || publicPrefixes.any { path.startsWith(it) }) {
            return chain.filter(exchange)
        }

        val authHeader = request.headers.getFirst("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header for path: $path")
            return unauthorized(exchange)
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        val ip = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
        return try {
            val claims = extractClaims(token)
            val userId = claims["userId"] as? String ?: return unauthorized(exchange)
            val roleId = when (val role = claims["role"]) {
                is Int -> role
                is Long -> role.toInt()
                else -> 2
            }
            val roleName = if (roleId == 1) "ADMIN" else "USER"
            val email = claims.subject.orEmpty()

            val mutatedRequest = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Role", roleName)
                .header("X-User-Email", email)
                .build()

            logger.debug("JWT validated for userId=$userId role=$roleName path=$path")
            chain.filter(exchange.mutate().request(mutatedRequest).build())
        } catch (e: Exception) {
            logger.warn("auth.token.invalid path={} ip={} reason={}", path, ip, e.message)
            unauthorized(exchange)
        }
    }

    private fun extractClaims(token: String): Claims {
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        return try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).body
        } catch (e: Exception) {
            if (previousSecret.isBlank()) throw e
            val prevKey = Keys.hmacShaKeyFor(previousSecret.toByteArray())
            Jwts.parserBuilder().setSigningKey(prevKey).build().parseClaimsJws(token).body
        }
    }

    @Suppress("ForbiddenVoid")
    private fun unauthorized(exchange: ServerWebExchange): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.add("Content-Type", "application/json")
        val body = """{"error":"Unauthorized","message":"Invalid or missing authentication token"}"""
        val buffer = response.bufferFactory().wrap(body.toByteArray())
        return response.writeWith(Mono.just(buffer))
    }

    override fun getOrder(): Int = FILTER_ORDER
}
