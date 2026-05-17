package com.example.learningservice.service

import com.example.common.exception.BusinessException
import com.example.common.result.ServiceResult
import com.example.learningservice.dto.CreateFlashcardRequestDto
import com.example.learningservice.dto.CreateFlashcardResponseDto
import com.example.learningservice.dto.DeleteFlashcardResponseDto
import com.example.learningservice.dto.FlashcardDTO
import com.example.learningservice.dto.GetDueCardsResponseDto
import com.example.learningservice.dto.GetFlashcardResponseDto
import com.example.learningservice.dto.GetFlashcardsResponseDto
import com.example.learningservice.dto.PagedFlashcardsResponseDto
import com.example.learningservice.dto.ReviewFlashcardResponseDto
import com.example.learningservice.dto.UpdateFlashcardRequestDto
import com.example.learningservice.dto.UpdateFlashcardResponseDto
import com.example.learningservice.entity.FlashcardEntity
import com.example.learningservice.entity.ReviewLogEntity
import com.example.learningservice.entity.VocabularyEntity
import com.example.learningservice.repository.FlashcardRepository
import com.example.learningservice.repository.ReviewLogRepository
import com.example.learningservice.repository.UserRepository
import com.example.learningservice.repository.VocabularyRepository
import com.example.learningservice.util.UserAuthUtil
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.AccessDeniedException
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

@Service
@Suppress("TooManyFunctions")
class FlashcardCrudService(
    private val flashcardRepository: FlashcardRepository,
    private val reviewLogRepository: ReviewLogRepository,
    private val userRepository: UserRepository,
    private val vocabularyRepository: VocabularyRepository,
    private val fsrsService: FSRSService,
    private val userAuthUtil: UserAuthUtil,
    private val userService: UserService,
) {
    private val logger = LoggerFactory.getLogger(FlashcardCrudService::class.java)

    // Get due flashcards
    @Transactional(readOnly = true)
    fun getDueCards(): GetDueCardsResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.debug("Getting due cards for user: $userId")
        val dueCards = flashcardRepository.findDueCards(requireNotNull(userId) { "User not authenticated" }, LocalDateTime.now())

        return GetDueCardsResponseDto(
            data = dueCards.map { toDTO(it) },
        )
    }

    // Get all flashcards (unpaged, kept for backward compatibility)
    @Transactional(readOnly = true)
    fun getAllFlashcards(): GetFlashcardsResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.debug("Getting all flashcards for user: $userId")
        val allCards = flashcardRepository.findByUser_UserId(requireNotNull(userId) { "User not authenticated" })

        return GetFlashcardsResponseDto(
            data = allCards.map { toDTO(it) },
        )
    }

    // Get flashcards with pagination
    @Transactional(readOnly = true)
    fun getAllFlashcardsPaged(
        page: Int,
        size: Int,
    ): PagedFlashcardsResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.debug("Getting flashcards page=$page size=$size for user: $userId")
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val flashcardPage =
            flashcardRepository.findByUser_UserId(
                requireNotNull(userId) { "User not authenticated" },
                pageable,
            )

        return PagedFlashcardsResponseDto(
            data = flashcardPage.content.map { toDTO(it) },
            page = flashcardPage.number,
            size = flashcardPage.size,
            totalElements = flashcardPage.totalElements,
            totalPages = flashcardPage.totalPages,
        )
    }

    // Get a specific flashcard — returns ServiceResult to make error paths explicit
    @Transactional(readOnly = true)
    fun getFlashcardById(flashcardId: UUID): ServiceResult<GetFlashcardResponseDto> {
        val userId = userAuthUtil.getCurrentUserId()
        val flashcard =
            flashcardRepository.findById(flashcardId).orElse(null)
                ?: return ServiceResult.Failure("Flashcard not found with id: $flashcardId")

        if (flashcard.user.userId != userId) {
            return ServiceResult.Failure("User does not have access to this flashcard")
        }

        return ServiceResult.Success(GetFlashcardResponseDto(data = toDTO(flashcard)))
    }

    // Process flashcard review
    @Transactional
    @Suppress("MagicNumber")
    fun processReview(
        flashcardId: UUID,
        rating: Int,
    ): ReviewFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Processing review for flashcard: $flashcardId with rating: $rating")

        val flashcard =
            flashcardRepository
                .findById(flashcardId)
                .orElseThrow { EntityNotFoundException("Flashcard not found with id: $flashcardId") }

        if (flashcard.user.userId != userId) {
            throw AccessDeniedException("User does not have access to this flashcard")
        }

        if (rating < 1 || rating > 4) {
            throw BusinessException("Rating must be between 1 and 4")
        }

        // Calculate elapsed days since due date
        val now = LocalDateTime.now()
        val elapsedDays =
            if (flashcard.due.isBefore(now)) {
                ChronoUnit.DAYS
                    .between(flashcard.due, now)
                    .toDouble()
                    .coerceAtLeast(0.0)
            } else {
                0.0
            }

        // Get current state before processing
        val currentState = flashcard.state

        // Process review with FSRS
        val updatedFlashcard = fsrsService.processReview(flashcard, rating)

        // Create review log based on Go-FSRS structure
        val reviewLog =
            ReviewLogEntity(
                flashcard = updatedFlashcard,
                userId = requireNotNull(userId) { "User not authenticated" },
                rating = rating,
                scheduledDays = updatedFlashcard.scheduledDays,
                elapsedDays = elapsedDays,
                reviewTimestamp = now,
                state = currentState, // State before review
            )

        // Save review log
        reviewLogRepository.save(reviewLog)

        logger.info("Calling updateUserStreak for user $userId after successful review")
        userService.updateUserStreak(userId)

        return ReviewFlashcardResponseDto(
            data = toDTO(updatedFlashcard),
        )
    }

    // Create new flashcard
    @Transactional
    fun createFlashcard(request: CreateFlashcardRequestDto): CreateFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Creating new flashcard for user: $userId")

        val user =
            userRepository
                .findById(requireNotNull(userId) { "User not authenticated" })
                .orElseThrow { EntityNotFoundException("User not found with id: $userId") }

        // Check if there's a vocabulary reference
        val vocabulary =
            if (request.vocabularyId != null) {
                vocabularyRepository
                    .findById(request.vocabularyId)
                    .orElse(null)
            } else {
                null
            }

        val flashcard =
            FlashcardEntity(
                user = user,
                vocabulary = vocabulary,
                frontText = request.frontText,
                backText = request.backText,
            )

        // Initialize with FSRS default values
        val savedFlashcard = fsrsService.initializeFlashcard(flashcard)

        return CreateFlashcardResponseDto(
            data = toDTO(savedFlashcard),
        )
    }

    // Create flashcard from vocabulary //use this method
    @Transactional
    fun createFlashcardFromVocabulary(vocabId: UUID): CreateFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Creating flashcard from vocabulary: $vocabId for user: $userId")

        val user =
            userRepository
                .findById(requireNotNull(userId) { "User not authenticated" })
                .orElseThrow { EntityNotFoundException("User not found with id: $userId") }

        val vocabulary =
            vocabularyRepository
                .findById(vocabId)
                .orElseThrow { EntityNotFoundException("Vocabulary item not found with id: $vocabId") }

        // Check if flashcard already exists for this vocabulary and user
        val existingFlashcard = flashcardRepository.findByUser_UserIdAndVocabulary_VocabId(userId, vocabId)
        if (existingFlashcard.isNotEmpty()) {
            throw BusinessException("Flashcard for this vocabulary item already exists")
        }

        // Create front and back text based on vocabulary
        val frontText = buildFrontText(vocabulary)
        val backText = buildBackText(vocabulary)

        val flashcard =
            FlashcardEntity(
                user = user,
                vocabulary = vocabulary,
                frontText = frontText,
                backText = backText,
            )

        // Initialize with FSRS default values
        val savedFlashcard = fsrsService.initializeFlashcard(flashcard)

        return CreateFlashcardResponseDto(
            data = toDTO(savedFlashcard),
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
    fun updateFlashcard(
        flashcardId: UUID,
        request: UpdateFlashcardRequestDto,
    ): UpdateFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Updating flashcard: $flashcardId")

        val existingFlashcard =
            flashcardRepository
                .findById(flashcardId)
                .orElseThrow { EntityNotFoundException("Flashcard not found with id: $flashcardId") }

        if (existingFlashcard.user.userId != userId) {
            throw AccessDeniedException("User does not have access to this flashcard")
        }

        existingFlashcard.frontText = request.frontText
        existingFlashcard.backText = request.backText

        val savedFlashcard = flashcardRepository.save(existingFlashcard)

        return UpdateFlashcardResponseDto(
            data = toDTO(savedFlashcard),
        )
    }

    // Delete flashcard
    @Transactional
    fun deleteFlashcard(flashcardId: UUID): DeleteFlashcardResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.info("Deleting flashcard: $flashcardId")

        val flashcard =
            flashcardRepository
                .findById(flashcardId)
                .orElseThrow { EntityNotFoundException("Flashcard not found with id: $flashcardId") }

        if (flashcard.user.userId != userId) {
            throw AccessDeniedException("User does not have access to this flashcard")
        }

        flashcardRepository.delete(flashcard)

        return DeleteFlashcardResponseDto()
    }

    // Get flashcards for a vocabulary item
    @Transactional(readOnly = true)
    fun getFlashcardsByVocabulary(vocabId: UUID): GetFlashcardsResponseDto {
        val userId = userAuthUtil.getCurrentUserId()
        logger.debug("Getting flashcards for vocabulary: $vocabId and user: $userId")

        val flashcards =
            flashcardRepository.findByUser_UserIdAndVocabulary_VocabId(
                requireNotNull(userId) { "User not authenticated" },
                vocabId,
            )

        return GetFlashcardsResponseDto(
            data = flashcards.map { toDTO(it) },
        )
    }

    internal fun toDTO(flashcard: FlashcardEntity): FlashcardDTO = flashcard.toDto()

    /**
     * Get count of flashcards created after a specific time
     */
    fun getFlashcardsCreatedCount(afterDate: LocalDateTime): Int = flashcardRepository.countByCreatedAtAfter(afterDate).toInt()

    /**
     * Get count of flashcards studied (reviewed) after a specific time
     */
    fun getFlashcardsStudiedCount(afterDate: LocalDateTime): Int {
        // Count distinct flashcards that have been reviewed
        return reviewLogRepository.countDistinctFlashcardIdByReviewTimestampAfter(afterDate).toInt()
    }
}
