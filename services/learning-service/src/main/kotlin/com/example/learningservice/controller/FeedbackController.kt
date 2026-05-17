package com.example.learningservice.controller

import com.example.learningservice.dto.FeedbackDTO
import com.example.learningservice.service.FeedbackService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/learning/feedback")
@Tag(name = "Feedback", description = "API endpoint for saving user feedback")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
class FeedbackController(
    private val feedbackService: FeedbackService,
) {
    @PostMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Save user feedback",
        description = "Saves feedback from a user's practice session",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun saveFeedback(
        @RequestBody feedbackDTO: FeedbackDTO,
    ): ResponseEntity<FeedbackDTO> {
        val savedFeedback = feedbackService.saveFeedback(feedbackDTO)
        return ResponseEntity.status(HttpStatus.CREATED).body(savedFeedback)
    }
}
