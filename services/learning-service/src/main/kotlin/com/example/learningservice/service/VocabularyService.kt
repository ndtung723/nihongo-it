package com.example.learningservice.service

import com.example.common.exception.BusinessException
import com.example.common.ext.orThrow
import com.example.learningservice.dto.CreateVocabularyRequestDto
import com.example.learningservice.dto.CreateVocabularyResponseDto
import com.example.learningservice.dto.GetVocabularyResponseDto
import com.example.learningservice.dto.PagedVocabularyResponseDto
import com.example.learningservice.dto.UpdateVocabularyRequestDto
import com.example.learningservice.dto.UpdateVocabularyResponseDto
import com.example.learningservice.dto.VocabularyDto
import com.example.learningservice.dto.VocabularyFilterRequestDto
import com.example.learningservice.entity.JlptLevel
import com.example.learningservice.entity.VocabularyEntity
import com.example.learningservice.repository.FlashcardRepository
import com.example.learningservice.repository.TopicRepository
import com.example.learningservice.repository.UserRepository
import com.example.learningservice.repository.VocabularyRepository
import com.example.learningservice.util.UserAuthUtil
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class VocabularyService(
    private val vocabularyRepository: VocabularyRepository,
    private val userRepository: UserRepository,
    private val topicRepository: TopicRepository,
    private val userAuthUtil: UserAuthUtil,
    private val flashcardCrudService: FlashcardCrudService,
    private val flashcardRepository: FlashcardRepository,
) {
    private val logger = LoggerFactory.getLogger(VocabularyService::class.java)

    @Transactional
    @Suppress("ThrowsCount")
    fun createVocabulary(request: CreateVocabularyRequestDto): CreateVocabularyResponseDto {
        val currentUserId =
            userAuthUtil.getCurrentUserId()
                ?: throw BusinessException("User not authenticated")

        if (request.term.isBlank()) {
            throw BusinessException("Term must be provided")
        }

        if (vocabularyRepository.existsByTerm(request.term)) {
            throw BusinessException("Vocabulary with term '${request.term}' already exists")
        }

        val topics = topicRepository.findByName(request.topicName)
        if (topics.isEmpty()) {
            throw BusinessException("Topic '${request.topicName}' does not exist")
        }
        val topic = topics.first()

        val vocabulary =
            VocabularyEntity(
                term = request.term,
                meaning = request.meaning,
                pronunciation = request.pronunciation,
                example = request.example,
                exampleMeaning = request.exampleMeaning,
                audioPath = request.audioPath,
                jlptLevel = JlptLevel.valueOf(request.jlptLevel),
                topic = topic,
                createdAt = Instant.now(),
            )

        val saved = vocabularyRepository.save(vocabulary)
        return CreateVocabularyResponseDto(
            message = "Vocabulary created successfully",
            data = mapToResponse(saved),
        )
    }

    @Transactional(readOnly = true)
    fun getVocabularybyId(vocabId: UUID): GetVocabularyResponseDto {
        val currentUserId = userAuthUtil.getCurrentUserId()
        val vocabulary = vocabularyRepository.findById(vocabId).orThrow("Vocabulary not found")

        val isSaved =
            currentUserId?.let { userId ->
                vocabulary.savedByUsers.any { it.userId == userId }
            } ?: false

        return GetVocabularyResponseDto(data = mapToResponse(vocabulary, isSaved))
    }

    @Transactional(readOnly = true)
    fun getVocabularyByTerm(term: String): GetVocabularyResponseDto {
        val currentUserId = userAuthUtil.getCurrentUserId()
        val vocabulary = vocabularyRepository.findByTerm(term).orThrow("Vocabulary not found with term: $term")

        val isSaved =
            currentUserId?.let { userId ->
                vocabulary.savedByUsers.any { it.userId == userId }
            } ?: false

        return GetVocabularyResponseDto(data = mapToResponse(vocabulary, isSaved))
    }

    @Transactional
    @Suppress("ThrowsCount")
    fun updateVocabulary(
        vocabId: UUID,
        request: UpdateVocabularyRequestDto,
    ): UpdateVocabularyResponseDto {
        val currentUserId =
            userAuthUtil.getCurrentUserId()
                ?: throw BusinessException("User not authenticated")

        val vocabulary = vocabularyRepository.findById(vocabId).orThrow("Vocabulary not found")

        if (request.term.isBlank()) {
            throw BusinessException("Term cannot be blank")
        }

        if (request.term != vocabulary.term && vocabularyRepository.existsByTerm(request.term)) {
            throw BusinessException("Vocabulary with term '${request.term}' already exists")
        }

        val topic =
            request.topicName?.let { topicName ->
                val topics = topicRepository.findByName(topicName)
                if (topics.isEmpty()) throw BusinessException("Topic '$topicName' does not exist")
                topics.first()
            } ?: vocabulary.topic

        val updatedVocabulary =
            vocabulary.copy(
                term = request.term,
                meaning = request.meaning,
                pronunciation = request.pronunciation,
                example = request.example,
                exampleMeaning = request.exampleMeaning,
                audioPath = request.audioPath,
                jlptLevel = JlptLevel.valueOf(request.jlptLevel),
                topic = topic,
            )

        val savedVocab = vocabularyRepository.save(updatedVocabulary)
        return UpdateVocabularyResponseDto(data = mapToResponse(savedVocab))
    }

    @Transactional(readOnly = true)
    fun filterVocabulary(filter: VocabularyFilterRequestDto): PagedVocabularyResponseDto {
        val pageable = PageRequest.of(filter.page, filter.size)
        val currentUserId = userAuthUtil.getCurrentUserId()
        val keyword = filter.keyword
        val topicName = filter.topicName

        val result: Page<VocabularyEntity> =
            when {
                // Kết hợp tìm kiếm theo cả topicId và jlptLevel nếu cả hai đều được cung cấp
                filter.topicId != null && filter.jlptLevel != null -> {
                    vocabularyRepository.findByTopic_TopicIdAndJlptLevel(filter.topicId, filter.jlptLevel, pageable)
                }
                // Tìm kiếm theo keyword
                keyword != null -> {
                    vocabularyRepository.searchVocabulary(keyword, pageable)
                }
                // Tìm kiếm theo jlptLevel
                filter.jlptLevel != null -> {
                    vocabularyRepository.findByJlptLevel(filter.jlptLevel, pageable)
                }
                // Tìm kiếm theo topicId
                filter.topicId != null -> {
                    vocabularyRepository.findByTopic_TopicId(filter.topicId, pageable)
                }
                // Tìm kiếm theo topicName
                topicName != null -> {
                    vocabularyRepository.findByTopicName(topicName, pageable)
                }
                // Lấy tất cả
                else -> {
                    vocabularyRepository.findAll(pageable)
                }
            }

        val content =
            result.content.map { vocabulary ->
                val isSaved =
                    currentUserId?.let { userId ->
                        vocabulary.savedByUsers.any { it.userId == userId }
                    } ?: false

                mapToResponse(vocabulary, isSaved)
            }

        return PagedVocabularyResponseDto(
            content = content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            lastPage = result.isLast,
        )
    }

    @Transactional
    fun deleteVocabulary(vocabId: UUID) {
        userAuthUtil.getCurrentUserId()
            ?: throw BusinessException("User not authenticated")

        val vocabulary = vocabularyRepository.findById(vocabId).orThrow("Vocabulary not found")

        vocabularyRepository.delete(vocabulary)
    }

    @Transactional
    fun saveVocabularyToNotebook(vocabId: UUID): VocabularyDto {
        val currentUserId =
            userAuthUtil.getCurrentUserId()
                ?: throw BusinessException("User not authenticated")

        val user =
            userRepository
                .findById(currentUserId)
                .orElseThrow { BusinessException("User not found") }

        val vocabulary = vocabularyRepository.findById(vocabId).orThrow("Vocabulary not found")

        vocabulary.savedByUsers.add(user)
        vocabularyRepository.save(vocabulary)

        // Automatically create a flashcard for this vocabulary
        try {
            flashcardCrudService.createFlashcardFromVocabulary(vocabId)
            logger.info("Automatically created flashcard for vocabulary $vocabId when saved to notebook")
        } catch (e: Exception) {
            // Log the error but don't fail the save operation
            // This allows vocabulary to be saved even if flashcard creation fails
            // (e.g., if a flashcard already exists)
            logger.warn("Failed to auto-create flashcard for vocabulary $vocabId: ${e.message}")
        }

        return mapToResponse(vocabulary, true)
    }

    @Transactional
    fun removeVocabularyFromNotebook(vocabId: UUID): VocabularyDto {
        val currentUserId =
            userAuthUtil.getCurrentUserId()
                ?: throw BusinessException("User not authenticated")

        val vocabulary = vocabularyRepository.findById(vocabId).orThrow("Vocabulary not found")

        vocabulary.savedByUsers.removeIf { it.userId == currentUserId }
        vocabularyRepository.save(vocabulary)

        // Delete associated flashcard for this vocabulary
        try {
            // Find the flashcards for this vocabulary and user
            val flashcards = flashcardRepository.findByUser_UserIdAndVocabulary_VocabId(currentUserId, vocabId)

            // Delete each flashcard
            flashcards.forEach { flashcard ->
                flashcardCrudService.deleteFlashcard(requireNotNull(flashcard.flashcardId) { "Flashcard ID missing" })
                logger.info("Deleted flashcard ${flashcard.flashcardId} when vocabulary $vocabId was removed from notebook")
            }
        } catch (e: Exception) {
            // Log the error but don't fail the remove operation
            logger.warn("Failed to delete flashcard for vocabulary $vocabId: ${e.message}")
        }

        return mapToResponse(vocabulary, false)
    }

    @Transactional(readOnly = true)
    fun getSavedVocabulary(filter: VocabularyFilterRequestDto): PagedVocabularyResponseDto {
        val currentUserId =
            userAuthUtil.getCurrentUserId()
                ?: throw BusinessException("User not authenticated")

        // Use pagination parameters from the filter
        val pageable = createPageableWithSort(filter.page, filter.size, filter.sort)
        val keyword = filter.keyword

        // Apply keyword filter if provided
        val result =
            if (keyword != null && keyword.isNotBlank()) {
                vocabularyRepository.findSavedByUserAndKeyword(currentUserId, keyword, pageable)
            } else {
                vocabularyRepository.findSavedByUser(currentUserId, pageable)
            }

        val content =
            result.content.map { vocabulary ->
                mapToResponse(vocabulary, true)
            }

        // Create a response with pagination information
        return PagedVocabularyResponseDto(
            content = content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            lastPage = result.isLast,
        )
    }

    @Transactional(readOnly = true)
    fun filterVocabularyByTopicId(filter: VocabularyFilterRequestDto): PagedVocabularyResponseDto {
        val pageable = PageRequest.of(filter.page, filter.size)
        val currentUserId = userAuthUtil.getCurrentUserId()

        if (filter.topicId == null) {
            throw BusinessException("Topic ID is required")
        }

        // Kiểm tra topic tồn tại
        val topic =
            topicRepository
                .findById(filter.topicId)
                .orElseThrow { BusinessException("Topic not found with ID: ${filter.topicId}") }

        // Tìm vocabulary theo topic ID kết hợp với keyword nếu có
        val keyword = filter.keyword
        val result =
            if (keyword != null) {
                vocabularyRepository.findByTopicIdAndKeyword(filter.topicId, keyword, pageable)
            } else {
                vocabularyRepository.findByTopic_TopicId(filter.topicId, pageable)
            }

        val content =
            result.content.map { vocabulary ->
                val isSaved =
                    currentUserId?.let { userId ->
                        vocabulary.savedByUsers.any { it.userId == userId }
                    } ?: false

                mapToResponse(vocabulary, isSaved)
            }

        return PagedVocabularyResponseDto(
            content = content,
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            lastPage = result.isLast,
        )
    }

    /**
     * Creates a PageRequest with sorting based on the sort parameter
     */
    private fun createPageableWithSort(
        page: Int,
        size: Int,
        sort: String?,
    ): PageRequest =
        when (sort) {
            "date_asc" ->
                PageRequest.of(
                    page,
                    size,
                    org.springframework.data.domain.Sort
                        .by("createdAt")
                        .ascending(),
                )
            "date_desc" ->
                PageRequest.of(
                    page,
                    size,
                    org.springframework.data.domain.Sort
                        .by("createdAt")
                        .descending(),
                )
            "jlpt_asc" ->
                PageRequest.of(
                    page,
                    size,
                    org.springframework.data.domain.Sort
                        .by("jlptLevel")
                        .ascending(),
                )
            "jlpt_desc" ->
                PageRequest.of(
                    page,
                    size,
                    org.springframework.data.domain.Sort
                        .by("jlptLevel")
                        .descending(),
                )
            "alpha_asc" ->
                PageRequest.of(
                    page,
                    size,
                    org.springframework.data.domain.Sort
                        .by("term")
                        .ascending(),
                )
            "alpha_desc" ->
                PageRequest.of(
                    page,
                    size,
                    org.springframework.data.domain.Sort
                        .by("term")
                        .descending(),
                )
            else ->
                PageRequest.of(
                    page,
                    size,
                    org.springframework.data.domain.Sort
                        .by("createdAt")
                        .descending(),
                )
        }

    private fun mapToResponse(
        vocabulary: VocabularyEntity,
        isSaved: Boolean = false,
    ): VocabularyDto =
        VocabularyDto(
            vocabId = requireNotNull(vocabulary.vocabId) { "Vocabulary ID missing" },
            term = vocabulary.term.orEmpty(),
            meaning = vocabulary.meaning,
            pronunciation = vocabulary.pronunciation,
            example = vocabulary.example,
            exampleMeaning = vocabulary.exampleMeaning,
            audioPath = vocabulary.audioPath,
            jlptLevel = vocabulary.jlptLevel,
            topicId = vocabulary.topic.topicId,
            topicName = vocabulary.topic.name,
            createdAt = vocabulary.createdAt,
            isSaved = isSaved,
        )

    /**
     * Get total count of vocabulary entries
     */
    fun getVocabularyCount(): Int = vocabularyRepository.count().toInt()
}
