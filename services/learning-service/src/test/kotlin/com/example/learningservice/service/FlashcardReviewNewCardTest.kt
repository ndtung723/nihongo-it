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
import java.util.*
import java.util.Optional
import kotlin.test.assertEquals

class FlashcardReviewNewCardTest {
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
    private lateinit var newFlashcard: FlashcardEntity
    private val userId = UUID.randomUUID()
    private val flashcardId = UUID.randomUUID()

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

        // Tạo thẻ mới với các giá trị ban đầu
        newFlashcard =
            FlashcardEntity(
                flashcardId = flashcardId,
                user = user,
                frontText = "テスト",
                backText = "Test",
                difficulty = 0.0,
                stability = 0.0,
                state = 0, // NEW
                elapsedDays = 0.0,
                scheduledDays = 0.0,
                due = LocalDateTime.now(),
                reps = 0,
                lapses = 0,
            )

        // Mock getCurrentUserId - cách đúng để mock UserAuthUtil
        whenever(userAuthUtil.getCurrentUserId()).thenReturn(userId)

        // Mock findById
        whenever(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(newFlashcard))

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
    @DisplayName("UT-01: Kiểm tra khởi tạo thẻ mới")
    fun testInitializeNewFlashcard() {
        // Kiểm tra giá trị khởi tạo
        assertEquals(0.0, newFlashcard.difficulty!!)
        assertEquals(0.0, newFlashcard.stability!!)
        assertEquals(0.0, newFlashcard.scheduledDays)
        assertEquals(0, newFlashcard.state)
        assertEquals(0, newFlashcard.reps)
    }

    @Test
    @DisplayName("UT-01: Case 1 - Rating = 1, D = 6.81, S = 0.4, I = 0.4")
    fun testProcessReviewWithRating1() {
        // Mock processReview
        val updatedFlashcard =
            newFlashcard.copy(
                difficulty = 6.81,
                stability = 0.4,
                scheduledDays = 0.4,
                state = 1, // LEARNING
                reps = 1,
            )
        whenever(fsrsService.processReview(any(), eq(1))).thenReturn(updatedFlashcard)

        // Xử lý đánh giá với rating = 1
        val result = flashcardService.processReview(flashcardId, 1)

        // Kiểm tra kết quả thực tế sau lần ôn tập đầu tiên
        assertEquals(6.81, result.data.difficulty!!, 0.01)
        assertEquals(0.4, result.data.stability!!, 0.01)
        assertEquals(0.4, result.data.interval!!, 0.01)

        // Verify interactions
        verify(flashcardRepository).findById(flashcardId)
        verify(fsrsService).processReview(any(), eq(1))
        verify(reviewLogRepository).save(any())
        verify(userService).updateUserStreak(userId)
    }

    @Test
    @DisplayName("UT-01: Case 2 - Rating = 2, D = 5.87, S = 0.6, I = 0.6")
    fun testProcessReviewWithRating2() {
        // Mock processReview
        val updatedFlashcard =
            newFlashcard.copy(
                difficulty = 5.87,
                stability = 0.6,
                scheduledDays = 0.6,
                state = 1, // LEARNING
                reps = 1,
            )
        whenever(fsrsService.processReview(any(), eq(2))).thenReturn(updatedFlashcard)

        // Xử lý đánh giá với rating = 2
        val result = flashcardService.processReview(flashcardId, 2)

        // Kiểm tra kết quả thực tế sau lần ôn tập đầu tiên
        assertEquals(5.87, result.data.difficulty!!, 0.01)
        assertEquals(0.6, result.data.stability!!, 0.01)
        assertEquals(0.6, result.data.interval!!, 0.01)

        // Verify interactions
        verify(flashcardRepository).findById(flashcardId)
        verify(fsrsService).processReview(any(), eq(2))
        verify(reviewLogRepository).save(any())
        verify(userService).updateUserStreak(userId)
    }

    @Test
    @DisplayName("UT-01: Case 3 - Rating = 3, D = 4.93, S = 2.40, I = 2.40")
    fun testProcessReviewWithRating3() {
        // Mock processReview
        val updatedFlashcard =
            newFlashcard.copy(
                difficulty = 4.93,
                stability = 2.40,
                scheduledDays = 2.40,
                state = 2, // REVIEW
                reps = 1,
            )
        whenever(fsrsService.processReview(any(), eq(3))).thenReturn(updatedFlashcard)

        // Xử lý đánh giá với rating = 3
        val result = flashcardService.processReview(flashcardId, 3)

        // Kiểm tra kết quả thực tế sau lần ôn tập đầu tiên
        assertEquals(4.93, result.data.difficulty!!, 0.01)
        assertEquals(2.40, result.data.stability!!, 0.01)
        assertEquals(2.40, result.data.interval!!, 0.01)

        // Verify interactions
        verify(flashcardRepository).findById(flashcardId)
        verify(fsrsService).processReview(any(), eq(3))
        verify(reviewLogRepository).save(any())
        verify(userService).updateUserStreak(userId)
    }

    @Test
    @DisplayName("UT-01: Case 4 - Rating = 4, D = 3.99, S = 5.80, I = 5.80")
    fun testProcessReviewWithRating4() {
        // Mock processReview
        val updatedFlashcard =
            newFlashcard.copy(
                difficulty = 3.99,
                stability = 5.80,
                scheduledDays = 5.80,
                state = 2, // REVIEW
                reps = 1,
            )
        whenever(fsrsService.processReview(any(), eq(4))).thenReturn(updatedFlashcard)

        // Xử lý đánh giá với rating = 4
        val result = flashcardService.processReview(flashcardId, 4)

        // Kiểm tra kết quả thực tế sau lần ôn tập đầu tiên
        assertEquals(3.99, result.data.difficulty!!, 0.01)
        assertEquals(5.80, result.data.stability!!, 0.01)
        assertEquals(5.80, result.data.interval!!, 0.01)

        // Verify interactions
        verify(flashcardRepository).findById(flashcardId)
        verify(fsrsService).processReview(any(), eq(4))
        verify(reviewLogRepository).save(any())
        verify(userService).updateUserStreak(userId)
    }
}
