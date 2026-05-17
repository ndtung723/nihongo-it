package com.example.apigateway.filter

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.util.Date

class GatewayJwtFilterTest {
    private val secret = "test-secret-key-must-be-32-chars-min-for-hs256"
    private lateinit var filter: GatewayJwtFilter
    private lateinit var chain: GatewayFilterChain

    @BeforeEach
    fun setup() {
        filter = GatewayJwtFilter(secret = secret, previousSecret = "")
        chain = mock()
        whenever(chain.filter(any())).thenReturn(Mono.empty())
    }

    private fun buildToken(
        userId: String = "user-001",
        roleId: Int = 2,
        email: String = "user@example.com",
        expiryMs: Long = 60_000L,
    ): String {
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        return Jwts
            .builder()
            .subject(email)
            .claim("userId", userId)
            .claim("role", roleId)
            .issuedAt(Date())
            .expiration(Date(System.currentTimeMillis() + expiryMs))
            .signWith(key)
            .compact()
    }

    @Nested
    @DisplayName("public paths — no auth required")
    inner class PublicPaths {
        @Test
        @DisplayName("login path → chain.filter called without auth check")
        fun loginPath_passThrough() {
            val request = MockServerHttpRequest.get("/api/v1/user/auth/login").build()
            val exchange = MockServerWebExchange.from(request)

            filter.filter(exchange, chain).block()

            verify(chain).filter(exchange)
        }

        @Test
        @DisplayName("actuator path → pass through")
        fun actuatorPath_passThrough() {
            val request = MockServerHttpRequest.get("/actuator/health").build()
            val exchange = MockServerWebExchange.from(request)

            filter.filter(exchange, chain).block()

            verify(chain).filter(exchange)
        }

        @Test
        @DisplayName("OPTIONS preflight → always passes through")
        fun optionsMethod_passThrough() {
            val request = MockServerHttpRequest.options("/api/v1/learning/vocabulary").build()
            val exchange = MockServerWebExchange.from(request)

            filter.filter(exchange, chain).block()

            verify(chain).filter(exchange)
        }

        @Test
        @DisplayName("public AI speech path → pass through")
        fun aiSpeechPath_passThrough() {
            val request = MockServerHttpRequest.get("/api/v1/ai/speech/analyze").build()
            val exchange = MockServerWebExchange.from(request)

            filter.filter(exchange, chain).block()

            verify(chain).filter(exchange)
        }
    }

    @Nested
    @DisplayName("protected paths — auth required")
    inner class ProtectedPaths {
        @Test
        @DisplayName("missing Authorization header → 401 Unauthorized")
        fun missingAuthHeader_returns401() {
            val request = MockServerHttpRequest.get("/api/v1/learning/flashcards").build()
            val exchange = MockServerWebExchange.from(request)

            filter.filter(exchange, chain).block()

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
            verify(chain, never()).filter(any())
        }

        @Test
        @DisplayName("Authorization without Bearer prefix → 401")
        fun noBearerPrefix_returns401() {
            val request =
                MockServerHttpRequest
                    .get("/api/v1/learning/vocabulary")
                    .header("Authorization", "Basic dXNlcjpwYXNz")
                    .build()
            val exchange = MockServerWebExchange.from(request)

            filter.filter(exchange, chain).block()

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        }

        @Test
        @DisplayName("valid JWT (role=2 USER) → chain called, X-User-Role=USER injected")
        fun validUserJwt_injectsHeaders() {
            val token = buildToken(userId = "abc-123", roleId = 2, email = "u@test.com")
            val request =
                MockServerHttpRequest
                    .get("/api/v1/learning/vocabulary")
                    .header("Authorization", "Bearer $token")
                    .build()
            val exchange = MockServerWebExchange.from(request)

            val exchangeCaptor = argumentCaptor<ServerWebExchange>()
            whenever(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty())

            filter.filter(exchange, chain).block()

            verify(chain).filter(any())
            val mutated = exchangeCaptor.firstValue
            assertEquals("abc-123", mutated.request.headers.getFirst("X-User-Id"))
            assertEquals("USER", mutated.request.headers.getFirst("X-User-Role"))
            assertEquals("u@test.com", mutated.request.headers.getFirst("X-User-Email"))
        }

        @Test
        @DisplayName("valid JWT (role=1 ADMIN) → X-User-Role=ADMIN injected")
        fun validAdminJwt_injectsAdminRole() {
            val token = buildToken(userId = "admin-1", roleId = 1, email = "admin@test.com")
            val request =
                MockServerHttpRequest
                    .get("/api/v1/learning/admin/vocabulary")
                    .header("Authorization", "Bearer $token")
                    .build()
            val exchange = MockServerWebExchange.from(request)

            val exchangeCaptor = argumentCaptor<ServerWebExchange>()
            whenever(chain.filter(exchangeCaptor.capture())).thenReturn(Mono.empty())

            filter.filter(exchange, chain).block()

            val mutated = exchangeCaptor.firstValue
            assertEquals("ADMIN", mutated.request.headers.getFirst("X-User-Role"))
        }

        @Test
        @DisplayName("expired JWT → 401 Unauthorized")
        fun expiredJwt_returns401() {
            val expiredToken = buildToken(expiryMs = -1000L)
            val request =
                MockServerHttpRequest
                    .get("/api/v1/learning/vocabulary")
                    .header("Authorization", "Bearer $expiredToken")
                    .build()
            val exchange = MockServerWebExchange.from(request)

            filter.filter(exchange, chain).block()

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        }

        @Test
        @DisplayName("tampered / malformed JWT → 401 Unauthorized")
        fun malformedJwt_returns401() {
            val request =
                MockServerHttpRequest
                    .get("/api/v1/learning/vocabulary")
                    .header("Authorization", "Bearer not.a.valid.jwt.token")
                    .build()
            val exchange = MockServerWebExchange.from(request)

            filter.filter(exchange, chain).block()

            assertEquals(HttpStatus.UNAUTHORIZED, exchange.response.statusCode)
        }
    }
}
