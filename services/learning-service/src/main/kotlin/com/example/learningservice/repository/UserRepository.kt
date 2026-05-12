package com.example.learningservice.repository

import com.example.learningservice.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?

    // Statistics methods for dashboard
    fun countByCreatedAtAfter(sinceDate: Instant): Long
    fun countByLastLoginAfter(sinceDate: LocalDateTime): Long
    fun findTop10ByOrderByLastLoginDesc(): List<UserEntity>

    fun findByIsActiveTrueAndStreakCountGreaterThan(streakCount: Int): List<UserEntity>
}
