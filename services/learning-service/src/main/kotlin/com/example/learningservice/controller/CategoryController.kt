package com.example.learningservice.controller

import com.example.learningservice.dto.CategoryDTO
import com.example.learningservice.dto.CreateCategoryRequest
import com.example.learningservice.dto.TopicDTO
import com.example.learningservice.dto.UpdateCategoryRequest
import com.example.learningservice.service.CategoryService
import com.example.learningservice.service.TopicService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/learning/categories")
class CategoryController(
    private val categoryService: CategoryService,
    private val topicService: TopicService,
) {
    @GetMapping
    fun getAllCategories(): ResponseEntity<List<CategoryDTO>> {
        return ResponseEntity.ok(categoryService.getAllCategories())
    }

    @GetMapping("/{categoryId}")
    fun getCategoryById(@PathVariable categoryId: UUID): ResponseEntity<CategoryDTO> {
        return ResponseEntity.ok(categoryService.getCategoryById(categoryId))
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createCategory(@RequestBody request: CreateCategoryRequest): ResponseEntity<CategoryDTO> {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(request))
    }

    @PutMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateCategory(
        @PathVariable categoryId: UUID,
        @RequestBody request: UpdateCategoryRequest,
    ): ResponseEntity<CategoryDTO> {
        return ResponseEntity.ok(categoryService.updateCategory(categoryId, request))
    }

    @DeleteMapping("/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteCategory(@PathVariable categoryId: UUID): ResponseEntity<Unit> {
        categoryService.deleteCategory(categoryId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/search")
    fun searchCategories(@RequestParam query: String): ResponseEntity<List<CategoryDTO>> {
        return ResponseEntity.ok(categoryService.searchCategories(query))
    }

    @GetMapping("/{categoryId}/topics")
    fun getTopicsByCategory(@PathVariable categoryId: UUID): ResponseEntity<List<TopicDTO>> {
        return ResponseEntity.ok(topicService.getTopicsByCategoryId(categoryId))
    }
}
