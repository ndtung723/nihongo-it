package com.example.userservice.controller.admin

import com.example.userservice.dto.UserCreateRequest
import com.example.userservice.dto.UserDto
import com.example.userservice.dto.UserListResponse
import com.example.userservice.dto.UserUpdateRequest
import com.example.userservice.service.AdminService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/user/admin/users")
@Tag(name = "Admin User Management", description = "API endpoints for admin to manage user accounts")
@PreAuthorize("hasRole('ADMIN')")
class AdminController(
    private val adminService: AdminService,
) {

    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all users with pagination",
        description = "Retrieves a paginated list of all users in the system",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getAllUsers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) search: String?,
        @RequestParam(defaultValue = "email") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
    ): ResponseEntity<UserListResponse> {
        val direction = if (sortDir.equals("desc", ignoreCase = true)) {
            Sort.Direction.DESC
        } else {
            Sort.Direction.ASC
        }
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
        val result = adminService.getAllUsers(pageable, search)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{userId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get user by ID",
        description = "Retrieves a single user by their unique identifier",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getUserById(@PathVariable userId: UUID): ResponseEntity<UserDto> {
        val user = adminService.getUserById(userId)
        return ResponseEntity.ok(user)
    }

    @PostMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Create new user",
        description = "Creates a new user account in the system",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun createUser(@RequestBody request: UserCreateRequest): ResponseEntity<UserDto> {
        val createdUser = adminService.createUser(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser)
    }

    @PutMapping("/{userId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Update user",
        description = "Updates an existing user's information",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun updateUser(
        @PathVariable userId: UUID,
        @RequestBody request: UserUpdateRequest,
    ): ResponseEntity<UserDto> {
        val updatedUser = adminService.updateUser(userId, request)
        return ResponseEntity.ok(updatedUser)
    }

    @DeleteMapping("/{userId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Delete user",
        description = "Deactivates a user account (soft delete)",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun deleteUser(@PathVariable userId: UUID): ResponseEntity<Void> {
        adminService.deactivateUser(userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{userId}/activate", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Activate user",
        description = "Activates a deactivated user account",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun activateUser(@PathVariable userId: UUID): ResponseEntity<Void> {
        adminService.activateUser(userId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{userId}/change-role", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Change user role",
        description = "Changes a user's role (admin/user)",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun changeUserRole(
        @PathVariable userId: UUID,
        @RequestParam roleId: Int,
    ): ResponseEntity<Void> {
        adminService.changeUserRole(userId, roleId)
        return ResponseEntity.noContent().build()
    }
}
