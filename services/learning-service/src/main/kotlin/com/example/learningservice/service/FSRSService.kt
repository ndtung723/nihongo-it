package com.example.learningservice.service

import com.example.learningservice.config.FsrsProperties
import com.example.learningservice.entity.FlashcardEntity
import com.example.learningservice.repository.FlashcardRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

/**
 * Free Spaced Repetition Scheduler (FSRS) 4.0 implementation
 * Based on the FSRS v4 algorithm
 */
@Service
class FSRSService(
    private val flashcardRepository: FlashcardRepository,
    private val fsrsProperties: FsrsProperties,
) {
    private val logger = LoggerFactory.getLogger(FSRSService::class.java)

    private val w get() = fsrsProperties.weights.toDoubleArray()
    private val requestRetention get() = fsrsProperties.requestRetention
    private val maximumInterval get() = fsrsProperties.maximumInterval

    /**
     * Rating enum similar to the Go implementation
     */
    enum class Rating(val value: Int) {
        AGAIN(1),
        HARD(2),
        GOOD(3),
        EASY(4),
        ;

        companion object {
            fun fromInt(value: Int): Rating = when (value) {
                1 -> AGAIN
                2 -> HARD
                3 -> GOOD
                4 -> EASY
                else -> throw IllegalArgumentException("Invalid rating: $value")
            }
        }
    }

    /**
     * State enum similar to the Go implementation
     */
    enum class State(val value: Int) {
        NEW(0),
        LEARNING(1),
        REVIEW(2),
        RELEARNING(3),
        ;

        companion object {
            fun fromInt(value: Int): State = State.entries.first { it.value == value }
        }
    }

    /**
     * SchedulingInfo class similar to the Go implementation
     */
    data class SchedulingInfo(
        val due: LocalDateTime,
        val stability: Double,
        val difficulty: Double,
        val elapsedDays: Double,
        val scheduledDays: Double,
        val state: Int,
        val reps: Int,
        val lapses: Int,
    )

    /**
     * Process a flashcard review
     */
    @Transactional
    fun processReview(flashcard: FlashcardEntity, ratingValue: Int): FlashcardEntity {
        val rating = Rating.fromInt(ratingValue)
        val now = LocalDateTime.now()
        val isFirstReview = flashcard.reps == 0

        logger.info("Processing review with FSRS - Flashcard ID: ${flashcard.flashcardId}, Rating: $rating")
        logger.info("Is first review? $isFirstReview")

        if (isFirstReview) {
            // Lần đầu review: chỉ khởi tạo S₀(G), D₀(G), tính I, cập nhật state, scheduledDays, due
            flashcard.stability = when (rating) {
                Rating.AGAIN -> w[0]
                Rating.HARD -> w[1]
                Rating.GOOD -> w[2]
                Rating.EASY -> w[3]
            }
            flashcard.difficulty = w[4] - (rating.value - 3) * w[5]
            val interval = nextInterval(flashcard.stability ?: 1.0)
            flashcard.scheduledDays = interval
            flashcard.state = if (rating.value >= 3) State.REVIEW.value else State.LEARNING.value
            val days = interval.toLong()
            val hours = ((interval - days) * 24).toLong()
            flashcard.due = now.plusDays(days).plusHours(hours)
            // Không cập nhật lapses, elapsedDays, ...
        } else {
            // Các lần review tiếp theo: luôn dùng calculateSchedulingInfo cho mọi rating
            val elapsedDays = if (flashcard.due.isBefore(now)) {
                ChronoUnit.MILLIS.between(flashcard.due, now).toDouble() / (1000 * 60 * 60 * 24)
            } else {
                0.0
            }
            val info = calculateSchedulingInfo(flashcard, rating, elapsedDays, now)
            flashcard.difficulty = info.difficulty
            flashcard.stability = info.stability
            flashcard.state = info.state
            flashcard.elapsedDays = info.elapsedDays
            flashcard.scheduledDays = info.scheduledDays
            flashcard.due = info.due
            if (rating == Rating.AGAIN) {
                flashcard.lapses += 1
            }
        }
        flashcard.reps += 1
        flashcard.updatedAt = Instant.now()
        return flashcardRepository.save(flashcard)
    }

    /**
     * Initialize a new flashcard with default FSRS values
     * Không khởi tạo stability và difficulty - chỉ khi đánh giá lần đầu mới tính toán
     */
    @Transactional
    fun initializeFlashcard(flashcard: FlashcardEntity): FlashcardEntity {
        logger.info("Initializing new flashcard without FSRS parameters (will be set on first review)")

        // Thẻ mới chưa có stability và difficulty
        flashcard.stability = 0.0
        flashcard.difficulty = 0.0

        flashcard.state = State.NEW.value
        flashcard.elapsedDays = 0.0
        flashcard.scheduledDays = 0.0 // Chưa có khoảng thời gian
        flashcard.due = LocalDateTime.now() // Due ngay lập tức
        flashcard.reps = 0
        flashcard.lapses = 0

        return flashcardRepository.save(flashcard)
    }

    /**
     * Calculate the scheduling information based on the card state and rating
     */
    private fun calculateSchedulingInfo(
        card: FlashcardEntity,
        rating: Rating,
        elapsedDays: Double,
        now: LocalDateTime,
    ): SchedulingInfo {
        // Current state
        val state = State.fromInt(card.state)

        // Update difficulty
        val difficulty = card.difficulty ?: 5.0
        val stability = card.stability ?: 1.0
        val newDifficulty = updateDifficulty(difficulty, rating)

        // Calculate retrievability
        val retrievability = calculateRetrievability(stability, elapsedDays)

        // Update stability based on rating
        val newStability = updateStability(stability, newDifficulty, rating, retrievability)

        // Determine new state
        val newState = determineState(state, rating)

        // Calculate next interval
        val interval = calculateInterval(newStability, rating, newState)

        // Calculate new due date with precise time (not just days)
        val days = interval.toLong()
        val hours = ((interval - days) * 24).toLong()
        val due = now.plusDays(days).plusHours(hours)

        return SchedulingInfo(
            due = due,
            stability = newStability,
            difficulty = newDifficulty,
            elapsedDays = 0.0, // Reset elapsed days
            scheduledDays = interval,
            state = newState.value,
            reps = card.reps + 1,
            lapses = if (rating == Rating.AGAIN) card.lapses + 1 else card.lapses,
        )
    }

    /**
     * Calculate interval based on stability and state
     */
    private fun calculateInterval(stability: Double, rating: Rating, state: State): Double {
        // Sử dụng công thức FSRS cho tất cả các trạng thái
        val interval = nextInterval(stability)
        logger.info("FSRS calculation - Base interval calculated from stability $stability: $interval days")

        val finalInterval = when (rating) {
            Rating.AGAIN -> 0.0 // Same day (handled separately in processReview)
            Rating.HARD -> {
                // Không sử dụng w15 ở đây, vì đã được áp dụng trong updateStability
                max(1.0, interval)
            }
            Rating.GOOD -> {
                logger.info("GOOD rating - Using normal interval: $interval days")
                max(1.0, interval)
            }
            Rating.EASY -> {
                // Không sử dụng w16 ở đây, vì đã được áp dụng trong updateStability
                max(1.0, interval)
            }
        }.coerceAtMost(maximumInterval)

        logger.info("Final interval after adjustments: $finalInterval days")
        return finalInterval
    }

    /**
     * Calculates the next review interval based on stability
     * I(r,S) = 9 * S * (1/r - 1)
     */
    private fun nextInterval(stability: Double): Double {
        val requestRetentionFactor = 1.0 / requestRetention - 1.0
        logger.info("Request retention: $requestRetention, factor: $requestRetentionFactor")

        val interval = 9.0 * stability * requestRetentionFactor
        logger.info("FSRS formula: 9.0 * $stability * $requestRetentionFactor = $interval")

        val cappedInterval = min(interval, maximumInterval)
        if (cappedInterval < interval) {
            logger.info("Interval capped to maximum: $maximumInterval days")
        }

        return cappedInterval
    }

    /**
     * Updates the difficulty score based on the rating
     * D'(D,G) = w_7 * D_0(3) + (1-w_7) * (D - w_6 * (G-3))
     */
    private fun updateDifficulty(oldDifficulty: Double, rating: Rating): Double {
        // Initial difficulty for "Good" (G=3): D_0(3) = w_4
        val initialDifficulty = w[4]

        // Calculate new difficulty: D' = D - w_6 * (G-3)
        val difficultyChange = oldDifficulty - w[6] * (rating.value - 3)

        // Apply mean reversion: D' = w_7 * D_0(3) + (1-w_7) * D'
        val newDifficulty = w[7] * initialDifficulty + (1 - w[7]) * difficultyChange

        // Clamp difficulty between 1 and 10
        return max(min(newDifficulty, 10.0), 1.0)
    }

    /**
     * Updates the stability score based on rating, difficulty, and retrievability
     */
    private fun updateStability(oldStability: Double, difficulty: Double, rating: Rating, retrievability: Double): Double {
        return when (rating) {
            Rating.AGAIN -> {
                // Stability after forgetting (post-lapse stability):
                // S_f'(D,S,R) = w_11 * D^(-w_12) * ((S+1)^w_13 - 1) * e^(w_14 * (1-R))
                val forgettingStability = w[11] * Math.pow(difficulty, -w[12]) *
                    (Math.pow(oldStability + 1, w[13]) - 1) *
                    Math.exp(w[14] * (1 - retrievability))

                logger.info("AGAIN rating: Calculating post-lapse stability using formula S_f'")
                logger.info("Formula components: w11=${w[11]}, D^(-w12)=${Math.pow(difficulty, -w[12])}")
                logger.info("Formula components: (S+1)^w13-1=${Math.pow(oldStability + 1, w[13]) - 1}")
                logger.info("Formula components: e^(w14*(1-R))=${Math.exp(w[14] * (1 - retrievability))}")
                logger.info("Old stability: $oldStability, New stability: $forgettingStability")

                forgettingStability.coerceAtLeast(0.1) // Ensure minimum stability
            }
            else -> {
                // Stability after successful review:
                // S_r'(D,S,R,G) = S * (e^w_8 * (11-D) * S^(-w_9) * (e^(w_10*(1-R))-1) * w_15(if G=2) * w_16(if G=4) + 1)

                // Tính toán các thành phần của công thức
                val factor1 = Math.exp(w[8]) // e^w_8
                val factor2 = 11.0 - difficulty // (11-D)
                val factor3 = Math.pow(oldStability, -w[9]) // S^(-w_9)
                val factor4 = Math.exp(w[10] * (1.0 - retrievability)) - 1.0 // (e^(w_10*(1-R))-1)

                // Áp dụng hệ số đặc biệt cho HARD và EASY
                val hardMultiplier = if (rating == Rating.HARD) w[15] else 1.0
                val easyMultiplier = if (rating == Rating.EASY) w[16] else 1.0

                val stabilityIncrease = factor1 * factor2 * factor3 * factor4 * hardMultiplier * easyMultiplier
                val newStability = oldStability * (stabilityIncrease + 1.0)

                logger.info("${rating.name} rating: Calculating success stability using formula S_r'")
                logger.info("Formula components: e^w8=$factor1, (11-D)=$factor2")
                logger.info("Formula components: S^(-w9)=$factor3, e^(w10*(1-R))-1=$factor4")
                if (rating == Rating.HARD) {
                    logger.info("Applying HARD multiplier w15=${w[15]}")
                } else if (rating == Rating.EASY) {
                    logger.info("Applying EASY multiplier w16=${w[16]}")
                }
                logger.info("Stability increase factor: $stabilityIncrease")
                logger.info("Old stability: $oldStability, New stability: $newStability")

                newStability.coerceAtLeast(0.1) // Ensure minimum stability
            }
        }
    }

    /**
     * Calculates the retrievability score based on spacing effect
     * R(t,S) = (1 + t/(9*S))^(-1)
     */
    private fun calculateRetrievability(stability: Double, elapsedDays: Double): Double {
        val safeStability = stability.coerceAtLeast(0.1) // Avoid division by zero
        return Math.pow(1.0 + elapsedDays / (9.0 * safeStability), -1.0)
    }

    /**
     * Determines the new state of the card based on current state and rating
     */
    private fun determineState(oldState: State, rating: Rating): State {
        return when {
            rating == Rating.AGAIN -> State.RELEARNING // Any "Again" rating goes to relearning
            oldState == State.NEW || oldState == State.LEARNING || oldState == State.RELEARNING -> {
                if (rating.value >= 3) State.REVIEW else State.LEARNING // Move to review if Good/Easy
            }
            else -> State.REVIEW // Stay in review
        }
    }
}
