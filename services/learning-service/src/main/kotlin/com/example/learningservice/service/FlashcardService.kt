package com.example.learningservice.service

import com.example.learningservice.dto.UpdateFlashcardRequestDto
import com.example.learningservice.dto.CreateFlashcardRequestDto
import com.example.learningservice.dto.CreateFlashcardResponseDto
import com.example.learningservice.dto.DeleteFlashcardResponseDto
import com.example.learningservice.dto.FlashcardDTO
import com.example.learningservice.dto.GetDueCardsResponseDto
import com.example.learningservice.dto.GetFlashcardResponseDto
import com.example.learningservice.dto.GetFlashcardsResponseDto
import com.example.learningservice.dto.GetStatisticsResponseDto
import com.example.learningservice.dto.ResponseDto
import com.example.learningservice.dto.ResponseType
import com.example.learningservice.dto.ReviewFlashcardResponseDto
import com.example.learningservice.dto.UpdateFlashcardResponseDto
import com.example.learningservice.entity.FlashcardEntity
import com.example.learningservice.entity.ReviewLogEntity
import com.example.learningservice.entity.VocabularyEntity
import com.example.learningservice.exception.BusinessException
import com.example.learningservice.repository.FlashcardRepository
import com.example.learningservice.repository.ReviewLogRepository
import com.example.learningservice.repository.UserRepository
import com.example.learningservice.repository.VocabularyRepository
import com.example.learningservice.util.UserAuthUtil
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.AccessDeniedException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.time.format.DateTimeFormatter

@Service
class FlashcardService(
    private val flashcardRepository: FlashcardRepository,
    private val reviewLogRepository: ReviewLogRepository,
    private val userRepository: UserRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val fsrsService: FSRSService,
    private val userAuthUtil: UserAuthUtil,
    private val userService: UserService
) {
    private val logger = LoggerFactory.getLogger(FlashcardService::class.java)
    
    // Get due flashcards
    fun getDueCards(): GetDueCardsResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Getting due cards for user: $userId")
        val dueCards = flashcardRepository.findDueCards(userId!!, LocalDateTime.now())
        
        val result = ResponseDto(
            status = ResponseType.OK,
            message = "Due cards retrieved successfully"
        )
        
        return GetDueCardsResponseDto(
            result = result,
            data = dueCards.map { toDTO(it) }
        )
    }
    
    // Get all flashcards
    fun getAllFlashcards(): GetFlashcardsResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Getting all flashcards for user: $userId")
        val allCards = flashcardRepository.findByUser_UserId(userId!!)
        
        val result = ResponseDto(
            status = ResponseType.OK,
            message = "All flashcards retrieved successfully"
        )
        
        return GetFlashcardsResponseDto(
            result = result,
            data = allCards.map { toDTO(it) }
        )
    }
    
    // Get a specific flashcard
    fun getFlashcardById(flashcardId: UUID): GetFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        val flashcard = flashcardRepository.findById(flashcardId)
            .orElseThrow { EntityNotFoundException("Flashcard not found with id: $flashcardId") }
            
        if (flashcard.user.userId != userId) {
            throw AccessDeniedException("User does not have access to this flashcard")
        }
        
        val result = ResponseDto(
            status = ResponseType.OK,
            message = "Flashcard retrieved successfully"
        )
        
        return GetFlashcardResponseDto(
            result = result,
            data = toDTO(flashcard)
        )
    }
    
    // Process flashcard review
    @Transactional
    fun processReview(flashcardId: UUID, rating: Int): ReviewFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Processing review for flashcard: $flashcardId with rating: $rating")
        
        val flashcard = flashcardRepository.findById(flashcardId)
            .orElseThrow { EntityNotFoundException("Flashcard not found with id: $flashcardId") }
            
        if (flashcard.user.userId != userId) {
            throw AccessDeniedException("User does not have access to this flashcard")
        }
        
        if (rating < 1 || rating > 4) {
            throw BusinessException("Rating must be between 1 and 4")
        }
        
        // Calculate elapsed days since due date
        val now = LocalDateTime.now()
        val elapsedDays = if (flashcard.due.isBefore(now)) {
            ChronoUnit.DAYS.between(flashcard.due, now).toDouble().coerceAtLeast(0.0)
        } else {
            0.0
        }
        
        // Get current state before processing
        val currentState = flashcard.state
        
        // Process review with FSRS
        val updatedFlashcard = fsrsService.processReview(flashcard, rating)
        
        // Create review log based on Go-FSRS structure
        val reviewLog = ReviewLogEntity(
            flashcard = updatedFlashcard,
            userId = userId!!,
            rating = rating,
            scheduledDays = updatedFlashcard.scheduledDays,
            elapsedDays = elapsedDays,
            reviewTimestamp = now,
            state = currentState // State before review
        )
        
        // Save review log
        reviewLogRepository.save(reviewLog)

        logger.info("Calling updateUserStreak for user $userId after successful review")
        userService.updateUserStreak(userId)
        
        val result = ResponseDto(
            status = ResponseType.OK,
            message = "Flashcard reviewed successfully"
        )
        
        return ReviewFlashcardResponseDto(
            result = result,
            data = toDTO(updatedFlashcard)
        )
    }
    
    // Create new flashcard
    @Transactional
    fun createFlashcard(request: CreateFlashcardRequestDto): CreateFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Creating new flashcard for user: $userId")
        
        val user = userRepository.findById(userId!!)
            .orElseThrow { EntityNotFoundException("User not found with id: $userId") }
        
        // Check if there's a vocabulary reference
        val vocabulary = if (request.vocabularyId != null) {
            vocabularyRepository.findById(request.vocabularyId)
                .orElse(null)
        } else {
            null
        }
        
        val flashcard = FlashcardEntity(
            user = user,
            vocabulary = vocabulary,
            frontText = request.frontText,
            backText = request.backText,
        )
        
        // Initialize with FSRS default values
        val savedFlashcard = fsrsService.initializeFlashcard(flashcard)

        val result = ResponseDto(
            status = ResponseType.OK,
            message = "Flashcard created successfully"
        )
        
        return CreateFlashcardResponseDto(
            result = result,
            data = toDTO(savedFlashcard)
        )
    }
    
    // Create flashcard from vocabulary //use this method
    @Transactional
    fun createFlashcardFromVocabulary(vocabId: UUID): CreateFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Creating flashcard from vocabulary: $vocabId for user: $userId")

        val user = userRepository.findById(userId!!)
            .orElseThrow { EntityNotFoundException("User not found with id: $userId") }

        val vocabulary = vocabularyRepository.findById(vocabId)
            .orElseThrow { EntityNotFoundException("Vocabulary item not found with id: $vocabId") }

        // Check if flashcard already exists for this vocabulary and user
        val existingFlashcard = flashcardRepository.findByUser_UserIdAndVocabulary_VocabId(userId, vocabId)
        if (existingFlashcard.isNotEmpty()) {
            throw BusinessException("Flashcard for this vocabulary item already exists")
        }

        // Create front and back text based on vocabulary
        val frontText = buildFrontText(vocabulary)
        val backText = buildBackText(vocabulary)

        val flashcard = FlashcardEntity(
            user = user,
            vocabulary = vocabulary,
            frontText = frontText,
            backText = backText,
        )

        // Initialize with FSRS default values
        val savedFlashcard = fsrsService.initializeFlashcard(flashcard)

        val result = ResponseDto(
            status = ResponseType.OK,
            message = "Flashcard created from vocabulary successfully"
        )

        return CreateFlashcardResponseDto(
            result = result,
            data = toDTO(savedFlashcard)
        )
    }
    
    // Helper to build front text from vocabulary
    private fun buildFrontText(vocabulary: VocabularyEntity): String {
        val sb = StringBuilder()
        
        // Add kanji if available
        if (!vocabulary.term.isNullOrBlank()) {
            sb.append(vocabulary.term)
        }
        
        return sb.toString()
    }
    
    // Helper to build back text from vocabulary
    private fun buildBackText(vocabulary: VocabularyEntity): String {
        val sb = StringBuilder()
        
        // Add meaning
        sb.append(vocabulary.meaning)
        sb.append("\n\n")
        
        // Add reading if kanji was used on front
        if (!vocabulary.term.isNullOrBlank()) {
            sb.append("Reading: ")
            sb.append(vocabulary.term)
            sb.append("\n\n")
        }
        
        // Add example if available
        if (!vocabulary.example.isNullOrBlank()) {
            sb.append("Example: ")
            sb.append(vocabulary.example)
            if (!vocabulary.exampleMeaning.isNullOrBlank()) {
                sb.append("\n")
                sb.append(vocabulary.exampleMeaning)
            }
        }
        
        return sb.toString()
    }
    
    // Update flashcard
    @Transactional
    fun updateFlashcard(flashcardId: UUID, request: UpdateFlashcardRequestDto): UpdateFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Updating flashcard: $flashcardId")
        
        val existingFlashcard = flashcardRepository.findById(flashcardId)
            .orElseThrow { EntityNotFoundException("Flashcard not found with id: $flashcardId") }
            
        if (existingFlashcard.user.userId != userId) {
            throw AccessDeniedException("User does not have access to this flashcard")
        }
        
        existingFlashcard.frontText = request.frontText!!
        existingFlashcard.backText = request.backText!!

        val savedFlashcard = flashcardRepository.save(existingFlashcard)
        
        val result = ResponseDto(
            status = ResponseType.OK,
            message = "Flashcard updated successfully"
        )
        
        return UpdateFlashcardResponseDto(
            result = result,
            data = toDTO(savedFlashcard)
        )
    }
    
    // Delete flashcard
    @Transactional
    fun deleteFlashcard(flashcardId: UUID): DeleteFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Deleting flashcard: $flashcardId")

        val flashcard = flashcardRepository.findById(flashcardId)
            .orElseThrow { EntityNotFoundException("Flashcard not found with id: $flashcardId") }
            
        if (flashcard.user.userId != userId) {
            throw AccessDeniedException("User does not have access to this flashcard")
        }
        
        flashcardRepository.delete(flashcard)
        
        val result = ResponseDto(
            status = ResponseType.OK,
            message = "Flashcard deleted successfully"
        )
        
        return DeleteFlashcardResponseDto(
            result = result
        )
    }
    
    // Get study statistics
    fun getStudyStatistics(): GetStatisticsResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Getting study statistics for user: $userId")
        
        val allUserCards = flashcardRepository.findByUser_UserId(userId!!)
        val totalCards = allUserCards.size
        val dueCardsNow = flashcardRepository.findDueCards(userId, LocalDateTime.now()).size
        
        // Calculate cards due by day for the next 7 days
        val today = LocalDateTime.now()
        val cardsDueByDay = mutableMapOf<String, Int>()
        
        for (i in 0..6) {
            val date = today.plusDays(i.toLong())
            val dateString = date.toLocalDate().toString()
            val dueCount = flashcardRepository.findDueCards(userId, date).size - 
                           if (i > 0) flashcardRepository.findDueCards(userId, date.minusDays(1)).size else 0
            cardsDueByDay[dateString] = dueCount
        }
        
        // Get review history 
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        val recentReviews = reviewLogRepository.findByUserIdAndReviewTimestampAfter(userId, thirtyDaysAgo)
        
        // Daily review counts
        val dailyReviews = recentReviews
            .groupBy { it.reviewTimestamp.toLocalDate().toString() }
            .mapValues { it.value.size }
        
        // Retention rate by day
        val retentionRateByDay = recentReviews
            .groupBy { it.reviewTimestamp.toLocalDate().toString() }
            .mapValues { entry ->
                val dayReviews = entry.value
                val correctCount = dayReviews.count { it.rating >= 3 }
                if (dayReviews.isNotEmpty()) {
                    (correctCount.toDouble() / dayReviews.size) * 100.0
                } else {
                    0.0
                }
            }
        
        // Overall retention rate
        val correctReviews = recentReviews.count { it.rating >= 3 }
        val overallRetentionRate = if (recentReviews.isNotEmpty()) {
            (correctReviews.toDouble() / recentReviews.size) * 100.0
        } else {
            0.0
        }
        
        // Memory strength distribution
        val memoryStrengthDistribution = allUserCards
            .groupBy { 
                when {
                    it.stability == 0.0 -> "new"
                    it.stability!! <= 1.0 -> "weak"
                    it.stability!! <= 10.0 -> "medium"
                    else -> "strong"
                }
            }
            .mapValues { it.value.size }
        
        // Get cards by state
        val cardsByState = allUserCards
            .groupBy { FSRSService.State.entries.find { state -> state.value == it.state }?.name?.lowercase() ?: "unknown" }
            .mapValues { it.value.size }
        
        // JLPT level statistics (if vocabulary is available)
        val cardsByJlptLevel = allUserCards
            .filter { it.vocabulary != null }
            .groupBy { it.vocabulary?.jlptLevel ?: "unknown" }
            .mapValues { it.value.size }
        
        // Streak calculation
        val reviewsByDate = recentReviews
            .groupBy { it.reviewTimestamp.toLocalDate() }
        
        val statistics = mapOf(
            "summary" to mapOf(
                "totalCards" to totalCards,
                "dueCardsNow" to dueCardsNow,
                "reviewsLast30Days" to recentReviews.size,
                "currentStreak" to userService.getUserById(userId).streakCount,
                "overallRetentionRate" to overallRetentionRate
            ),
            "cardsDueByDay" to cardsDueByDay,
            "dailyReviews" to dailyReviews,
            "retentionRateByDay" to retentionRateByDay,
            "memoryStrengthDistribution" to memoryStrengthDistribution,
            "cardsByState" to cardsByState,
            "cardsByJlptLevel" to cardsByJlptLevel,
            "reviewTrend" to calculateReviewTrend(recentReviews),
            "averageRating" to if (recentReviews.isNotEmpty()) recentReviews.map { it.rating }.average() else 0.0
        )
        
        val result = ResponseDto(
            status = ResponseType.OK,
            message = "Study statistics retrieved successfully"
        )
        
        return GetStatisticsResponseDto(
            result = result,
            data = statistics
        )
    }
    
    // Helper method to calculate review trend
    private fun calculateReviewTrend(reviews: List<ReviewLogEntity>): Map<String, Any> {
        if (reviews.isEmpty()) {
            return mapOf("trend" to "neutral", "percentage" to 0.0)
        }
        
        val twoWeeksAgo = LocalDateTime.now().minusDays(14)
        val oneWeekAgo = LocalDateTime.now().minusDays(7)
        
        val previousWeekReviews = reviews.filter { 
            it.reviewTimestamp.isAfter(twoWeeksAgo) && it.reviewTimestamp.isBefore(oneWeekAgo) 
        }
        
        val currentWeekReviews = reviews.filter { 
            it.reviewTimestamp.isAfter(oneWeekAgo) 
        }
        
        val previousWeekCount = previousWeekReviews.size
        val currentWeekCount = currentWeekReviews.size
        
        val trend = when {
            previousWeekCount == 0 -> "up"
            currentWeekCount > previousWeekCount -> "up"
            currentWeekCount < previousWeekCount -> "down"
            else -> "neutral"
        }
        
        val percentage = if (previousWeekCount > 0) {
            ((currentWeekCount - previousWeekCount).toDouble() / previousWeekCount) * 100.0
        } else if (currentWeekCount > 0) {
            100.0
        } else {
            0.0
        }
        
        return mapOf(
            "trend" to trend,
            "percentage" to percentage
        )
    }
    
    // Get flashcards for a vocabulary item
    fun getFlashcardsByVocabulary(vocabId: UUID): GetFlashcardsResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Getting flashcards for vocabulary: $vocabId and user: $userId")
        
        val flashcards = flashcardRepository.findByVocabulary_VocabId(vocabId)
            .filter { it.user.userId == userId }
        
        val result = ResponseDto(
            status = ResponseType.OK,
            message = "Vocabulary flashcards retrieved successfully"
        )
        
        return GetFlashcardsResponseDto(
            result = result,
            data = flashcards.map { toDTO(it) }
        )
    }
    
    // Convert entity to DTO
    fun toDTO(flashcard: FlashcardEntity): FlashcardDTO {
        return FlashcardDTO(
            id = flashcard.flashcardId,
            frontText = flashcard.frontText,
            backText = flashcard.backText,
            vocabularyId = flashcard.vocabulary?.vocabId,
            due = flashcard.due,
            reps = flashcard.reps,
            lapses = flashcard.lapses,
            state = FSRSService.State.entries.find { it.value == flashcard.state }?.name?.lowercase() ?: "new",
            difficulty = flashcard.difficulty,
            stability = flashcard.stability,
            interval = flashcard.scheduledDays,
            createdAt = flashcard.createdAt,
            updatedAt = flashcard.updatedAt

        )
    }
    
    /**
     * Get count of flashcards created after a specific time
     */
    fun getFlashcardsCreatedCount(afterDate: LocalDateTime): Int {
        return flashcardRepository.countByCreatedAtAfter(afterDate).toInt()
    }
    
    /**
     * Get count of flashcards studied (reviewed) after a specific time
     */
    fun getFlashcardsStudiedCount(afterDate: LocalDateTime): Int {
        // Count distinct flashcards that have been reviewed
        return reviewLogRepository.countDistinctFlashcardIdByReviewTimestampAfter(afterDate).toInt()
    }
    
    /**
     * Get flashcard statistics for a specific user
     */
    fun getUserFlashcardStatistics(userId: UUID): Map<String, Any> {
        logger.info("Getting flashcard statistics for user: $userId")
        
        // Get user to access streak count
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found with id: $userId") }
        
        val allFlashcards = flashcardRepository.findByUser_UserId(userId)
        if (allFlashcards.isEmpty()) {
            // Return empty stats when user has no flashcards
            return mapOf(
                "summary" to mapOf(
                    "totalCards" to 0,
                    "dueCardsNow" to 0,
                    "reviewsLast30Days" to 0,
                    "overallRetentionRate" to 0
                ),
                "cardsByState" to mapOf<String, Int>(),
                "cardsByJlptLevel" to mapOf<String, Int>(),
                "dailyReviews" to mapOf<String, Int>(),
                "retentionRateByDay" to mapOf<String, Double>(),
                "memoryStrengthDistribution" to mapOf(
                    "weak" to 0,
                    "medium" to 0,
                    "strong" to 0
                ),
                "cardsDueByDay" to mapOf<String, Int>()
            )
        }
        
        val now = LocalDateTime.now()
        val thirtyDaysAgo = now.minusDays(30)
        
        // Calculate due cards
        val dueCardsNow = flashcardRepository.findDueCards(userId, now).size
        
        // Collect review logs
        val recentReviews = reviewLogRepository.findByUserIdAndReviewTimestampAfterOrderByReviewTimestampDesc(
            userId, thirtyDaysAgo
        )
        
        // Calculate retention rate
        val correctReviews = recentReviews.count { it.rating >= 3 }
        val overallRetentionRate = if (recentReviews.isNotEmpty()) {
            (correctReviews.toDouble() / recentReviews.size) * 100
        } else {
            0.0
        }
        
        // Group by state
        val cardsByState = allFlashcards.groupBy { 
            FSRSService.State.entries.find { s -> s.value == it.state }?.name?.lowercase() ?: "new"
        }.mapValues { it.value.size }
        
        // Group by JLPT level
        val cardsByJlptLevel = allFlashcards
            .mapNotNull { it.vocabulary?.jlptLevel }
            .groupBy { it }
            .mapValues { it.value.size }
        
        // Calculate due cards by day for next 30 days
        val cardsDueByDay = (0..29).associate { day ->
            val dueDate = now.plusDays(day.toLong()).toLocalDate()
            val dateStr = dueDate.toString()
            
            val count = allFlashcards.count { 
                it.due.toLocalDate() == dueDate 
            }
            
            dateStr to count
        }
        
        // Group reviews by date
        val dailyReviews = recentReviews
            .groupBy { it.reviewTimestamp.toLocalDate().toString() }
            .mapValues { it.value.size }
        
        // Calculate retention rate by day
        val retentionRateByDay = recentReviews
            .groupBy { it.reviewTimestamp.toLocalDate().toString() }
            .mapValues { entry ->
                val dayReviews = entry.value
                val dayCorrect = dayReviews.count { it.rating >= 3 }
                (dayCorrect.toDouble() / dayReviews.size) * 100
            }
        
        // Calculate memory strength distribution
        val memoryStrengthDistribution = mapOf(
            "weak" to allFlashcards.count { it.stability!! < 1.0 },
            "medium" to allFlashcards.count { it.stability!! >= 1.0 && it.stability!! < 10.0 },
            "strong" to allFlashcards.count { it.stability!! >= 10.0 }
        )
        
        return mapOf(
            "summary" to mapOf(
                "totalCards" to allFlashcards.size,
                "dueCardsNow" to dueCardsNow,
                "reviewsLast30Days" to recentReviews.size,
                "currentStreak" to user.streakCount,
                "overallRetentionRate" to overallRetentionRate
            ),
            "cardsByState" to cardsByState,
            "cardsByJlptLevel" to cardsByJlptLevel,
            "dailyReviews" to dailyReviews,
            "retentionRateByDay" to retentionRateByDay,
            "memoryStrengthDistribution" to memoryStrengthDistribution,
            "cardsDueByDay" to cardsDueByDay,
            "reviewTrend" to calculateReviewTrend(recentReviews),
            "averageRating" to if (recentReviews.isNotEmpty()) recentReviews.map { it.rating }.average() else 0.0,
        )
    }
    
    /**
     * Get the last review date for a user
     */
    fun getLastReviewDate(userId: UUID): LocalDateTime? {
        return reviewLogRepository.findFirstByUserIdOrderByReviewTimestampDesc(userId)
            ?.reviewTimestamp
    }
    
    /**
     * Get review history for a user
     */
    fun getUserReviewHistory(userId: UUID, days: Int): List<Map<String, Any>> {
        val startDate = LocalDateTime.now().minusDays(days.toLong())
        val reviews = reviewLogRepository.findByUserIdAndReviewTimestampAfterOrderByReviewTimestampDesc(
            userId, startDate
        )
        
        return reviews.map { review ->
            val map = mutableMapOf<String, Any>(
                ("reviewId" to review.reviewLogId) as Pair<String, Any>,
                ("flashcardId" to review.flashcard.flashcardId) as Pair<String, Any>,
                "rating" to review.rating,
                "elapsedDays" to review.elapsedDays,
                "scheduledDays" to review.scheduledDays,
                "state" to review.state,
                "timestamp" to review.reviewTimestamp.format(DateTimeFormatter.ISO_DATE_TIME)
            )
            
            // Handle nullable vocabulary separately
            review.flashcard.vocabulary?.let { vocab ->
                map["vocabulary"] = mapOf(
                    "vocabId" to vocab.vocabId,
                    "term" to vocab.term,
                    "meaning" to vocab.meaning,
                    "jlptLevel" to vocab.jlptLevel
                )
            } ?: run {
                map["vocabulary"] = emptyMap<String, Any>()
            }
            
            map
        }
    }
    
    /**
     * Get total count of all flashcards in the system
     */
    fun getTotalFlashcardsCount(): Int {
        return flashcardRepository.count().toInt()
    }
    
    /**
     * Get average retention rate across all users
     */
    fun getAverageRetentionRate(): Double {
        val thirtyDaysAgo = LocalDateTime.now().minusDays(30)
        val recentReviews = reviewLogRepository.findByReviewTimestampAfter(thirtyDaysAgo)
        
        if (recentReviews.isEmpty()) {
            return 0.0
        }
        
        val correctReviews = recentReviews.count { it.rating >= 3 }
        return (correctReviews.toDouble() / recentReviews.size) * 100
    }
}