package com.example.learningservice.controller.admin

import com.example.common.dto.ResponseDto
import com.example.common.dto.ResponseType
import com.example.common.security.PreAuthFilter
import com.example.learningservice.service.CategoryService
import com.example.learningservice.service.FlashcardService
import com.example.learningservice.service.TopicService
import com.example.learningservice.service.UserService
import com.example.learningservice.service.VocabularyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/learning/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "API endpoints for admin dashboard statistics")
@PreAuthFilter(hasRole = "admin")
class AdminDashboardController(
    private val userService: UserService,
    private val vocabularyService: VocabularyService,
    private val categoryService: CategoryService,
    private val topicService: TopicService,
    private val flashcardService: FlashcardService
) {
    private val logger = LoggerFactory.getLogger(AdminDashboardController::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    @GetMapping("/stats", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get dashboard statistics",
        description = "Retrieves various statistics for the admin dashboard",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getDashboardStats(): ResponseEntity<Any> {
        logger.info("Fetching dashboard statistics")

        // Get current date and time
        val now = LocalDateTime.now()
        val thirtyDaysAgo = now.minusDays(30)
        val sevenDaysAgo = now.minusDays(7)
        val startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0)

        try {
            // Collect various statistics
            val userCount = userService.getUserCount()
            val vocabularyCount = vocabularyService.getVocabularyCount()
            val categoryCount = categoryService.getCategoryCount()
            val topicCount = topicService.getTopicCount()

            // New users in last 7 days
            val newUsers = userService.getNewUserCount(sevenDaysAgo)

            // Active users in last 30 days
            val activeUsers = userService.getActiveUserCount(thirtyDaysAgo)

            // Flashcard statistics for today
            val flashcardsCreatedToday = flashcardService.getFlashcardsCreatedCount(startOfDay)
            val flashcardsStudiedToday = flashcardService.getFlashcardsStudiedCount(startOfDay)

            // Search count for today (if this statistic is tracked)
            val searchesToday = 0 // Placeholder, implement if search logging is available

            // Recent activities (example implementation)
            val recentActivities = userService.getRecentUserActivities(10).map { activity ->
                val timestamp = activity["timestamp"] as? LocalDateTime
                mapOf(
                    "user" to (activity["user"]?.toString() ?: ""),
                    "action" to (activity["action"]?.toString() ?: ""),
                    "timestamp" to (timestamp?.format(dateTimeFormatter) ?: "")
                )
            }

            // Assemble the stats
            val stats = mapOf(
                "userCount" to userCount,
                "vocabularyCount" to vocabularyCount,
                "categoryCount" to categoryCount,
                "topicCount" to topicCount,
                "newUsers" to newUsers,
                "activeUsers" to activeUsers,
                "flashcardsCreatedToday" to flashcardsCreatedToday,
                "flashcardsStudiedToday" to flashcardsStudiedToday,
                "searchesToday" to searchesToday,
                "recentActivities" to recentActivities
            )

            // Return with success response
            val response = mapOf(
                "result" to ResponseDto(ResponseType.OK, "Dashboard statistics retrieved successfully"),
                "data" to stats
            )

            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error retrieving dashboard statistics", e)

            // Create empty stats to maintain consistency with success response
            val emptyStats = mapOf(
                "userCount" to 0,
                "vocabularyCount" to 0,
                "categoryCount" to 0,
                "topicCount" to 0,
                "newUsers" to 0,
                "activeUsers" to 0,
                "flashcardsCreatedToday" to 0,
                "flashcardsStudiedToday" to 0,
                "searchesToday" to 0,
                "recentActivities" to emptyList<Map<String, Any>>()
            )

            // Return with error response but maintain the same structure
            val errorResponse = mapOf(
                "result" to ResponseDto(ResponseType.NG, "Error retrieving dashboard statistics: ${e.message}"),
                "data" to emptyStats
            )

            return ResponseEntity.ok(errorResponse)
        }
    }
} 