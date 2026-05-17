package com.example.userservice.service

import com.example.common.exception.BusinessException
import com.example.userservice.dto.UserCreateRequest
import com.example.userservice.dto.UserDto
import com.example.userservice.dto.UserListResponse
import com.example.userservice.dto.UserUpdateRequest
import com.example.userservice.entity.AuditAction
import com.example.userservice.entity.UserEntity
import com.example.userservice.repository.RoleRepository
import com.example.userservice.repository.UserRepository
import com.example.userservice.util.UserAuthUtil
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class AdminService(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userAuthUtil: UserAuthUtil,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(AdminService::class.java)

    fun getAllUsers(
        pageable: Pageable,
        search: String?,
    ): UserListResponse {
        val userPage: Page<UserEntity> =
            if (search.isNullOrBlank()) {
                userRepository.findAll(pageable)
            } else {
                userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(
                    search,
                    search,
                    pageable,
                )
            }

        val users = userPage.content.map { it.toUserDto() }

        return UserListResponse(
            users = users,
            totalItems = userPage.totalElements,
            totalPages = userPage.totalPages,
            currentPage = pageable.pageNumber,
        )
    }

    fun getUserById(userId: UUID): UserDto {
        val user = findUserById(userId)
        return user.toUserDto()
    }

    @Transactional
    fun createUser(request: UserCreateRequest): UserDto {
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException("Email ${request.email} is already registered")
        }

        val role =
            roleRepository.findByRoleId(request.roleId)
                ?: throw BusinessException("Invalid role ID: ${request.roleId}")

        val newUser =
            UserEntity(
                email = request.email,
                password = passwordEncoder.encode(request.password)!!,
                fullName = request.fullName,
                profilePicture = request.profilePicture,
                currentLevel = request.currentLevel,
                jlptGoal = request.jlptGoal,
                isActive = true,
                isEmailVerified = true,
                lastLogin = null,
                role = role,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val savedUser = userRepository.save(newUser)
        val actorId = userAuthUtil.getCurrentUserId()
        auditService.log(
            AuditAction.USER_CREATED,
            userId = savedUser.userId,
            actorId = actorId,
            targetType = "user",
            targetId = savedUser.userId?.toString(),
        )
        logger.info("User created by admin: ${savedUser.email}")

        return savedUser.toUserDto()
    }

    @Transactional
    fun updateUser(
        userId: UUID,
        request: UserUpdateRequest,
    ): UserDto {
        val existingUser = findUserById(userId)

        if (request.email != null &&
            request.email != existingUser.email &&
            userRepository.existsByEmail(request.email)
        ) {
            throw BusinessException("Email ${request.email} is already registered")
        }

        val role =
            if (request.roleId != null) {
                roleRepository.findByRoleId(request.roleId)
                    ?: throw BusinessException("Invalid role ID: ${request.roleId}")
            } else {
                existingUser.role
            }

        val reminderTime =
            if (request.reminderTime != null) {
                try {
                    LocalTime.parse(request.reminderTime, DateTimeFormatter.ofPattern("HH:mm"))
                } catch (e: Exception) {
                    throw BusinessException("Invalid reminder time format. Use HH:mm")
                }
            } else {
                existingUser.reminderTime
            }

        val updatedUser =
            existingUser.copy(
                email = request.email ?: existingUser.email,
                password =
                    if (request.password != null) {
                        passwordEncoder.encode(request.password)!!
                    } else {
                        existingUser.password
                    },
                fullName = request.fullName ?: existingUser.fullName,
                profilePicture = request.profilePicture ?: existingUser.profilePicture,
                currentLevel = request.currentLevel ?: existingUser.currentLevel,
                jlptGoal = request.jlptGoal ?: existingUser.jlptGoal,
                isActive = request.isActive ?: existingUser.isActive,
                isEmailVerified = request.isEmailVerified ?: existingUser.isEmailVerified,
                reminderEnabled = request.reminderEnabled ?: existingUser.reminderEnabled,
                reminderTime = reminderTime,
                notificationPreferences =
                    request.notificationPreferences
                        ?.split(",")
                        ?.map { it.trim() }
                        ?.toMutableSet()
                        ?: existingUser.notificationPreferences,
                minCardThreshold = request.minCardThreshold ?: existingUser.minCardThreshold,
                role = role,
                updatedAt = LocalDateTime.now(),
            )

        val savedUser = userRepository.save(updatedUser)
        val actorId = userAuthUtil.getCurrentUserId()
        auditService.log(
            AuditAction.USER_UPDATED,
            userId = userId,
            actorId = actorId,
            targetType = "user",
            targetId = userId.toString(),
        )
        logger.info("User updated by admin: ${savedUser.email}")

        return savedUser.toUserDto()
    }

    @Transactional
    fun deactivateUser(userId: UUID) {
        val user = findUserById(userId)

        if (user.role.roleId == 1) {
            val adminCount = userRepository.countByRoleRoleIdAndIsActive(1, true)
            if (adminCount <= 1) {
                throw BusinessException("Cannot deactivate the last active admin user")
            }
        }

        userRepository.save(user.copy(isActive = false, updatedAt = LocalDateTime.now()))
        val actorId = userAuthUtil.getCurrentUserId()
        auditService.log(
            AuditAction.USER_DEACTIVATED,
            userId = userId,
            actorId = actorId,
            targetType = "user",
            targetId = userId.toString(),
        )
        logger.info("User deactivated by admin: ${user.email}")
    }

    @Transactional
    fun activateUser(userId: UUID) {
        val user = findUserById(userId)
        userRepository.save(user.copy(isActive = true, updatedAt = LocalDateTime.now()))
        val actorId = userAuthUtil.getCurrentUserId()
        auditService.log(
            AuditAction.USER_ACTIVATED,
            userId = userId,
            actorId = actorId,
            targetType = "user",
            targetId = userId.toString(),
        )
        logger.info("User activated by admin: ${user.email}")
    }

    @Transactional
    fun changeUserRole(
        userId: UUID,
        roleId: Int,
    ) {
        val user = findUserById(userId)

        val newRole =
            roleRepository.findByRoleId(roleId)
                ?: throw BusinessException("Invalid role ID: $roleId")

        if (user.role.roleId == 1 && roleId != 1) {
            val adminCount = userRepository.countByRoleRoleIdAndIsActive(1, true)
            if (adminCount <= 1) {
                throw BusinessException("Cannot change role of the last active admin user")
            }
        }

        userRepository.save(user.copy(role = newRole, updatedAt = LocalDateTime.now()))
        val actorId = userAuthUtil.getCurrentUserId()
        auditService.log(
            AuditAction.ROLE_CHANGED,
            userId = userId,
            actorId = actorId,
            targetType = "user",
            targetId = userId.toString(),
            details = "oldRole=${user.role.roleName} newRole=${newRole.roleName}",
        )
        logger.info("User role changed by admin: ${user.email}, new role: ${newRole.roleName}")
    }

    private fun findUserById(userId: UUID): UserEntity =
        userRepository.findById(userId).orElseThrow { BusinessException("User not found with ID: $userId") }

    private fun UserEntity.toUserDto(): UserDto =
        UserDto(
            userId = this.userId ?: throw BusinessException("User ID is null"),
            email = this.email,
            fullName = this.fullName,
            roleId = this.role.roleId,
            profilePicture = this.profilePicture,
            currentLevel = this.currentLevel,
            jlptGoal = this.jlptGoal,
            lastLogin = this.lastLogin,
            isActive = this.isActive,
        )
}
