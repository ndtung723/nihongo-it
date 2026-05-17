package com.example.notify.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "user_id", updatable = false, nullable = false)
    val userId: UUID? = null,
    @Column(name = "email", nullable = false, unique = true, length = 50)
    val email: String,
    @Column(name = "password", nullable = false)
    val password: String,
    @Column(name = "full_name", nullable = false, length = 100)
    val fullName: String,
    @Column(name = "profile_picture")
    val profilePicture: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "current_level")
    val currentLevel: JlptLevel?,
    @Enumerated(EnumType.STRING)
    @Column(name = "jlpt_goal")
    val jlptGoal: JlptLevel?,
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @Column(name = "is_email_verified")
    val isEmailVerified: Boolean = false,
    @Column(name = "verification_token")
    val verificationToken: String? = null,
    @Column(name = "reset_password_token")
    val resetPasswordToken: String? = null,
    @Column(name = "reset_password_expires")
    val resetPasswordExpires: LocalDateTime? = null,
    @Column(name = "last_login")
    val lastLogin: LocalDateTime?,
    @Column(name = "streak_count")
    val streakCount: Int = 0,
    @Column(name = "last_study_date")
    val lastStudyDate: LocalDateTime? = null,
    @Column(name = "points")
    val points: Int = 0,
    @Column(name = "daily_goal_minutes")
    val dailyGoalMinutes: Int = 15,
    @Column(name = "reminder_enabled")
    val reminderEnabled: Boolean = true,
    @Column(name = "reminder_time")
    val reminderTime: LocalTime? = LocalTime.of(20, 0), // Default reminder at 8 PM
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "user_notification_preferences",
        joinColumns = [JoinColumn(name = "user_id")],
    )
    @Column(name = "preference", length = 50)
    val notificationPreferences: MutableSet<String> = mutableSetOf("email", "app"),
    @Column(name = "min_card_threshold")
    val minCardThreshold: Int? = 5, // Minimum number of cards before sending notification
    @Column(name = "firebase_token")
    val firebaseToken: String? = null,
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    var role: RoleEntity,
    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    val flashcards: MutableList<FlashcardEntity> = mutableListOf(),
    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
