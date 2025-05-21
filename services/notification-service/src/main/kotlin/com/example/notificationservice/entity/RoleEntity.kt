package com.example.notificationservice.entity

import jakarta.persistence.*

@Entity
@Table(name = "roles")
data class RoleEntity(
    @Id
    @Column(name = "role_id")
    val roleId: Int,

    @Column(name = "role_name", length = 20)
    val roleName: String,
) {
    companion object {
        const val ROLE_ADMIN = 1
        const val ROLE_USER = 2
    }
}
