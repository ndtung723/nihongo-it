package com.example.learningservice.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "conversation_lines")
data class ConversationLineEntity(
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "line_id", updatable = false, nullable = false)
    val lineId: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    val conversation: ConversationEntity,
    @Column(nullable = false)
    var speaker: String,
    @Column(name = "japanese_text", nullable = false, columnDefinition = "TEXT")
    var japaneseText: String,
    @Column(name = "vietnamese_translation", nullable = true, columnDefinition = "TEXT")
    var vietnameseTranslation: String? = null,
    @Column(nullable = true, columnDefinition = "TEXT")
    var notes: String? = null,
    @Column(name = "important_vocab", nullable = true, columnDefinition = "TEXT")
    var importantVocab: String? = null,
    @Column(name = "order_index", nullable = false)
    var orderIndex: Int,
)
