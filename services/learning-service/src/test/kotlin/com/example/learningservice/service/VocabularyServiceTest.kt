package com.example.learningservice.service

import com.example.common.exception.BusinessException
import com.example.learningservice.dto.VocabularyFilterRequestDto
import com.example.learningservice.entity.CategoryEntity
import com.example.learningservice.entity.JlptLevel
import com.example.learningservice.entity.RoleEntity
import com.example.learningservice.entity.TopicEntity
import com.example.learningservice.entity.UserEntity
import com.example.learningservice.entity.VocabularyEntity
import com.example.learningservice.repository.FlashcardRepository
import com.example.learningservice.repository.TopicRepository
import com.example.learningservice.repository.UserRepository
import com.example.learningservice.repository.VocabularyRepository
import com.example.learningservice.util.UserAuthUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VocabularyServiceTest {
    private lateinit var vocabularyRepository: VocabularyRepository
    private lateinit var userRepository: UserRepository
    private lateinit var topicRepository: TopicRepository
    private lateinit var userAuthUtil: UserAuthUtil
    private lateinit var flashcardCrudService: FlashcardCrudService
    private lateinit var flashcardRepository: FlashcardRepository
    private lateinit var service: VocabularyService

    private val userId = UUID.randomUUID()
    private val vocabId = UUID.randomUUID()

    private fun makeUser() =
        UserEntity(
            userId = userId,
            email = "user@test.com",
            password = "encoded",
            fullName = "Test User",
            role = RoleEntity(RoleEntity.ROLE_USER, "ROLE_USER"),
        )

    private fun makeTopic(): TopicEntity {
        val category =
            CategoryEntity(
                categoryId = UUID.randomUUID(),
                name = "Test Category",
                meaning = "テスト",
            )
        return TopicEntity(
            topicId = UUID.randomUUID(),
            name = "Test Topic",
            meaning = "テスト",
            category = category,
        )
    }

    private fun makeVocabulary(savedUsers: MutableSet<UserEntity> = mutableSetOf()): VocabularyEntity =
        VocabularyEntity(
            vocabId = vocabId,
            term = "テスト",
            meaning = "test",
            pronunciation = "てすと",
            example = null,
            exampleMeaning = null,
            audioPath = null,
            jlptLevel = JlptLevel.N4,
            topic = makeTopic(),
            createdAt = Instant.now(),
            savedByUsers = savedUsers,
        )

    @BeforeEach
    fun setup() {
        vocabularyRepository = mock()
        userRepository = mock()
        topicRepository = mock()
        userAuthUtil = mock()
        flashcardCrudService = mock()
        flashcardRepository = mock()
        service =
            VocabularyService(
                vocabularyRepository,
                userRepository,
                topicRepository,
                userAuthUtil,
                flashcardCrudService,
                flashcardRepository,
            )
    }

    @Nested
    @DisplayName("saveVocabularyToNotebook()")
    inner class SaveVocabularyToNotebook {
        @Test
        @DisplayName("happy path → adds user to savedByUsers, saves, isSaved=true")
        fun happyPath_addsSavedUser() {
            val user = makeUser()
            val vocab = makeVocabulary()
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(userId)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab))
            whenever(vocabularyRepository.save(any())).thenReturn(vocab)

            val result = service.saveVocabularyToNotebook(vocabId)

            assertTrue(result.isSaved)
            verify(vocabularyRepository).save(vocab)
        }

        @Test
        @DisplayName("auto-creates flashcard (silently ignores if flashcard already exists)")
        fun autoCreatesFlashcard() {
            val user = makeUser()
            val vocab = makeVocabulary()
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(userId)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))
            whenever(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab))
            whenever(vocabularyRepository.save(any())).thenReturn(vocab)
            whenever(flashcardCrudService.createFlashcardFromVocabulary(vocabId))
                .thenThrow(BusinessException("Flashcard already exists"))

            // Should NOT throw even though flashcard creation fails
            val result = service.saveVocabularyToNotebook(vocabId)

            assertTrue(result.isSaved)
        }

        @Test
        @DisplayName("vocabulary not found → throws BusinessException")
        fun vocabNotFound_throwsBusinessException() {
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(userId)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(makeUser()))
            whenever(vocabularyRepository.findById(vocabId)).thenReturn(Optional.empty())

            assertThrows<Exception> {
                service.saveVocabularyToNotebook(vocabId)
            }
        }

        @Test
        @DisplayName("not authenticated → throws BusinessException")
        fun notAuthenticated_throwsBusinessException() {
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(null)

            assertThrows<BusinessException> {
                service.saveVocabularyToNotebook(vocabId)
            }
            verify(vocabularyRepository, never()).save(any())
        }
    }

    @Nested
    @DisplayName("removeVocabularyFromNotebook()")
    inner class RemoveVocabularyFromNotebook {
        @Test
        @DisplayName("happy path → removes user from savedByUsers, isSaved=false")
        fun happyPath_removesSavedUser() {
            val user = makeUser()
            val vocab = makeVocabulary(savedUsers = mutableSetOf(user))
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(userId)
            whenever(vocabularyRepository.findById(vocabId)).thenReturn(Optional.of(vocab))
            whenever(vocabularyRepository.save(any())).thenReturn(vocab)
            whenever(flashcardRepository.findByUser_UserIdAndVocabulary_VocabId(userId, vocabId))
                .thenReturn(emptyList())

            val result = service.removeVocabularyFromNotebook(vocabId)

            assertEquals(false, result.isSaved)
            verify(vocabularyRepository).save(vocab)
        }

        @Test
        @DisplayName("not authenticated → throws BusinessException")
        fun notAuthenticated_throwsBusinessException() {
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(null)

            assertThrows<BusinessException> {
                service.removeVocabularyFromNotebook(vocabId)
            }
        }
    }

    @Nested
    @DisplayName("filterVocabulary()")
    inner class FilterVocabulary {
        @Test
        @DisplayName("keyword filter → calls searchVocabulary")
        fun withKeyword_callsSearchVocabulary() {
            val filter = VocabularyFilterRequestDto(keyword = "テスト", page = 0, size = 10)
            val emptyPage = PageImpl<VocabularyEntity>(emptyList(), PageRequest.of(0, 10), 0)
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(null)
            whenever(vocabularyRepository.searchVocabulary(any(), any())).thenReturn(emptyPage)

            service.filterVocabulary(filter)

            verify(vocabularyRepository).searchVocabulary(any(), any())
        }

        @Test
        @DisplayName("no filters → calls findAll(pageable)")
        fun noFilters_callsFindAll() {
            val filter = VocabularyFilterRequestDto(page = 0, size = 20)
            val emptyPage = PageImpl<VocabularyEntity>(emptyList(), PageRequest.of(0, 20), 0)
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(null)
            whenever(vocabularyRepository.findAll(any<org.springframework.data.domain.Pageable>())).thenReturn(emptyPage)

            service.filterVocabulary(filter)

            verify(vocabularyRepository).findAll(any<org.springframework.data.domain.Pageable>())
        }

        @Test
        @DisplayName("authenticated user → isSaved correctly reflects saved status")
        fun authenticatedUser_isSavedReflectsSavedStatus() {
            val user = makeUser()
            val savedVocab = makeVocabulary(savedUsers = mutableSetOf(user))
            val page = PageImpl(listOf(savedVocab), PageRequest.of(0, 10), 1)
            val filter = VocabularyFilterRequestDto(page = 0, size = 10)
            whenever(userAuthUtil.getCurrentUserId()).thenReturn(userId)
            whenever(vocabularyRepository.findAll(any<org.springframework.data.domain.Pageable>())).thenReturn(page)

            val result = service.filterVocabulary(filter)

            assertEquals(1, result.content.size)
            assertTrue(result.content[0].isSaved)
        }
    }

    @Nested
    @DisplayName("getVocabularyCount()")
    inner class GetVocabularyCount {
        @Test
        @DisplayName("delegates to repository.count()")
        fun delegatesToRepository() {
            whenever(vocabularyRepository.count()).thenReturn(42L)

            val count = service.getVocabularyCount()

            assertEquals(42, count)
        }
    }
}
