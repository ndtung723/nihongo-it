package com.example.notify.repository

import com.example.notify.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<UserEntity, UUID> {
    // Find active users with reminders enabled
    fun findByIsActiveAndReminderEnabled(
        isActive: Boolean,
        reminderEnabled: Boolean,
    ): List<UserEntity>
}
