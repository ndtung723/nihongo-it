package com.example.learningservice.service

import com.example.learningservice.config.FsrsProperties
import com.example.learningservice.entity.FlashcardEntity
import com.example.learningservice.entity.RoleEntity
import com.example.learningservice.entity.UserEntity
import com.example.learningservice.repository.FlashcardRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FSRSServiceTest {
    private lateinit var flashcardRepository: FlashcardRepository
    private lateinit var fsrsService: FSRSService
    private val props = FsrsProperties()

    private val userId = UUID.randomUUID()
    private val flashcardId = UUID.randomUUID()

    private fun makeUser() =
        UserEntity(
            userId = userId,
            email = "test@test.com",
            password = "pw",
            fullName = "Test",
            role = RoleEntity(RoleEntity.ROLE_USER, "ROLE_USER"),
        )

    private fun makeNewFlashcard() =
        FlashcardEntity(
            flashcardId = flashcardId,
            user = makeUser(),
            frontText = "テスト",
            backText = "test",
            stability = 0.0,
            difficulty = 0.0,
            state = FSRSService.State.NEW.value,
            due = LocalDateTime.now(),
            reps = 0,
            lapses = 0,
        )

    private fun makeReviewFlashcard(
        stability: Double = 2.4,
        difficulty: Double = 4.93,
    ) = FlashcardEntity(
        flashcardId = flashcardId,
        user = makeUser(),
        frontText = "テスト",
        backText = "test",
        stability = stability,
        difficulty = difficulty,
        state = FSRSService.State.REVIEW.value,
        due = LocalDateTime.now().minusDays(1),
        reps = 1,
        lapses = 0,
        scheduledDays = 2.4,
    )

    @BeforeEach
    fun setup() {
        flashcardRepository = mock()
        whenever(flashcardRepository.save(any())).thenAnswer { it.arguments[0] }
        fsrsService = FSRSService(flashcardRepository, props)
    }

    @Nested
    @DisplayName("initializeFlashcard()")
    inner class InitializeFlashcard {
        @Test
        @DisplayName("newly created card → state=NEW, stability=0, difficulty=0, reps=0")
        fun freshCard_hasNewState() {
            val card = makeNewFlashcard()

            val result = fsrsService.initializeFlashcard(card)

            assertEquals(FSRSService.State.NEW.value, result.state)
            assertEquals(0.0, result.stability)
            assertEquals(0.0, result.difficulty)
            assertEquals(0, result.reps)
            assertEquals(0, result.lapses)
        }
    }

    @Nested
    @DisplayName("processReview() — first review (reps=0)")
    inner class FirstReview {
        @Test
        @DisplayName("rating GOOD (3) → state=REVIEW, reps becomes 1")
        fun rating3_stateBecomesReview() {
            val card = makeNewFlashcard()

            val result = fsrsService.processReview(card, FSRSService.Rating.GOOD.value)

            assertEquals(FSRSService.State.REVIEW.value, result.state)
            assertEquals(1, result.reps)
            assertTrue(result.stability!! > 0.0, "stability should be positive after GOOD rating")
        }

        @Test
        @DisplayName("rating EASY (4) → state=REVIEW")
        fun rating4_stateBecomesReview() {
            val card = makeNewFlashcard()

            val result = fsrsService.processReview(card, FSRSService.Rating.EASY.value)

            assertEquals(FSRSService.State.REVIEW.value, result.state)
        }

        @Test
        @DisplayName("rating AGAIN (1) → state=LEARNING")
        fun rating1_stateBecomesLearning() {
            val card = makeNewFlashcard()

            val result = fsrsService.processReview(card, FSRSService.Rating.AGAIN.value)

            assertEquals(FSRSService.State.LEARNING.value, result.state)
        }

        @Test
        @DisplayName("rating HARD (2) → state=LEARNING")
        fun rating2_stateBecomesLearning() {
            val card = makeNewFlashcard()

            val result = fsrsService.processReview(card, FSRSService.Rating.HARD.value)

            assertEquals(FSRSService.State.LEARNING.value, result.state)
        }

        @Test
        @DisplayName("GOOD rating → stability equals default w[2] weight")
        fun goodRating_stabilityEqualsW2() {
            val card = makeNewFlashcard()

            val result = fsrsService.processReview(card, FSRSService.Rating.GOOD.value)

            assertEquals(props.weights[2], result.stability!!, 0.001)
        }

        @Test
        @DisplayName("due date is set in the future after review")
        fun dueDateSetInFuture() {
            val card = makeNewFlashcard()
            val before = LocalDateTime.now()

            val result = fsrsService.processReview(card, FSRSService.Rating.GOOD.value)

            assertTrue(
                result.due.isAfter(before) || result.due.isAfter(before.minusSeconds(1)),
                "Due date should be after (or equal to) now",
            )
        }
    }

    @Nested
    @DisplayName("processReview() — subsequent review (reps > 0)")
    inner class SubsequentReview {
        @Test
        @DisplayName("AGAIN rating → state=RELEARNING, lapses incremented")
        fun againRating_becomesRelearning() {
            val card = makeReviewFlashcard()

            val result = fsrsService.processReview(card, FSRSService.Rating.AGAIN.value)

            assertEquals(FSRSService.State.RELEARNING.value, result.state)
            assertEquals(1, result.lapses)
        }

        @Test
        @DisplayName("GOOD rating on review card → stays in REVIEW state")
        fun goodRatingOnReviewCard_staysInReview() {
            val card = makeReviewFlashcard()

            val result = fsrsService.processReview(card, FSRSService.Rating.GOOD.value)

            assertEquals(FSRSService.State.REVIEW.value, result.state)
        }

        @Test
        @DisplayName("reps always incremented by 1")
        fun repsIncrementedByOne() {
            val card = makeReviewFlashcard()
            val initialReps = card.reps

            val result = fsrsService.processReview(card, FSRSService.Rating.GOOD.value)

            assertEquals(initialReps + 1, result.reps)
        }
    }

    @Nested
    @DisplayName("Rating.fromInt()")
    inner class RatingFromInt {
        @Test
        @DisplayName("1 → AGAIN, 2 → HARD, 3 → GOOD, 4 → EASY")
        fun validValues_returnCorrectRating() {
            assertEquals(FSRSService.Rating.AGAIN, FSRSService.Rating.fromInt(1))
            assertEquals(FSRSService.Rating.HARD, FSRSService.Rating.fromInt(2))
            assertEquals(FSRSService.Rating.GOOD, FSRSService.Rating.fromInt(3))
            assertEquals(FSRSService.Rating.EASY, FSRSService.Rating.fromInt(4))
        }

        @Test
        @DisplayName("invalid value → throws IllegalArgumentException")
        fun invalidValue_throwsIllegalArgumentException() {
            assertThrows<IllegalArgumentException> {
                FSRSService.Rating.fromInt(5)
            }
            assertThrows<IllegalArgumentException> {
                FSRSService.Rating.fromInt(0)
            }
        }
    }
}
