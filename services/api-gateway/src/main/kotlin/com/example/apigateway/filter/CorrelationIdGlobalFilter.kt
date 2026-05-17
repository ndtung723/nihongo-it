package com.example.apigateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.UUID

const val CORRELATION_ID_HEADER = "X-Correlation-Id"

@Component
class CorrelationIdGlobalFilter :
    GlobalFilter,
    Ordered {
    companion object {
        private const val FILTER_ORDER = -200
    }

    private val logger = LoggerFactory.getLogger(CorrelationIdGlobalFilter::class.java)

    @Suppress("ForbiddenVoid")
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val correlationId =
            exchange.request.headers.getFirst(CORRELATION_ID_HEADER)
                ?: UUID.randomUUID().toString()

        logger.debug(
            "correlationId={} method={} path={}",
            correlationId,
            exchange.request.method,
            exchange.request.path.value(),
        )

        val mutatedRequest =
            exchange.request
                .mutate()
                .header(CORRELATION_ID_HEADER, correlationId)
                .build()

        return chain
            .filter(exchange.mutate().request(mutatedRequest).build())
            .then(
                Mono.fromRunnable {
                    exchange.response.headers.set(CORRELATION_ID_HEADER, correlationId)
                },
            )
    }

    override fun getOrder(): Int = FILTER_ORDER
}
