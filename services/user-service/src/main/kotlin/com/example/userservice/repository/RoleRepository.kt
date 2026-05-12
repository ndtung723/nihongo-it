package com.example.userservice.repository

import com.example.userservice.entity.RoleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<RoleEntity, Int> {
    fun findByRoleId(roleId: Int): RoleEntity?
}
