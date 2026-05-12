package com.example.userservice.config

import com.example.common.exception.BusinessException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import feign.Response
import feign.codec.ErrorDecoder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.charset.StandardCharsets

@Configuration
class FeignErrorDecoderConfig {

    @Bean
    fun feignErrorDecoder(objectMapper: ObjectMapper): ErrorDecoder {
        return CustomErrorDecoder(objectMapper)
    }
}

class CustomErrorDecoder(private val objectMapper: ObjectMapper) : ErrorDecoder {

    private val logger = LoggerFactory.getLogger(CustomErrorDecoder::class.java)
    private val defaultDecoder = ErrorDecoder.Default()

    override fun decode(methodKey: String, response: Response): Exception {
        try {
            // Đọc body response
            val responseBody = getResponseBody(response)

            if (responseBody.isNotBlank()) {
                try {
                    // Parse response body to get error message
                    val jsonNode = objectMapper.readTree(responseBody)

                    // Kiểm tra các định dạng response có thể có
                    val errorMessage = extractErrorMessage(jsonNode)

                    if (errorMessage.isNotBlank()) {
                        logger.warn("Feign client error from $methodKey: $errorMessage")
                        return BusinessException(errorMessage)
                    }
                } catch (e: Exception) {
                    logger.warn("Error parsing error response: ${e.message}")
                }
            }

            // Các lỗi HTTP phổ biến
            return when (response.status()) {
                400 -> BusinessException("Bad request: Invalid input data")
                401 -> BusinessException("Unauthorized: Authentication required")
                403 -> BusinessException("Forbidden: You don't have permission to access this resource")
                404 -> BusinessException("Resource not found")
                408 -> BusinessException("Request timeout: Service is taking too long to respond")
                429 -> BusinessException("Too many requests: Please try again later")
                500 -> BusinessException("Internal server error: Service is currently unavailable")
                502 -> BusinessException("Bad gateway: Service is temporarily unavailable")
                503 -> BusinessException("Service unavailable: Please try again later")
                504 -> BusinessException("Gateway timeout: Service is taking too long to respond")
                else -> defaultDecoder.decode(methodKey, response)
            }
        } catch (e: Exception) {
            logger.error("Error in Feign error decoder: ${e.message}", e)
            return defaultDecoder.decode(methodKey, response)
        }
    }

    private fun getResponseBody(response: Response): String {
        return try {
            if (response.body() != null) {
                val bodyBytes = response.body().asInputStream().readBytes()
                String(bodyBytes, StandardCharsets.UTF_8)
            } else {
                ""
            }
        } catch (e: Exception) {
            logger.warn("Could not read response body: ${e.message}")
            ""
        }
    }

    private fun extractErrorMessage(jsonNode: JsonNode): String {
        // Kiểm tra định dạng response của api-service
        return when {
            jsonNode.has("message") -> jsonNode.get("message").asText("")
            jsonNode.has("error") && jsonNode.get("error").has("message") ->
                jsonNode.get("error").get("message").asText("")
            jsonNode.has("status") && jsonNode.has("error") && jsonNode.has("message") ->
                jsonNode.get("message").asText("")
            jsonNode.has("errors") && jsonNode.get("errors").isObject ->
                extractErrorsFromMap(jsonNode.get("errors"))
            else -> ""
        }
    }

    private fun extractErrorsFromMap(errorsNode: JsonNode): String {
        val errorMessages = mutableListOf<String>()
        errorsNode.fields().forEach { (field, value) ->
            val error = if (value.isTextual) value.asText() else field
            errorMessages.add(error)
        }
        return errorMessages.joinToString(", ")
    }
}
