package com.example.learningservice.controller.admin

import com.example.learningservice.dto.StudyStatisticsDto
import com.example.learningservice.service.FlashcardAdminService
import com.example.learningservice.service.FlashcardStatisticsService
import com.example.learningservice.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

@RestController
@RequestMapping("/api/v1/learning/admin/statistics")
@Tag(name = "Admin Statistics", description = "API endpoints for admin statistics")
@PreAuthorize("hasRole('ADMIN')")
class AdminStatisticsController(
    private val userService: UserService,
    private val flashcardStatisticsService: FlashcardStatisticsService,
    private val flashcardAdminService: FlashcardAdminService,
) {
    private val logger = LoggerFactory.getLogger(AdminStatisticsController::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    @GetMapping("/users", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all user statistics",
        description = "Retrieves statistics for all users with pagination and sorting",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getAllUserStatistics(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "lastActive") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<Any> {
        logger.info("Fetching statistics for all users (page: $page, size: $size, sortBy: $sortBy, sortDir: $sortDir, search: $search)")

        val direction = if (sortDir.equals("desc", ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
        val usersPage = userService.getAllUsers(pageable, search)

        val userStatsList = usersPage.content.map { user ->
            val uid = requireNotNull(user.userId) { "User ID missing" }
            val flashcardStats = flashcardStatisticsService.getUserFlashcardStatistics(uid)
            val lastReview = flashcardStatisticsService.getLastReviewDate(uid)
            val lastActive = lastReview?.format(dateTimeFormatter)
                ?: user.updatedAt.atZone(ZoneOffset.UTC).format(dateTimeFormatter)

            mapOf(
                "userId" to user.userId,
                "userName" to user.fullName,
                "email" to user.email,
                "summary" to flashcardStats.summary,
                "cardsByState" to flashcardStats.cardsByState,
                "lastActive" to lastActive,
                "progress" to calculateUserProgress(flashcardStats),
            )
        }

        return ResponseEntity.ok(
            mapOf(
                "data" to mapOf(
                    "users" to userStatsList,
                    "totalItems" to usersPage.totalElements,
                    "totalPages" to usersPage.totalPages,
                    "currentPage" to usersPage.number,
                ),
            ),
        )
    }

    @GetMapping("/users/{userId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get detailed statistics for a specific user",
        description = "Retrieves detailed statistics for a specific user",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getUserStatisticsById(@PathVariable userId: UUID): ResponseEntity<Any> {
        logger.info("Fetching detailed statistics for user: $userId")

        val user = userService.getUserById(userId)
        val flashcardStats = flashcardStatisticsService.getUserFlashcardStatistics(userId)
        val lastReview = flashcardStatisticsService.getLastReviewDate(userId)
        val reviewHistory = flashcardStatisticsService.getUserReviewHistory(userId, 30)
        val lastActive = lastReview?.format(dateTimeFormatter)
            ?: user.updatedAt.atZone(ZoneOffset.UTC).format(dateTimeFormatter)

        val userStats = mapOf(
            "userId" to user.userId,
            "userName" to user.fullName,
            "email" to user.email,
            "profileInfo" to mapOf(
                "currentLevel" to user.currentLevel,
                "jlptGoal" to user.jlptGoal,
                "createdAt" to user.createdAt.atZone(ZoneOffset.UTC).format(dateTimeFormatter),
                "lastLogin" to user.lastLogin?.format(dateTimeFormatter),
                "isActive" to user.isActive,
                "isEmailVerified" to user.isEmailVerified,
                "streakCount" to user.streakCount,
                "points" to user.points,
                "reminderEnabled" to user.reminderEnabled,
                "reminderTime" to user.reminderTime?.toString(),
                "minCardThreshold" to user.minCardThreshold,
            ),
            "summary" to flashcardStats.summary,
            "cardsByState" to flashcardStats.cardsByState,
            "cardsByJlptLevel" to flashcardStats.cardsByJlptLevel,
            "dailyReviews" to flashcardStats.dailyReviews,
            "retentionRateByDay" to flashcardStats.retentionRateByDay,
            "memoryStrengthDistribution" to flashcardStats.memoryStrengthDistribution,
            "cardsDueByDay" to flashcardStats.cardsDueByDay,
            "lastActive" to lastActive,
            "progress" to calculateUserProgress(flashcardStats),
            "reviewHistory" to reviewHistory,
        )

        return ResponseEntity.ok(mapOf("data" to userStats))
    }

    @GetMapping("/overview", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get statistics overview for admin dashboard",
        description = "Retrieves general statistics overview for admin dashboard",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @Suppress("LongMethod")
    fun getStatisticsOverview(): ResponseEntity<Any> {
        logger.info("Fetching statistics overview for admin dashboard")

        val thirtyDaysAgo = java.time.LocalDateTime.now().minusDays(30)

        val totalUsers = userService.getUserCount()
        val activeUsers = userService.getActiveUserCount(thirtyDaysAgo)
        val totalFlashcards = flashcardAdminService.getTotalFlashcardsCount()
        val averageCardsPerUser = if (totalUsers > 0) totalFlashcards / totalUsers else 0
        val averageRetentionRate = flashcardAdminService.getAverageRetentionRate()
        val usersByLevel = userService.getUserCountByCurrentLevel()
        val usersByJlptGoal = userService.getUserCountByJlptGoal()

        val topPerformingUsers = userService.getTopPerformingUsers(5)
            .map { user ->
                val uid = requireNotNull(user.userId) { "User ID missing" }
                val flashcardStats = flashcardStatisticsService.getUserFlashcardStatistics(uid)
                val lastReview = flashcardStatisticsService.getLastReviewDate(uid)
                val lastActive = lastReview?.format(dateTimeFormatter)
                    ?: user.updatedAt.atZone(ZoneOffset.UTC).format(dateTimeFormatter)

                mapOf(
                    "userId" to user.userId,
                    "userName" to user.fullName,
                    "email" to user.email,
                    "summary" to flashcardStats.summary,
                    "lastActive" to lastActive,
                    "progress" to calculateUserProgress(flashcardStats),
                )
            }

        val mostActiveUsers = userService.getMostActiveUsers(5)
            .map { user ->
                val uid = requireNotNull(user.userId) { "User ID missing" }
                val flashcardStats = flashcardStatisticsService.getUserFlashcardStatistics(uid)
                val lastReview = flashcardStatisticsService.getLastReviewDate(uid)
                val lastActive = lastReview?.format(dateTimeFormatter)
                    ?: user.updatedAt.atZone(ZoneOffset.UTC).format(dateTimeFormatter)

                mapOf(
                    "userId" to user.userId,
                    "userName" to user.fullName,
                    "email" to user.email,
                    "summary" to flashcardStats.summary,
                    "lastActive" to lastActive,
                    "progress" to calculateUserProgress(flashcardStats),
                )
            }

        val overview = mapOf(
            "totalUsers" to totalUsers,
            "activeUsers" to activeUsers,
            "totalFlashcards" to totalFlashcards,
            "averageCardsPerUser" to averageCardsPerUser,
            "averageRetentionRate" to averageRetentionRate,
            "usersByLevel" to usersByLevel,
            "usersByJlptGoal" to usersByJlptGoal,
            "topPerformingUsers" to topPerformingUsers,
            "mostActiveUsers" to mostActiveUsers,
        )

        return ResponseEntity.ok(mapOf("data" to overview))
    }

    private fun calculateUserProgress(stats: StudyStatisticsDto): Int {
        val totalCards = stats.summary.totalCards
        if (totalCards == 0) return 0
        val reviewCards = stats.cardsByState["review"] ?: 0
        val graduatedCards = stats.cardsByState["graduated"] ?: 0
        return ((reviewCards + graduatedCards) * 100 / totalCards)
    }
}
