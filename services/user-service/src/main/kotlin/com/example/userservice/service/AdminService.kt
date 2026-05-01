package com.example.userservice.service

import com.example.userservice.dto.UserCreateRequest
import com.example.userservice.dto.UserDto
import com.example.userservice.dto.UserListResponse
import com.example.userservice.dto.UserUpdateRequest
import com.example.userservice.entity.UserEntity
import com.example.common.exception.BusinessException
import com.example.userservice.repository.RoleRepository
import com.example.userservice.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private val logger = LoggerFactory.getLogger(AdminService::class.java)
    
    /**
     * Get all users with pagination and optional search
     */
    fun getAllUsers(pageable: Pageable, search: String?): UserListResponse {
        val userPage: Page<UserEntity> = if (search.isNullOrBlank()) {
            userRepository.findAll(pageable)
        } else {
            // Use a custom repository method to search by email or fullName
            // This is a basic implementation, you may need to create a custom query
            userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                search, search, pageable
            )
        }
        
        val users = userPage.content.map { it.toUserDto() }
        
        return UserListResponse(
            users = users,
            totalItems = userPage.totalElements,
            totalPages = userPage.totalPages,
            currentPage = pageable.pageNumber
        )
    }
    
    /**
     * Get user by ID
     */
    fun getUserById(userId: UUID): UserDto {
        val user = findUserById(userId)
        return user.toUserDto()
    }
    
    /**
     * Create a new user
     */
    @Transactional
    fun createUser(request: UserCreateRequest): UserDto {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException("Email ${request.email} is already registered")
        }
        
        // Get role
        val role = roleRepository.findByRoleId(request.roleId)
            ?: throw BusinessException("Invalid role ID: ${request.roleId}")
        
        // Create user entity
        val newUser = UserEntity(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            fullName = request.fullName,
            profilePicture = request.profilePicture,
            currentLevel = request.currentLevel,
            jlptGoal = request.jlptGoal,
            isActive = true,
            isEmailVerified = true, // Admin-created accounts are pre-verified
            lastLogin = null,
            role = role,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        val savedUser = userRepository.save(newUser)
        logger.info("User created by admin: ${savedUser.email}")
        
        return savedUser.toUserDto()
    }
    
    /**
     * Update an existing user
     */
    @Transactional
    fun updateUser(userId: UUID, request: UserUpdateRequest): UserDto {
        val existingUser = findUserById(userId)
        
        // Check if email is being changed and already exists
        if (request.email != null && 
            request.email != existingUser.email && 
            userRepository.existsByEmail(request.email)) {
            throw BusinessException("Email ${request.email} is already registered")
        }
        
        // Prepare role if it's being updated
        val role = if (request.roleId != null) {
            roleRepository.findByRoleId(request.roleId)
                ?: throw BusinessException("Invalid role ID: ${request.roleId}")
        } else {
            existingUser.role
        }
        
        // Parse reminder time if provided
        val reminderTime = if (request.reminderTime != null) {
            try {
                LocalTime.parse(request.reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
            } catch (e: Exception) {
                throw BusinessException("Invalid reminder time format. Use HH:mm")
            }
        } else {
            existingUser.reminderTime
        }
        
        // Update user with non-null values from request
        val updatedUser = existingUser.copy(
            email = request.email ?: existingUser.email,
            password = if (request.password != null) 
                passwordEncoder.encode(request.password) else existingUser.password,
            fullName = request.fullName ?: existingUser.fullName,
            profilePicture = request.profilePicture ?: existingUser.profilePicture,
            currentLevel = request.currentLevel ?: existingUser.currentLevel,
            jlptGoal = request.jlptGoal ?: existingUser.jlptGoal,
            isActive = request.isActive ?: existingUser.isActive,
            isEmailVerified = request.isEmailVerified ?: existingUser.isEmailVerified,
            reminderEnabled = request.reminderEnabled ?: existingUser.reminderEnabled,
            reminderTime = reminderTime,
            notificationPreferences = request.notificationPreferences
                ?.split(",")?.map { it.trim() }?.toMutableSet()
                ?: existingUser.notificationPreferences,
            minCardThreshold = request.minCardThreshold ?: existingUser.minCardThreshold,
            role = role,
            updatedAt = LocalDateTime.now()
        )
        
        val savedUser = userRepository.save(updatedUser)
        logger.info("User updated by admin: ${savedUser.email}")
        
        return savedUser.toUserDto()
    }
    
    /**
     * Deactivate a user (soft delete)
     */
    @Transactional
    fun deactivateUser(userId: UUID) {
        val user = findUserById(userId)
        
        // Don't allow deactivation of the last admin user
        if (user.role.roleId == 1) {
            val adminCount = userRepository.countByRoleRoleIdAndIsActive(1, true)
            if (adminCount <= 1) {
                throw BusinessException("Cannot deactivate the last active admin user")
            }
        }
        
        val deactivatedUser = user.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )
        
        userRepository.save(deactivatedUser)
        logger.info("User deactivated by admin: ${user.email}")
    }
    
    /**
     * Activate a user
     */
    @Transactional
    fun activateUser(userId: UUID) {
        val user = findUserById(userId)
        
        val activatedUser = user.copy(
            isActive = true,
            updatedAt = LocalDateTime.now()
        )
        
        userRepository.save(activatedUser)
        logger.info("User activated by admin: ${user.email}")
    }
    
    /**
     * Change a user's role
     */
    @Transactional
    fun changeUserRole(userId: UUID, roleId: Int) {
        val user = findUserById(userId)
        
        // Get the new role
        val newRole = roleRepository.findByRoleId(roleId)
            ?: throw BusinessException("Invalid role ID: $roleId")
        
        // Don't allow changing the last admin to a regular user
        if (user.role.roleId == 1 && roleId != 1) {
            val adminCount = userRepository.countByRoleRoleIdAndIsActive(1, true)
            if (adminCount <= 1) {
                throw BusinessException("Cannot change role of the last active admin user")
            }
        }
        
        val updatedUser = user.copy(
            role = newRole,
            updatedAt = LocalDateTime.now()
        )
        
        userRepository.save(updatedUser)
        logger.info("User role changed by admin: ${user.email}, new role: ${newRole.roleName}")
    }
    
    /**
     * Helper method to find a user by ID or throw exception
     */
    private fun findUserById(userId: UUID): UserEntity {
        return userRepository.findById(userId)
            .orElseThrow { BusinessException("User not found with ID: $userId") }
    }
    
    /**
     * Extension function to convert UserEntity to UserDto
     */
    private fun UserEntity.toUserDto(): UserDto {
        return UserDto(
            userId = this.userId ?: throw BusinessException("User ID is null"),
            email = this.email,
            fullName = this.fullName,
            roleId = this.role.roleId,
            profilePicture = this.profilePicture,
            currentLevel = this.currentLevel,
            jlptGoal = this.jlptGoal,
            lastLogin = this.lastLogin,
            isActive = this.isActive
        )
    }
} 