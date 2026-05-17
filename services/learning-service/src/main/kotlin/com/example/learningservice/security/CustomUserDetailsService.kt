package com.example.learningservice.security

import com.example.learningservice.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(email: String): UserDetails {
        val user =
            userRepository.findByEmail(email)
                ?: throw UsernameNotFoundException("User not found with email: $email")

        val authorities =
            when (user.role.roleId) {
                1 -> listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
                2 -> listOf(SimpleGrantedAuthority("ROLE_USER"))
                else -> throw IllegalArgumentException("Invalid roleId: ${user.role.roleId}")
            }

        return org.springframework.security.core.userdetails.User
            .withUsername(user.email)
            .password(user.password)
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build()
    }
}
