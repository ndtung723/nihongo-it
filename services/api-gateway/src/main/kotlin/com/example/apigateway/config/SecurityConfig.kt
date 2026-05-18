package com.example.apigateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    companion object {
        private const val CORS_MAX_AGE_SECONDS = 3600L
    }

    // Defaults cover all three coexisting frontends during the Next.js migration window:
    //   - 5173: legacy Vue (will be removed in Phase 12.4)
    //   - 3000: frontend-user (Next.js)
    //   - 3002: frontend-admin (Next.js) — host port 3002 because grafana takes 3001
    // Production CORS_ALLOWED_ORIGINS env var should list real domains.
    @Value("\${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000,http://localhost:3002}")
    private lateinit var allowedOrigins: String

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .cors { it.disable() } // handled by corsWebFilter bean
            .authorizeExchange {
                it.pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                it.pathMatchers("/**").permitAll() // JWT validation handled by GatewayJwtFilter
            }.build()

    @Bean
    fun corsWebFilter(): CorsWebFilter {
        val corsConfig = CorsConfiguration()

        val origins = allowedOrigins.split(",").map { it.trim() }
        if (origins.any { it.contains('*') }) {
            corsConfig.allowedOriginPatterns = origins
        } else {
            corsConfig.allowedOrigins = origins
        }

        corsConfig.addAllowedMethod("*")
        corsConfig.addAllowedHeader("*")
        corsConfig.addExposedHeader("Authorization")
        corsConfig.addExposedHeader("Content-Disposition")
        corsConfig.allowCredentials = true
        corsConfig.maxAge = CORS_MAX_AGE_SECONDS

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", corsConfig)
        return CorsWebFilter(source)
    }
}
