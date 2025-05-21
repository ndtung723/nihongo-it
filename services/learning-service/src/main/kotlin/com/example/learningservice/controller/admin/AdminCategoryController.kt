package com.example.learningservice.controller.admin

import com.example.learningservice.dto.CategoryDTO
import com.example.learningservice.dto.CreateCategoryRequest
import com.example.learningservice.dto.ResponseDto
import com.example.learningservice.dto.ResponseType
import com.example.learningservice.dto.UpdateCategoryRequest
import com.example.learningservice.security.PreAuthFilter
import com.example.learningservice.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/learning-service-api/v1/admin/categories")
@Tag(name = "Admin Category Management", description = "API endpoints for admin to manage categories")
@PreAuthFilter(hasRole = "admin")
class AdminCategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all categories",
        description = "Retrieves a list of all categories in the system",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getAllCategories(): ResponseEntity<List<CategoryDTO>> {
        return ResponseEntity.ok(categoryService.getAllCategories())
    }

    @GetMapping("/{categoryId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get category by ID",
        description = "Retrieves a single category by its unique identifier",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getCategoryById(@PathVariable categoryId: UUID): ResponseEntity<CategoryDTO> {
        return ResponseEntity.ok(categoryService.getCategoryById(categoryId))
    }

    @PostMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Create new category",
        description = "Creates a new category in the system",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun createCategory(@RequestBody request: CreateCategoryRequest): ResponseEntity<CategoryDTO> {
        val createdCategory = categoryService.createCategory(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory)
    }

    @PutMapping("/{categoryId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Update category",
        description = "Updates an existing category's information",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun updateCategory(
        @PathVariable categoryId: UUID,
        @RequestBody request: UpdateCategoryRequest
    ): ResponseEntity<CategoryDTO> {
        val updatedCategory = categoryService.updateCategory(categoryId, request)
        return ResponseEntity.ok(updatedCategory)
    }

    @DeleteMapping("/{categoryId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Delete category",
        description = "Deletes a category from the system",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun deleteCategory(@PathVariable categoryId: UUID): ResponseEntity<ResponseDto> {
        categoryService.deleteCategory(categoryId)
        return ResponseEntity.ok(
            ResponseDto(
                status = ResponseType.OK,
                message = "Category deleted successfully"
            )
        )
    }

    @PatchMapping("/{categoryId}/toggle-status", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Toggle category status",
        description = "Activates or deactivates a category",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun toggleCategoryStatus(@PathVariable categoryId: UUID): ResponseEntity<CategoryDTO> {
        val updatedCategory = categoryService.toggleCategoryStatus(categoryId)
        return ResponseEntity.ok(updatedCategory)
    }

    @GetMapping("/search", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Search categories",
        description = "Searches for categories matching the provided query for name or meaning",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun searchCategories(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) meaningQuery: String?
    ): ResponseEntity<List<CategoryDTO>> {
        return ResponseEntity.ok(categoryService.searchCategoriesWithMeaning(query, meaningQuery))
    }
} 