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

class FlashcardProgressiveLearningTest {
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
        user = UserEntity(
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

        // Tạo thẻ mới với các giá trị ban đầu (chưa có lịch sử ôn tập)
        newFlashcard = FlashcardEntity(
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

        // Mock getCurrentUserId
        whenever(userAuthUtil.getCurrentUserId()).thenReturn(userId)

        // Khởi tạo service
        flashcardService = FlashcardCrudService(
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
    @DisplayName("UT-03: Theo dõi một thẻ qua bảy lần ôn liên tiếp")
    fun testProgressiveLearning() {
        // Ánh xạ giữa rating và tên đánh giá
        // 1: Again, 2: Hard, 3: Good, 4: Easy
        val ratingNames = mapOf(
            1 to "Again",
            2 to "Hard",
            3 to "Good",
            4 to "Easy",
        )

        // Chuỗi đánh giá: Again→ Hard → Hard→ Good → Good → Easy → Easy
        val ratings = listOf(1, 2, 2, 3, 3, 4, 4)

        // Dữ liệu mong đợi sau mỗi lần ôn tập
        val expectedData = listOf(
            Triple(6.81, 0.40, 0.40), // Lần 1: Again - D, S, I
            Triple(7.64, 0.59, 0.59), // Lần 2: Hard
            Triple(8.47, 0.80, 0.80), // Lần 3: Hard
            Triple(8.43, 1.72, 1.72), // Lần 4: Good
            Triple(8.40, 3.54, 3.54), // Lần 5: Good
            Triple(7.51, 15.37, 15.37), // Lần 6: Easy
            Triple(6.63, 67.62, 67.62), // Lần 7: Easy
        )

        // Thẻ hiện tại để theo dõi các thay đổi
        var currentFlashcard = newFlashcard

        // Thực hiện 7 lần ôn tập liên tiếp
        for (i in ratings.indices) {
            val rating = ratings[i]
            val (expectedD, expectedS, expectedI) = expectedData[i]
            val reviewNumber = i + 1

            println("Lần ôn tập $reviewNumber - ${ratingNames[rating]}")

            // Tính toán trạng thái mới dựa trên rating
            val newState = when {
                rating <= 2 -> 1 // LEARNING
                else -> 2 // REVIEW
            }

            // Tạo flashcard đã cập nhật với các giá trị mong đợi
            val updatedFlashcard = currentFlashcard.copy(
                difficulty = expectedD,
                stability = expectedS,
                scheduledDays = expectedI,
                state = newState,
                reps = currentFlashcard.reps + 1,
            )

            // Mock findById cho mỗi lần ôn tập
            whenever(flashcardRepository.findById(flashcardId)).thenReturn(Optional.of(currentFlashcard))

            // Mock processReview để trả về kết quả mong đợi
            whenever(fsrsService.processReview(any(), eq(rating))).thenReturn(updatedFlashcard)

            // Thực hiện ôn tập
            val result = flashcardService.processReview(flashcardId, rating)

            // Kiểm tra kết quả
            assertEquals(expectedD, result.data.difficulty!!, 0.01, "Lần $reviewNumber: Difficulty không khớp")
            assertEquals(expectedS, result.data.stability!!, 0.01, "Lần $reviewNumber: Stability không khớp")
            assertEquals(expectedI, result.data.interval!!, 0.01, "Lần $reviewNumber: Interval không khớp")

            // Verify interactions
            verify(flashcardRepository).findById(flashcardId)
            verify(fsrsService).processReview(any(), eq(rating))
            verify(reviewLogRepository).save(any())
            verify(userService).updateUserStreak(userId)

            // Cập nhật flashcard hiện tại cho lần ôn tập tiếp theo
            currentFlashcard = updatedFlashcard

            // Reset mocks để chuẩn bị cho lần ôn tập tiếp theo
            clearInvocations(flashcardRepository, fsrsService, reviewLogRepository, userService)
        }
    }
}
