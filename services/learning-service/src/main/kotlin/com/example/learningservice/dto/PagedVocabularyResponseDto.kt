package com.example.learningservice.dto

// Paginated response
data class PagedVocabularyResponseDto(
    val content: List<VocabularyDto>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val lastPage: Boolean,
)
