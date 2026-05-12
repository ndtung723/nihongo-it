package com.example.userservice.repository

import com.example.userservice.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {
    fun findByToken(token: String): RefreshTokenEntity?

    @Transactional
    fun deleteByUserId(userId: UUID)

    @Transactional
    fun deleteByToken(token: String)

    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.familyId = :familyId")
    fun deleteByFamilyId(familyId: UUID)

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :now")
    fun deleteAllExpired(now: LocalDateTime)
}
