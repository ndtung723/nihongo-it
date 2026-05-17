package com.example.apigateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class SecurityHeadersFilter :
    GlobalFilter,
    Ordered {
    companion object {
        // Runs last so its beforeCommit fires closest to response flush
        private const val FILTER_ORDER = 100
    }

    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        exchange.response.beforeCommit {
            val headers = exchange.response.headers

            fun addIfAbsent(
                name: String,
                value: String,
            ) {
                if (!headers.containsHeader(name)) headers.set(name, value)
            }
            addIfAbsent("X-Content-Type-Options", "nosniff")
            addIfAbsent("X-Frame-Options", "DENY")
            // Modern browsers ignore X-XSS-Protection; set to 0 to disable broken heuristics
            addIfAbsent("X-XSS-Protection", "0")
            addIfAbsent("Referrer-Policy", "strict-origin-when-cross-origin")
            addIfAbsent("Permissions-Policy", "camera=(), microphone=(), geolocation=()")
            Mono.empty<Void>()
        }
        return chain.filter(exchange)
    }

    override fun getOrder(): Int = FILTER_ORDER
}
