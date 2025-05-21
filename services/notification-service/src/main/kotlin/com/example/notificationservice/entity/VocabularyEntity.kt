package com.example.notificationservice.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "vocabulary", 
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["term"], name = "uk_vocabulary_term")
    ]
)
data class VocabularyEntity(
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "vocab_id", updatable = false, nullable = false)
    val vocabId: UUID? = null,

    @Column(name = "term", nullable = false, unique = true)
    val term: String?, // japanese term (kanji, hiragana, katakana)

    @Column(name = "meaning", nullable = false)
    val meaning: String, // vietnamese meaning

    @Column(name = "pronunciation")
    val pronunciation: String?, // VD: かんすう (có thể null)

    @Column(name = "example", columnDefinition = "text")
    val example: String?, // VD: 私は日本語を勉強しています。 (có thể null)

    @Column(name = "example_meaning", columnDefinition = "text")
    val exampleMeaning: String?, // vietnamese meaning of the example

    @Column(name = "audio_path")
    val audioPath: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_level")
    val jlptLevel: JlptLevel,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    val topic: TopicEntity,

    @Column(name = "created_at")
    val createdAt: LocalDateTime?,

    @OneToMany(mappedBy = "vocabulary", cascade = [CascadeType.ALL])
    val flashcards: MutableList<FlashcardEntity> = mutableListOf(),

    @ManyToMany
    @JoinTable(
        name = "saved_vocabulary",
        joinColumns = [JoinColumn(name = "vocab_id")],
        inverseJoinColumns = [JoinColumn(name = "user_id")],
    )
    val savedByUsers: MutableSet<UserEntity> = mutableSetOf(),
)
