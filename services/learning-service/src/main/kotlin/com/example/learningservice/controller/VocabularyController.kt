package com.example.learningservice.controller

import com.example.learningservice.dto.CategoryDTO
import com.example.learningservice.dto.CreateVocabularyRequestDto
import com.example.learningservice.dto.CreateVocabularyResponseDto
import com.example.learningservice.dto.GetVocabularyResponseDto
import com.example.learningservice.dto.PagedVocabularyResponseDto
import com.example.learningservice.dto.TopicDTO
import com.example.learningservice.dto.UpdateVocabularyRequestDto
import com.example.learningservice.dto.UpdateVocabularyResponseDto
import com.example.learningservice.dto.VocabularyDto
import com.example.learningservice.dto.VocabularyFilterRequestDto
import com.example.learningservice.entity.JlptLevel
import com.example.learningservice.service.CategoryService
import com.example.learningservice.service.VocabularyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/learning/vocabulary")
@Tag(name = "Vocabulary", description = "API endpoints for managing Japanese IT vocabulary, including CRUD operations and user notebook")
class VocabularyController(
    private val vocabularyService: VocabularyService,
    private val categoryService: CategoryService,
) {
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create new vocabulary",
        description = "Creates a new vocabulary entry with provided details",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Vocabulary created successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = CreateVocabularyResponseDto::class))],
            ),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        ],
    )
    fun createVocabulary(
        @Parameter(description = "Vocabulary creation details", required = true)
        @Valid
        @RequestBody request: CreateVocabularyRequestDto,
    ): CreateVocabularyResponseDto = vocabularyService.createVocabulary(request)

    @GetMapping("/{vocabId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get vocabulary by ID",
        description = "Retrieves a specific vocabulary entry by its ID",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved vocabulary",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetVocabularyResponseDto::class))],
            ),
            ApiResponse(responseCode = "404", description = "Vocabulary not found"),
        ],
    )
    fun getVocabulary(
        @Parameter(description = "Unique identifier of the vocabulary entry", required = true)
        @PathVariable vocabId: UUID,
    ): GetVocabularyResponseDto = vocabularyService.getVocabularybyId(vocabId)

    @GetMapping("/term/{term}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get vocabulary by term",
        description = "Retrieves a specific vocabulary entry by its Japanese term",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved vocabulary",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = GetVocabularyResponseDto::class))],
            ),
            ApiResponse(responseCode = "404", description = "Vocabulary not found"),
        ],
    )
    fun getVocabularyByTerm(
        @Parameter(description = "Japanese term of the vocabulary entry", required = true)
        @PathVariable term: String,
    ): GetVocabularyResponseDto = vocabularyService.getVocabularyByTerm(term)

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
        summary = "Filter vocabulary",
        description = "Filters vocabulary entries based on various criteria with pagination support",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved filtered vocabulary list",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = PagedVocabularyResponseDto::class))],
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        ],
    )
    fun filterVocabulary(
        @Parameter(description = "Filter by JLPT level")
        @RequestParam(required = false) jlptLevel: JlptLevel?,
        @Parameter(description = "Filter by topic name")
        @RequestParam(required = false) topicName: String?,
        @Parameter(description = "Search by keyword in hiragana, kanji, or meaning")
        @RequestParam(required = false) keyword: String?,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort option")
        @RequestParam(required = false) sort: String?,
    ): PagedVocabularyResponseDto {
        // Validate page and size parameters to prevent invalid values
        val validPage = if (page < 0) 0 else page
        val validSize =
            if (size <= 0) {
                20
            } else if (size > 100) {
                100
            } else {
                size
            }

        val filter =
            VocabularyFilterRequestDto(
                jlptLevel = jlptLevel,
                topicName = topicName,
                keyword = keyword,
                page = validPage,
                size = validSize,
                sort = sort,
            )
        return vocabularyService.filterVocabulary(filter)
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PutMapping("/{vocabId}", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Update vocabulary",
        description = "Updates an existing vocabulary entry with the provided details",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vocabulary updated successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = UpdateVocabularyResponseDto::class))],
            ),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            ApiResponse(responseCode = "404", description = "Vocabulary not found"),
        ],
    )
    fun updateVocabulary(
        @Parameter(description = "Unique identifier of the vocabulary to update", required = true)
        @PathVariable vocabId: UUID,
        @Parameter(description = "Updated vocabulary details", required = true)
        @Valid
        @RequestBody request: UpdateVocabularyRequestDto,
    ): UpdateVocabularyResponseDto = vocabularyService.updateVocabulary(vocabId, request)

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{vocabId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Delete vocabulary",
        description = "Deletes a vocabulary entry by its ID",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vocabulary deleted successfully",
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            ApiResponse(responseCode = "404", description = "Vocabulary not found"),
        ],
    )
    fun deleteVocabulary(
        @Parameter(description = "Unique identifier of the vocabulary to delete", required = true)
        @PathVariable vocabId: UUID,
    ): ResponseEntity<Void> {
        vocabularyService.deleteVocabulary(vocabId)
        return ResponseEntity.noContent().build()
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{vocabId}/save", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Save vocabulary to user's notebook by ID",
        description = "Adds a vocabulary entry to the current user's personal notebook using ID (legacy method)",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vocabulary saved to notebook successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = VocabularyDto::class))],
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            ApiResponse(responseCode = "404", description = "Vocabulary not found"),
            ApiResponse(responseCode = "409", description = "Vocabulary already saved to notebook"),
        ],
    )
    fun saveVocabularyToNotebook(
        @Parameter(description = "Unique identifier of the vocabulary to save", required = true)
        @PathVariable vocabId: UUID,
    ): VocabularyDto {
        val result = vocabularyService.saveVocabularyToNotebook(vocabId)
        return result
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @DeleteMapping("/{vocabId}/save", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Remove vocabulary from user's notebook",
        description = "Removes a vocabulary entry from the current user's personal notebook",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Vocabulary removed from notebook successfully",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = VocabularyDto::class))],
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            ApiResponse(responseCode = "404", description = "Vocabulary not found or not in notebook"),
        ],
    )
    fun removeVocabularyFromNotebook(
        @Parameter(description = "Unique identifier of the vocabulary to remove from notebook", required = true)
        @PathVariable vocabId: UUID,
    ): VocabularyDto {
        val result = vocabularyService.removeVocabularyFromNotebook(vocabId)
        return result
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/saved", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get user's saved vocabulary",
        description = "Retrieves all vocabulary entries saved to the current user's notebook with pagination",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved saved vocabulary",
                content = [Content(mediaType = "application/json", schema = Schema(implementation = PagedVocabularyResponseDto::class))],
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        ],
    )
    fun getSavedVocabulary(
        @Parameter(description = "Search by keyword in hiragana, kanji, or meaning")
        @RequestParam(required = false) keyword: String?,
        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int,
        @Parameter(description = "Sort option")
        @RequestParam(defaultValue = "date_desc")
        sort: String?,
    ): PagedVocabularyResponseDto {
        // Validate page and size parameters to prevent invalid values
        val validPage = if (page < 0) 0 else page
        val validSize =
            if (size <= 0) {
                20
            } else if (size > 100) {
                100
            } else {
                size
            }

        val filter =
            VocabularyFilterRequestDto(
                keyword = keyword,
                page = validPage,
                size = validSize,
            )
        val result = vocabularyService.getSavedVocabulary(filter)
        return result
    }

    @GetMapping("/topics", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all topics for vocabulary",
        description = "Returns a list of all available topics for organizing vocabulary entries",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved topics list",
                content = [Content(mediaType = "application/json")],
            ),
        ],
    )
    fun getAllTopics(): ResponseEntity<List<TopicDTO>> {
        // Get all topics regardless of category
        // This is the primary organization structure for vocabulary items
        return ResponseEntity.ok(categoryService.getAllTopics())
    }

    @GetMapping("/categories", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get available vocabulary categories",
        description = "Returns a list of all available categories for vocabulary entries",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved categories list",
                content = [Content(mediaType = "application/json")],
            ),
        ],
    )
    fun getCategories(): List<CategoryDTO> {
        // Get categories from the database
        return categoryService.getAllCategories()
    }

    @GetMapping("/jlpt-levels", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get available JLPT levels",
        description = "Returns a list of all available JLPT levels for vocabulary entries",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved JLPT levels list",
                content = [Content(mediaType = "application/json")],
            ),
        ],
    )
    fun getJlptLevels(): List<JlptLevel> {
        val levels = JlptLevel.entries
        return levels
    }

    @GetMapping("/categories/{categoryId}/topics", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get topics for a category",
        description = "Returns all topics belonging to a specific category",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved topics list",
                content = [Content(mediaType = "application/json")],
            ),
            ApiResponse(responseCode = "404", description = "Category not found"),
        ],
    )
    fun getTopicsByCategory(
        @Parameter(description = "Category ID", required = true)
        @PathVariable categoryId: UUID,
    ): ResponseEntity<List<TopicDTO>> = ResponseEntity.ok(categoryService.getTopicsForCategory(categoryId))
}
