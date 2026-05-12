package com.example.learningservice.controller.admin

import com.example.learningservice.dto.ConversationDTO
import com.example.learningservice.dto.CreateConversationRequest
import com.example.learningservice.dto.PagedResponse
import com.example.learningservice.dto.UpdateConversationRequest
import com.example.learningservice.service.ConversationService
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
@RequestMapping("/api/v1/learning/admin/conversations")
@Tag(name = "Admin Conversation Management", description = "API endpoints for admin to manage conversations")
@PreAuthorize("hasRole('ADMIN')")
class AdminConversationController(
    private val conversationService: ConversationService,
) {

    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all conversations with pagination",
        description = "Retrieves a paginated list of all conversations in the system",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getAllConversations(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "title") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<PagedResponse<ConversationDTO>> {
        val direction = if (sortDir.equals("desc", ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))

        val result = if (search != null && search.isNotBlank()) {
            conversationService.searchConversations(search, pageable)
        } else {
            conversationService.getAllConversations(pageable)
        }

        return ResponseEntity.ok(result)
    }

    @GetMapping("/jlpt/{level}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get conversations by JLPT level",
        description = "Retrieves all conversations for a specific JLPT level",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getConversationsByJlptLevel(
        @PathVariable level: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<PagedResponse<ConversationDTO>> {
        val pageable = PageRequest.of(page, size)
        return ResponseEntity.ok(conversationService.getConversationsByJlptLevel(level, pageable))
    }

    @GetMapping("/{conversationId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get conversation by ID",
        description = "Retrieves a single conversation by its unique identifier",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getConversationById(@PathVariable conversationId: UUID): ResponseEntity<ConversationDTO> {
        return ResponseEntity.ok(conversationService.getConversationById(conversationId))
    }

    @PostMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Create new conversation",
        description = "Creates a new conversation in the system",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun createConversation(@RequestBody request: CreateConversationRequest): ResponseEntity<ConversationDTO> {
        val createdConversation = conversationService.createConversation(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdConversation)
    }

    @PutMapping("/{conversationId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Update conversation",
        description = "Updates an existing conversation's information",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun updateConversation(
        @PathVariable conversationId: UUID,
        @RequestBody request: UpdateConversationRequest,
    ): ResponseEntity<ConversationDTO> {
        val updatedConversation = conversationService.updateConversation(conversationId, request)
        return ResponseEntity.ok(updatedConversation)
    }

    @DeleteMapping("/{conversationId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Delete conversation",
        description = "Deletes a conversation from the system",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun deleteConversation(@PathVariable conversationId: UUID): ResponseEntity<Void> {
        conversationService.deleteConversation(conversationId)
        return ResponseEntity.noContent().build()
    }
}
