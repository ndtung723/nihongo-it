package com.example.learningservice.controller

import com.example.learningservice.dto.ConversationDTO
import com.example.learningservice.dto.PagedResponse
import com.example.learningservice.service.ConversationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/learning/conversations")
@Tag(name = "Conversation", description = "API endpoints for users to access conversations")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')") // Yêu cầu ít nhất quyền user
class ConversationController(
    private val conversationService: ConversationService,
) {
    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all conversations with pagination",
        description = "Retrieves a paginated list of all conversations available for learning",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getAllConversations(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "title") sortBy: String,
        @RequestParam(defaultValue = "asc") sortDir: String,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<PagedResponse<ConversationDTO>> {
        val safePage = maxOf(0, page)
        val safeSize = size.coerceIn(1, 100)
        val direction = if (sortDir.equals("desc", ignoreCase = true)) Sort.Direction.DESC else Sort.Direction.ASC
        val pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, sortBy))

        val result =
            if (search != null && search.isNotBlank()) {
                conversationService.searchConversations(search, pageable)
            } else {
                conversationService.getAllConversations(pageable)
            }

        return ResponseEntity.ok(result)
    }

    @GetMapping("/{conversationId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get conversation by ID",
        description = "Retrieves a single conversation by its unique identifier",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getConversationById(
        @PathVariable conversationId: UUID,
    ): ResponseEntity<ConversationDTO> = ResponseEntity.ok(conversationService.getConversationById(conversationId))

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
        val pageable = PageRequest.of(maxOf(0, page), size.coerceIn(1, 100))
        return ResponseEntity.ok(conversationService.getConversationsByJlptLevel(level, pageable))
    }
}
