package com.example.common.exception

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

class GlobalExceptionHandlerTest {

    private lateinit var mockMvc: MockMvc

    @RestController
    @RequestMapping("/test")
    class TestController {
        data class NameRequest(@field:NotBlank(message = "name must not be blank") val name: String)

        @GetMapping("/business")
        fun business(): String = throw BusinessException("Test business error")

        @GetMapping("/unauthorized")
        fun unauthorized(): String = throw UnauthorizedException("Not authorized")

        @GetMapping("/access-denied")
        fun accessDenied(): String = throw AccessDeniedException("Forbidden")

        @GetMapping("/generic")
        fun generic(): String = throw UnsupportedOperationException("Something broke")

        @PostMapping("/validate", consumes = ["application/json"])
        fun validate(@Valid @RequestBody body: NameRequest): String = body.name
    }

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(TestController())
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Nested
    @DisplayName("BusinessException")
    inner class BusinessExceptions {

        @Test
        @DisplayName("→ 400 with message field")
        fun returns400WithMessage() {
            mockMvc.get("/test/business") {
                accept(MediaType.APPLICATION_JSON)
            }.andExpect {
                status { isBadRequest() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.message") { value("Test business error") }
            }
        }
    }

    @Nested
    @DisplayName("UnauthorizedException")
    inner class UnauthorizedExceptions {

        @Test
        @DisplayName("→ 401 with message field")
        fun returns401() {
            mockMvc.get("/test/unauthorized") {
                accept(MediaType.APPLICATION_JSON)
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.message") { value("Not authorized") }
            }
        }
    }

    @Nested
    @DisplayName("AccessDeniedException")
    inner class AccessDeniedExceptions {

        @Test
        @DisplayName("→ 403 with 'Access denied!' message")
        fun returns403() {
            mockMvc.get("/test/access-denied") {
                accept(MediaType.APPLICATION_JSON)
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.message") { value("Access denied!") }
            }
        }
    }

    @Nested
    @DisplayName("Generic Exception")
    inner class GenericExceptions {

        @Test
        @DisplayName("→ 500 with generic message (no stack trace leaked)")
        fun returns500() {
            mockMvc.get("/test/generic") {
                accept(MediaType.APPLICATION_JSON)
            }.andExpect {
                status { isInternalServerError() }
                jsonPath("$.message") { value("An unexpected error occurred") }
            }
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException")
    inner class ValidationExceptions {

        @Test
        @DisplayName("blank field → 400 with errors array containing field name")
        fun blankField_returns400WithFieldErrors() {
            mockMvc.post("/test/validate") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":""}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.errors") { exists() }
                jsonPath("$.errors[0].field") { value("name") }
            }
        }
    }
}
