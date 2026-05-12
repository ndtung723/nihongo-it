package com.example.learningservice.controller.admin

import com.example.learningservice.dto.CreateTopicRequest
import com.example.learningservice.dto.TopicDTO
import com.example.learningservice.dto.UpdateTopicRequest
import com.example.learningservice.service.TopicService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/v1/learning/admin/topics")
@Tag(name = "Admin Topic Management", description = "API endpoints for admin to manage topics")
@PreAuthorize("hasRole('ADMIN')")
class AdminTopicController(
    private val topicService: TopicService,
) {

    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all topics",
        description = "Retrieves a list of all topics in the system",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getAllTopics(): ResponseEntity<List<TopicDTO>> {
        return ResponseEntity.ok(topicService.getAllTopics())
    }

    @GetMapping("/category/{categoryId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get topics by category ID",
        description = "Retrieves all topics belonging to a specific category",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getTopicsByCategoryId(@PathVariable categoryId: UUID): ResponseEntity<List<TopicDTO>> {
        return ResponseEntity.ok(topicService.getTopicsByCategoryId(categoryId))
    }

    @GetMapping("/{topicId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get topic by ID",
        description = "Retrieves a single topic by its unique identifier",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun getTopicById(@PathVariable topicId: UUID): ResponseEntity<TopicDTO> {
        return ResponseEntity.ok(topicService.getTopicById(topicId))
    }

    @PostMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Create new topic",
        description = "Creates a new topic in the system",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun createTopic(@RequestBody request: CreateTopicRequest): ResponseEntity<TopicDTO> {
        val createdTopic = topicService.createTopic(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTopic)
    }

    @PutMapping("/{topicId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Update topic",
        description = "Updates an existing topic's information",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun updateTopic(
        @PathVariable topicId: UUID,
        @RequestBody request: UpdateTopicRequest,
    ): ResponseEntity<TopicDTO> {
        val updatedTopic = topicService.updateTopic(topicId, request)
        return ResponseEntity.ok(updatedTopic)
    }

    @DeleteMapping("/{topicId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Delete topic",
        description = "Deletes a topic from the system",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun deleteTopic(@PathVariable topicId: UUID): ResponseEntity<Void> {
        topicService.deleteTopic(topicId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{topicId}/toggle-status", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Toggle topic status",
        description = "Activates or deactivates a topic",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun toggleTopicStatus(@PathVariable topicId: UUID): ResponseEntity<TopicDTO> {
        val updatedTopic = topicService.toggleTopicStatus(topicId)
        return ResponseEntity.ok(updatedTopic)
    }

    @GetMapping("/search", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Search topics in category",
        description = "Searches for topics in a specific category matching the provided query for name or meaning",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun searchTopicsInCategory(
        @RequestParam categoryId: UUID,
        @RequestParam query: String,
    ): ResponseEntity<List<TopicDTO>> {
        return ResponseEntity.ok(topicService.searchTopicsInCategory(categoryId, query))
    }
}
