package com.example.learningservice.controller

import com.example.learningservice.dto.CreateTopicRequest
import com.example.learningservice.dto.TopicDTO
import com.example.learningservice.dto.UpdateTopicRequest
import com.example.learningservice.service.TopicService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/learning/topics")
@Tag(name = "Topics", description = "API endpoints for managing Japanese IT vocabulary topics")
class TopicController(
    private val topicService: TopicService,
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get all topics",
        description = "Returns a list of all available topics",
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
    fun getAllTopics(): ResponseEntity<List<TopicDTO>> = ResponseEntity.ok(topicService.getAllTopics())

    @GetMapping("/{topicId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Get topic by ID",
        description = "Returns a specific topic by its ID",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Successfully retrieved topic",
                content = [Content(mediaType = "application/json")],
            ),
            ApiResponse(responseCode = "404", description = "Topic not found"),
        ],
    )
    fun getTopicById(
        @Parameter(description = "Topic ID", required = true)
        @PathVariable topicId: UUID,
    ): ResponseEntity<TopicDTO> = ResponseEntity.ok(topicService.getTopicById(topicId))

    @PostMapping(produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create new topic",
        description = "Creates a new topic with the provided details",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Topic created successfully",
                content = [Content(mediaType = "application/json")],
            ),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
        ],
    )
    fun createTopic(
        @RequestBody request: CreateTopicRequest,
    ): ResponseEntity<TopicDTO> = ResponseEntity.status(HttpStatus.CREATED).body(topicService.createTopic(request))

    @PutMapping("/{topicId}", produces = [MediaType.APPLICATION_JSON_VALUE], consumes = [MediaType.APPLICATION_JSON_VALUE])
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update topic",
        description = "Updates an existing topic with the provided details",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Topic updated successfully",
                content = [Content(mediaType = "application/json")],
            ),
            ApiResponse(responseCode = "400", description = "Invalid request data"),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            ApiResponse(responseCode = "404", description = "Topic not found"),
        ],
    )
    fun updateTopic(
        @Parameter(description = "Topic ID", required = true)
        @PathVariable topicId: UUID,
        @RequestBody request: UpdateTopicRequest,
    ): ResponseEntity<TopicDTO> = ResponseEntity.ok(topicService.updateTopic(topicId, request))

    @DeleteMapping("/{topicId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete topic",
        description = "Deletes a topic by its ID",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "Topic deleted successfully",
            ),
            ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            ApiResponse(responseCode = "404", description = "Topic not found"),
        ],
    )
    fun deleteTopic(
        @Parameter(description = "Topic ID", required = true)
        @PathVariable topicId: UUID,
    ): ResponseEntity<Unit> {
        topicService.deleteTopic(topicId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/search", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Search topics",
        description = "Search topics within a category",
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
    fun searchTopics(
        @Parameter(description = "Category ID", required = true)
        @RequestParam categoryId: UUID,
        @Parameter(description = "Search query", required = true)
        @RequestParam query: String,
    ): ResponseEntity<List<TopicDTO>> = ResponseEntity.ok(topicService.searchTopicsInCategory(categoryId, query))
}
