package com.example.learningservice.controller.admin

import com.example.learningservice.service.CategoryService
import com.example.learningservice.service.FlashcardCrudService
import com.example.learningservice.service.TopicService
import com.example.learningservice.service.UserService
import com.example.learningservice.service.VocabularyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/v1/learning/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "API endpoints for admin dashboard statistics")
@PreAuthorize("hasRole('ADMIN')")
class AdminDashboardController(
    private val userService: UserService,
    private val vocabularyService: VocabularyService,
    private val categoryService: CategoryService,
    private val topicService: TopicService,
    private val flashcardCrudService: FlashcardCrudService,
) {
    private val logger = LoggerFactory.getLogger(AdminDashboardController::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    @GetMapping("/stats", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get dashboard statistics",
        description = "Retrieves various statistics for the admin dashboard",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @Suppress("LongMethod")
    fun getDashboardStats(): ResponseEntity<Any> {
        logger.info("Fetching dashboard statistics")

        val now = LocalDateTime.now()
        val thirtyDaysAgo = now.minusDays(30)
        val sevenDaysAgo = now.minusDays(7)
        val startOfDay =
            now
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)

        val userCount = userService.getUserCount()
        val vocabularyCount = vocabularyService.getVocabularyCount()
        val categoryCount = categoryService.getCategoryCount()
        val topicCount = topicService.getTopicCount()
        val newUsers = userService.getNewUserCount(sevenDaysAgo.toInstant(ZoneOffset.UTC))
        val activeUsers = userService.getActiveUserCount(thirtyDaysAgo)
        val flashcardsCreatedToday = flashcardCrudService.getFlashcardsCreatedCount(startOfDay)
        val flashcardsStudiedToday = flashcardCrudService.getFlashcardsStudiedCount(startOfDay)
        val searchesToday = 0

        val recentActivities =
            userService.getRecentUserActivities(10).map { activity ->
                val timestamp = activity["timestamp"] as? LocalDateTime
                mapOf(
                    "user" to activity["user"]?.toString().orEmpty(),
                    "action" to activity["action"]?.toString().orEmpty(),
                    "timestamp" to timestamp?.format(dateTimeFormatter).orEmpty(),
                )
            }

        val stats =
            mapOf(
                "userCount" to userCount,
                "vocabularyCount" to vocabularyCount,
                "categoryCount" to categoryCount,
                "topicCount" to topicCount,
                "newUsers" to newUsers,
                "activeUsers" to activeUsers,
                "flashcardsCreatedToday" to flashcardsCreatedToday,
                "flashcardsStudiedToday" to flashcardsStudiedToday,
                "searchesToday" to searchesToday,
                "recentActivities" to recentActivities,
            )

        return ResponseEntity.ok(mapOf("data" to stats))
    }
}
