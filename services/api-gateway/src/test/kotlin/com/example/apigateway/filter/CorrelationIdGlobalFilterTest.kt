package com.example.apigateway.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class CorrelationIdGlobalFilterTest {

    private lateinit var filter: CorrelationIdGlobalFilter
    private lateinit var chain: GatewayFilterChain

    @BeforeEach
    fun setup() {
        filter = CorrelationIdGlobalFilter()
        chain = mock()
        whenever(chain.filter(any())).thenReturn(Mono.empty())
    }

    @Test
    @DisplayName("no existing correlation ID → generates UUID and injects into request")
    fun noExistingId_generatesAndInjects() {
        val request = MockServerHttpRequest.get("/api/v1/learning/vocabulary").build()
        val exchange = MockServerWebExchange.from(request)

        val captor = argumentCaptor<ServerWebExchange>()
        whenever(chain.filter(captor.capture())).thenReturn(Mono.empty())

        filter.filter(exchange, chain).block()

        val mutated = captor.firstValue
        val correlationId = mutated.request.headers.getFirst(CORRELATION_ID_HEADER)
        assertNotNull(correlationId)
        // UUID format: 8-4-4-4-12
        val uuidPattern = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        assert(uuidPattern.matches(correlationId!!)) {
            "Expected UUID-format correlation ID but got: $correlationId"
        }
    }

    @Test
    @DisplayName("existing correlation ID in request → reused verbatim")
    fun existingId_reused() {
        val existingId = "existing-correlation-id-123"
        val request = MockServerHttpRequest.get("/api/v1/learning/vocabulary")
            .header(CORRELATION_ID_HEADER, existingId)
            .build()
        val exchange = MockServerWebExchange.from(request)

        val captor = argumentCaptor<ServerWebExchange>()
        whenever(chain.filter(captor.capture())).thenReturn(Mono.empty())

        filter.filter(exchange, chain).block()

        val mutated = captor.firstValue
        assertEquals(existingId, mutated.request.headers.getFirst(CORRELATION_ID_HEADER))
    }

    @Test
    @DisplayName("filter order is -200 (runs before JWT filter at -100)")
    fun filterOrder_isMinus200() {
        assertEquals(-200, filter.order)
    }

    @Test
    @DisplayName("chain.filter is always called — filter does not block the request")
    fun chainAlwaysCalled() {
        val request = MockServerHttpRequest.get("/any-path").build()
        val exchange = MockServerWebExchange.from(request)

        filter.filter(exchange, chain).block()

        verify(chain).filter(any())
    }
}
