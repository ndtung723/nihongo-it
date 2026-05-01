package com.example.learningservice.controller.admin

import com.example.common.dto.ResponseDto
import com.example.common.dto.ResponseType
import com.example.common.security.PreAuthFilter
import com.example.learningservice.service.FlashcardService
import com.example.learningservice.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.get

@RestController
@RequestMapping("/api/v1/learning/admin/statistics")
@Tag(name = "Admin Statistics", description = "API endpoints for admin statistics")
@PreAuthFilter(hasRole = "admin")
class AdminStatisticsController(
    private val userService: UserService,
    private val flashcardService: FlashcardService
) {
    private val logger = LoggerFactory.getLogger(AdminStatisticsController::class.java)
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    @GetMapping("/users", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all user statistics",
        description = "Retrieves statistics for all users with pagination and sorting",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getAllUserStatistics(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "lastActive") sortBy: String,
        @RequestParam(defaultValue = "desc") sortDir: String,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<Any> {
        logger.info("Fetching statistics for all users (page: $page, size: $size, sortBy: $sortBy, sortDir: $sortDir, search: $search)")
        
        try {
            val direction = if (sortDir.equals("desc", ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC
            val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
            
            val usersPage = userService.getAllUsers(pageable, search)
            
            // Map users to their statistics
            val userStatsList = usersPage.content.map { user ->
                val flashcardStats = flashcardService.getUserFlashcardStatistics(user.userId!!)
                val lastReview = flashcardService.getLastReviewDate(user.userId!!)
                
                mapOf(
                    "userId" to user.userId,
                    "userName" to user.fullName,
                    "email" to user.email,
                    "summary" to flashcardStats["summary"],
                    "cardsByState" to flashcardStats["cardsByState"],
                    "lastActive" to (lastReview?.format(dateTimeFormatter) ?: user.updatedAt?.format(dateTimeFormatter)),
                    "progress" to calculateUserProgress(flashcardStats)
                )
            }
            
            val response = mapOf(
                "result" to ResponseDto(ResponseType.OK, "User statistics retrieved successfully"),
                "data" to mapOf(
                    "users" to userStatsList,
                    "totalItems" to usersPage.totalElements,
                    "totalPages" to usersPage.totalPages,
                    "currentPage" to usersPage.number
                )
            )
            
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error retrieving user statistics", e)
            
            val errorResponse = mapOf(
                "result" to ResponseDto(ResponseType.NG, "Error retrieving user statistics: ${e.message}"),
                "data" to mapOf(
                    "users" to emptyList<Any>(),
                    "totalItems" to 0,
                    "totalPages" to 0,
                    "currentPage" to 0
                )
            )
            
            return ResponseEntity.ok(errorResponse)
        }
    }
    
    @GetMapping("/users/{userId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get detailed statistics for a specific user",
        description = "Retrieves detailed statistics for a specific user",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getUserStatisticsById(@PathVariable userId: UUID): ResponseEntity<Any> {
        logger.info("Fetching detailed statistics for user: $userId")
        
        try {
            val user = userService.getUserById(userId)
            val flashcardStats = flashcardService.getUserFlashcardStatistics(userId)
            val lastReview = flashcardService.getLastReviewDate(userId)
            
            // Get detailed review history for the user
            val reviewHistory = flashcardService.getUserReviewHistory(userId, 30) // Last 30 days
            
            val userStats = mapOf(
                "userId" to user.userId,
                "userName" to user.fullName,
                "email" to user.email,
                "profileInfo" to mapOf(
                    "currentLevel" to user.currentLevel,
                    "jlptGoal" to user.jlptGoal,
                    "createdAt" to user.createdAt?.format(dateTimeFormatter),
                    "lastLogin" to user.lastLogin?.format(dateTimeFormatter)
                ),
                "summary" to flashcardStats["summary"],
                "cardsByState" to flashcardStats["cardsByState"],
                "cardsByJlptLevel" to flashcardStats["cardsByJlptLevel"],
                "dailyReviews" to flashcardStats["dailyReviews"],
                "retentionRateByDay" to flashcardStats["retentionRateByDay"],
                "memoryStrengthDistribution" to flashcardStats["memoryStrengthDistribution"],
                "cardsDueByDay" to flashcardStats["cardsDueByDay"],
                "lastActive" to (lastReview?.format(dateTimeFormatter) ?: user.updatedAt?.format(dateTimeFormatter)),
                "progress" to calculateUserProgress(flashcardStats),
                "reviewHistory" to reviewHistory
            )
            
            val response = mapOf(
                "result" to ResponseDto(ResponseType.OK, "User statistics retrieved successfully"),
                "data" to userStats
            )
            
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error retrieving user statistics for user $userId", e)
            
            val errorResponse = mapOf(
                "result" to ResponseDto(ResponseType.NG, "Error retrieving user statistics: ${e.message}"),
                "data" to emptyMap<String, Any>()
            )
            
            return ResponseEntity.ok(errorResponse)
        }
    }
    
    @GetMapping("/overview", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get statistics overview for admin dashboard",
        description = "Retrieves general statistics overview for admin dashboard",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getStatisticsOverview(): ResponseEntity<Any> {
        logger.info("Fetching statistics overview for admin dashboard")
        
        try {
            val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
            
            // Get general statistics
            val totalUsers = userService.getUserCount()
            val activeUsers = userService.getActiveUserCount(thirtyDaysAgo)
            val totalFlashcards = flashcardService.getTotalFlashcardsCount()
            val averageCardsPerUser = if (totalUsers > 0) totalFlashcards / totalUsers else 0
            
            // Get retention rate across all users
            val averageRetentionRate = flashcardService.getAverageRetentionRate()
            
            // Get users by level and JLPT goal
            val usersByLevel = userService.getUserCountByCurrentLevel()
            val usersByJlptGoal = userService.getUserCountByJlptGoal()
            
            // Get top performing users (by retention rate)
            val topPerformingUsers = userService.getTopPerformingUsers(5)
                .map { user ->
                    val flashcardStats = flashcardService.getUserFlashcardStatistics(user.userId!!)
                    val lastReview = flashcardService.getLastReviewDate(user.userId!!)
                    
                    mapOf(
                        "userId" to user.userId,
                        "userName" to user.fullName,
                        "email" to user.email,
                        "summary" to flashcardStats["summary"],
                        "lastActive" to (lastReview?.format(dateTimeFormatter) ?: user.updatedAt?.format(dateTimeFormatter)),
                        "progress" to calculateUserProgress(flashcardStats)
                    )
                }
            
            // Get most active users (by number of reviews)
            val mostActiveUsers = userService.getMostActiveUsers(5)
                .map { user ->
                    val flashcardStats = flashcardService.getUserFlashcardStatistics(user.userId!!)
                    val lastReview = flashcardService.getLastReviewDate(user.userId!!)
                    
                    mapOf(
                        "userId" to user.userId,
                        "userName" to user.fullName,
                        "email" to user.email,
                        "summary" to flashcardStats["summary"],
                        "lastActive" to (lastReview?.format(dateTimeFormatter) ?: user.updatedAt?.format(dateTimeFormatter)),
                        "progress" to calculateUserProgress(flashcardStats)
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
                "mostActiveUsers" to mostActiveUsers
            )
            
            val response = mapOf(
                "result" to ResponseDto(ResponseType.OK, "Statistics overview retrieved successfully"),
                "data" to overview
            )
            
            return ResponseEntity.ok(response)
        } catch (e: Exception) {
            logger.error("Error retrieving statistics overview", e)
            
            val errorResponse = mapOf(
                "result" to ResponseDto(ResponseType.NG, "Error retrieving statistics overview: ${e.message}"),
                "data" to emptyMap<String, Any>()
            )
            
            return ResponseEntity.ok(errorResponse)
        }
    }
    
    // Helper method to calculate user progress as a percentage
    private fun calculateUserProgress(flashcardStats: Map<String, Any>): Int {
        val summary = flashcardStats["summary"] as? Map<*, *> ?: return 0
        val cardsByState = flashcardStats["cardsByState"] as? Map<*, *> ?: return 0
        
        val totalCards = summary["totalCards"] as? Int ?: 0
        
        if (totalCards == 0) {
            return 0
        }
        
        // Calculate progress based on card states (review and graduated cards are considered "learned")
        val reviewCards = (cardsByState["review"] as? Int) ?: 0
        val graduatedCards = (cardsByState["graduated"] as? Int) ?: 0
        
        return ((reviewCards + graduatedCards) * 100 / totalCards)
    }
} 