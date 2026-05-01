package com.example.learningservice.controller.admin

import com.example.learningservice.dto.CreateVocabularyRequestDto
import com.example.learningservice.dto.CreateVocabularyResponseDto
import com.example.learningservice.dto.GetVocabularyResponseDto
import com.example.learningservice.dto.PagedVocabularyResponseDto
import com.example.common.dto.ResponseDto
import com.example.learningservice.dto.UpdateVocabularyRequestDto
import com.example.learningservice.dto.UpdateVocabularyResponseDto
import com.example.learningservice.dto.VocabularyFilterRequestDto
import com.example.learningservice.entity.JlptLevel
import com.example.common.security.PreAuthFilter
import com.example.learningservice.service.VocabularyService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/learning/admin/vocabulary")
@Tag(name = "Admin Vocabulary Management", description = "API endpoints for admin to manage vocabulary")
@PreAuthFilter(hasRole = "admin")
class AdminVocabularyController(
    private val vocabularyService: VocabularyService
) {

    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all vocabulary with pagination",
        description = "Retrieves a paginated list of all vocabulary in the system",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getAllVocabulary(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) jlptLevel: String?
    ): ResponseEntity<PagedVocabularyResponseDto> {
        val filter = VocabularyFilterRequestDto(
            page = page,
            size = size,
            jlptLevel = jlptLevel?.let { JlptLevel.valueOf(it) }
        )
        return ResponseEntity.ok(vocabularyService.filterVocabulary(filter))
    }

    @GetMapping("/topic/{topicId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get vocabulary by topic ID with pagination",
        description = "Retrieves all vocabulary belonging to a specific topic",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getVocabularyByTopicId(
        @PathVariable topicId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedVocabularyResponseDto> {
        // Implementation needed
        val filter = VocabularyFilterRequestDto(
            topicId = topicId,
            page = page,
            size = size
        )
        return ResponseEntity.ok(vocabularyService.filterVocabularyByTopicId(filter))
    }

    @GetMapping("/{vocabId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get vocabulary by ID",
        description = "Retrieves a single vocabulary item by its unique identifier",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun getVocabularyById(@PathVariable vocabId: UUID): ResponseEntity<GetVocabularyResponseDto> {
        return ResponseEntity.ok(vocabularyService.getVocabularybyId(vocabId))
    }

    @PostMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Create new vocabulary",
        description = "Creates a new vocabulary item in the system",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun createVocabulary(@Valid @RequestBody request: CreateVocabularyRequestDto): ResponseEntity<CreateVocabularyResponseDto> {
        val createdVocabulary = vocabularyService.createVocabulary(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVocabulary)
    }

    @PutMapping("/{vocabId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Update vocabulary",
        description = "Updates an existing vocabulary item's information",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun updateVocabulary(
        @PathVariable vocabId: UUID,
        @Valid @RequestBody request: UpdateVocabularyRequestDto
    ): ResponseEntity<UpdateVocabularyResponseDto> {
        val updatedVocabulary = vocabularyService.updateVocabulary(vocabId, request)
        return ResponseEntity.ok(updatedVocabulary)
    }

    @DeleteMapping("/{vocabId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Delete vocabulary",
        description = "Deletes a vocabulary item from the system",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun deleteVocabulary(@PathVariable vocabId: UUID): ResponseEntity<ResponseDto> {
        val result = vocabularyService.deleteVocabulary(vocabId)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/search", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Search vocabulary",
        description = "Searches for vocabulary matching the provided query in term, pronunciation, or meaning",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    fun searchVocabulary(
        @RequestParam query: String,
        @RequestParam(required = false) topicId: UUID?,
        @RequestParam(required = false) jlptLevel: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PagedVocabularyResponseDto> {
        val filter = VocabularyFilterRequestDto(
            keyword = query,
            topicId = topicId,
            jlptLevel = jlptLevel?.let { JlptLevel.valueOf(it) },
            page = page,
            size = size
        )
        return ResponseEntity.ok(vocabularyService.filterVocabulary(filter))
    }
} 