package com.example.learningservice.service

import com.example.learningservice.entity.FlashcardEntity
import com.example.learningservice.entity.RoleEntity
import com.example.learningservice.entity.UserEntity
import com.example.learningservice.repository.FlashcardRepository
import com.example.learningservice.repository.ReviewLogRepository
import com.example.learningservice.repository.UserRepository
import com.example.learningservice.repository.VocabularyRepository
import com.example.learningservice.util.UserAuthUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.Optional
import kotlin.test.assertEquals

class FlashcardReviewExistingCardTest {
    // Mock repositories and services
    private lateinit var flashcardRepository: FlashcardRepository
    private lateinit var reviewLogRepository: ReviewLogRepository
    private lateinit var userRepository: UserRepository
    private lateinit var vocabularyRepository: VocabularyRepository
    private lateinit var fsrsService: FSRSService
    private lateinit var userAuthUtil: UserAuthUtil
    private lateinit var userService: UserService

    // Service under test
    private lateinit var flashcardService: FlashcardCrudService

    private lateinit var user: UserEntity
    private lateinit var existingFlashcard: FlashcardEntity
    private val userId = UUID.randomUUID()
    private val flashcardId = UUID.randomUUID()

    // Dữ liệu đầu vào: S = 5.8 ngày, D = 3.99, lần ôn trước cách 6 ngày (t=6)
    private val initialStability = 5.8
    private val initialDifficulty = 3.99
    private val elapsedDays = 6.0

    @BeforeEach
    fun setup() {
        // Khởi tạo mocks
        flashcardRepository = mock()
        reviewLogRepository = mock()
        userRepository = mock()
        vocabularyRepository = mock()
        fsrsService = mock()
        userAuthUtil = mock()
        userService = mock()

        // Tạo user
        val userRole = RoleEntity(RoleEntity.ROLE_USER, "ROLE_USER")
        user =
            UserEntity(
                userId = userId,
                email = "test@example.com",
                password = "password",
                fullName = "Test User",
                profilePicture = null,
                currentLevel = null,
                jlptGoal = null,
                lastLogin = LocalDateTime.now(),
                role = userRole,
            )

        // Tạo thẻ đã có lịch sử ôn tập
        val dueDate = LocalDateTime.now().minus(elapsedDays.toLong(), ChronoUnit.DAYS)
        existingFlashcard =
            FlashcardEntity(
                flashcardId = flashcardId,
                user = user,
                frontText = "テスト",
                backText = "Test",
                difficulty = initialDifficulty,
                stability = initialStability,
                state = 2, // REVIEW
                elapsedDays = elapsedDays,
                scheduledDays = initialStability,
                due = dueDate,
                reps = 1,
                lapses = 0,
            )

        // Mock getCurrentUserId
        whenever(userAuthUtil.getCurrentUserId()).thenReturn(userId)

        // Mock findById
        whenever(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(existingFlashcard))

        // Khởi tạo service
        flashcardService =
            FlashcardCrudService(
                flashcardRepository,
                reviewLogRepository,
                userRepository,
                vocabularyRepository,
                fsrsService,
                userAuthUtil,
                userService,
            )
    }

    @Test
    @DisplayName("UT-02: Kiểm tra thẻ đã có lịch sử ôn tập")
    fun testExistingFlashcard() {
        // Kiểm tra giá trị khởi tạo
        assertEquals(initialDifficulty, existingFlashcard.difficulty!!, 0.01)
        assertEquals(initialStability, existingFlashcard.stability!!, 0.01)
        assertEquals(initialStability, existingFlashcard.scheduledDays, 0.01)
        assertEquals(2, existingFlashcard.state)
        assertEquals(1, existingFlashcard.reps)
        assertEquals(elapsedDays, existingFlashcard.elapsedDays, 0.01)
    }

    @Test
    @DisplayName("UT-02: Case 1 - Rating = 1, D = 5.70, S = 2.09, I = 2.09")
    fun testProcessReviewWithRating1() {
        // Mock processReview
        val updatedFlashcard =
            existingFlashcard.copy(
                difficulty = 5.70,
                stability = 2.09,
                scheduledDays = 2.09,
                state = 1, // LEARNING
                reps = 2,
            )
        whenever(fsrsService.processReview(any(), eq(1))).thenReturn(updatedFlashcard)

        // Xử lý đánh giá với rating = 1
        val result = flashcardService.processReview(flashcardId, 1)

        // Kiểm tra kết quả thực tế sau lần ôn tập
        assertEquals(5.70, result.data.difficulty!!, 0.01)
        assertEquals(2.09, result.data.stability!!, 0.01)
        assertEquals(2.09, result.data.interval!!, 0.01)

        // Verify interactions
        verify(flashcardRepository).findById(flashcardId)
        verify(fsrsService).processReview(any(), eq(1))
        verify(reviewLogRepository).save(any())
        verify(userService).updateUserStreak(userId)
    }

    @Test
    @DisplayName("UT-02: Case 2 - Rating = 2, D = 6.55, S = 3.16, I = 3.16")
    fun testProcessReviewWithRating2() {
        // Mock processReview
        val updatedFlashcard =
            existingFlashcard.copy(
                difficulty = 6.55,
                stability = 3.16,
                scheduledDays = 3.16,
                state = 2, // REVIEW
                reps = 2,
            )
        whenever(fsrsService.processReview(any(), eq(2))).thenReturn(updatedFlashcard)

        // Xử lý đánh giá với rating = 2
        val result = flashcardService.processReview(flashcardId, 2)

        // Kiểm tra kết quả thực tế sau lần ôn tập
        assertEquals(6.55, result.data.difficulty!!, 0.01)
        assertEquals(3.16, result.data.stability!!, 0.01)
        assertEquals(3.16, result.data.interval!!, 0.01)

        // Verify interactions
        verify(flashcardRepository).findById(flashcardId)
        verify(fsrsService).processReview(any(), eq(2))
        verify(reviewLogRepository).save(any())
        verify(userService).updateUserStreak(userId)
    }

    @Test
    @DisplayName("UT-02: Case 3 - Rating = 3, D = 6.53, S = 8.41, I = 8.41")
    fun testProcessReviewWithRating3() {
        // Mock processReview
        val updatedFlashcard =
            existingFlashcard.copy(
                difficulty = 6.53,
                stability = 8.41,
                scheduledDays = 8.41,
                state = 2, // REVIEW
                reps = 2,
            )
        whenever(fsrsService.processReview(any(), eq(3))).thenReturn(updatedFlashcard)

        // Xử lý đánh giá với rating = 3
        val result = flashcardService.processReview(flashcardId, 3)

        // Kiểm tra kết quả thực tế sau lần ôn tập
        assertEquals(6.53, result.data.difficulty!!, 0.01)
        assertEquals(8.41, result.data.stability!!, 0.01)
        assertEquals(8.41, result.data.interval!!, 0.01)

        // Verify interactions
        verify(flashcardRepository).findById(flashcardId)
        verify(fsrsService).processReview(any(), eq(3))
        verify(reviewLogRepository).save(any())
        verify(userService).updateUserStreak(userId)
    }

    @Test
    @DisplayName("UT-02: Case 4 - Rating = 4, D = 5.66, S = 46.44, I = 46.44")
    fun testProcessReviewWithRating4() {
        // Mock processReview
        val updatedFlashcard =
            existingFlashcard.copy(
                difficulty = 5.66,
                stability = 46.44,
                scheduledDays = 46.44,
                state = 2, // REVIEW
                reps = 2,
            )
        whenever(fsrsService.processReview(any(), eq(4))).thenReturn(updatedFlashcard)

        // Xử lý đánh giá với rating = 4
        val result = flashcardService.processReview(flashcardId, 4)

        // Kiểm tra kết quả thực tế sau lần ôn tập
        assertEquals(5.66, result.data.difficulty!!, 0.01)
        assertEquals(46.44, result.data.stability!!, 0.01)
        assertEquals(46.44, result.data.interval!!, 0.01)

        // Verify interactions
        verify(flashcardRepository).findById(flashcardId)
        verify(fsrsService).processReview(any(), eq(4))
        verify(reviewLogRepository).save(any())
        verify(userService).updateUserStreak(userId)
    }
}
