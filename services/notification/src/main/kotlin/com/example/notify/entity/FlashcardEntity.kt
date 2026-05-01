package com.example.notify.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "flashcards")
data class FlashcardEntity(
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "flashcard_id", updatable = false, nullable = false)
    val flashcardId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocabulary_id")
    val vocabulary: VocabularyEntity? = null,

    @Column(name = "front_text", columnDefinition = "text")
    var frontText: String,

    @Column(name = "back_text",  columnDefinition = "text")
    var backText: String,

    // FSRS algorithm fields
    @Column(name = "difficulty", nullable = true)
    var difficulty: Double? = null, // Item difficulty (chỉ có giá trị sau đánh giá đầu tiên)

    @Column(name = "stability", nullable = true)
    var stability: Double? = null, // Memory stability (chỉ có giá trị sau đánh giá đầu tiên)

    @Column(name = "state")
    var state: Int = 0, // 0=New, 1=Learning, 2=Review, 3=Relearning

    @Column(name = "elapsed_days")
    var elapsedDays: Double = 0.0, // Days since last review

    @Column(name = "scheduled_days")
    var scheduledDays: Double = 0.0, // Days scheduled before next review

    @Column(name = "due")
    var due: LocalDateTime = LocalDateTime.now(), // Next review date

    @Column(name = "reps")
    var reps: Int = 0, // Number of repetitions

    @Column(name = "lapses")
    var lapses: Int = 0, // Number of times forgotten

    @Column(name = "is_suspended")
    var isSuspended: Boolean = false, // Whether card is temporarily suspended

    @OneToMany(mappedBy = "flashcard", cascade = [CascadeType.ALL], orphanRemoval = true)
    var reviewLogs: MutableList<ReviewLogEntity> = mutableListOf(),

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
