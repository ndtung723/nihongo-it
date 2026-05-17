package com.example.common.security

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class GatewayHeaderAuthFilterTest {
    private lateinit var filter: GatewayHeaderAuthFilter
    private lateinit var chain: FilterChain

    @BeforeEach
    fun setup() {
        filter = GatewayHeaderAuthFilter()
        chain = mock()
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Nested
    @DisplayName("valid gateway headers")
    inner class ValidHeaders {
        @Test
        @DisplayName("USER role → ROLE_USER authority + userId as principal name")
        fun userRole_setsCorrectAuthority() {
            val request =
                MockHttpServletRequest().apply {
                    addHeader("X-User-Id", "user-123")
                    addHeader("X-User-Role", "USER")
                }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            val auth = SecurityContextHolder.getContext().authentication
            assertNotNull(auth)
            assertEquals("user-123", auth!!.name)
            assertEquals(listOf("ROLE_USER"), auth.authorities.map { it.authority })
            verify(chain).doFilter(request, response)
        }

        @Test
        @DisplayName("ADMIN role → ROLE_ADMIN authority")
        fun adminRole_setsCorrectAuthority() {
            val request =
                MockHttpServletRequest().apply {
                    addHeader("X-User-Id", "admin-456")
                    addHeader("X-User-Role", "ADMIN")
                }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            val auth = SecurityContextHolder.getContext().authentication
            assertNotNull(auth)
            assertEquals(listOf("ROLE_ADMIN"), auth!!.authorities.map { it.authority })
        }

        @Test
        @DisplayName("UUID-formatted userId → stored verbatim as principal name")
        fun uuidUserId_storedVerbatim() {
            val userId = "550e8400-e29b-41d4-a716-446655440000"
            val request =
                MockHttpServletRequest().apply {
                    addHeader("X-User-Id", userId)
                    addHeader("X-User-Role", "USER")
                }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            assertEquals(userId, SecurityContextHolder.getContext().authentication?.name)
        }
    }

    @Nested
    @DisplayName("missing or blank headers")
    inner class MissingHeaders {
        @Test
        @DisplayName("missing X-User-Id → no authentication set, chain continues")
        fun missingUserId_noAuth() {
            val request =
                MockHttpServletRequest().apply {
                    addHeader("X-User-Role", "USER")
                }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            assertNull(SecurityContextHolder.getContext().authentication)
            verify(chain).doFilter(request, response)
        }

        @Test
        @DisplayName("missing X-User-Role → no authentication set, chain continues")
        fun missingRole_noAuth() {
            val request =
                MockHttpServletRequest().apply {
                    addHeader("X-User-Id", "user-123")
                }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            assertNull(SecurityContextHolder.getContext().authentication)
            verify(chain).doFilter(request, response)
        }

        @Test
        @DisplayName("blank X-User-Id → no authentication set")
        fun blankUserId_noAuth() {
            val request =
                MockHttpServletRequest().apply {
                    addHeader("X-User-Id", "   ")
                    addHeader("X-User-Role", "USER")
                }
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            assertNull(SecurityContextHolder.getContext().authentication)
        }

        @Test
        @DisplayName("both headers missing → no authentication set, chain continues")
        fun bothHeadersMissing_noAuth() {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()

            filter.doFilter(request, response, chain)

            assertNull(SecurityContextHolder.getContext().authentication)
            verify(chain).doFilter(request, response)
        }
    }
}
