package com.example.learningservice.service

import com.example.learningservice.dto.ConversationDTO
import com.example.learningservice.dto.CreateConversationRequest
import com.example.learningservice.dto.PagedResponse
import com.example.learningservice.dto.UpdateConversationRequest
import com.example.learningservice.entity.ConversationEntity
import com.example.learningservice.entity.ConversationLineEntity
import com.example.learningservice.exception.BusinessException
import com.example.learningservice.repository.ConversationLineRepository
import com.example.learningservice.repository.ConversationRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val conversationLineRepository: ConversationLineRepository
) {

    fun getAllConversations(pageable: Pageable): PagedResponse<ConversationDTO> {
        val page = conversationRepository.findAll(pageable)
        val content = page.content.map { ConversationDTO.fromEntity(it) }
        
        return PagedResponse(
            content = content,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            currentPage = page.number,
            size = page.size
        )
    }

    fun searchConversations(query: String, pageable: Pageable): PagedResponse<ConversationDTO> {
        val page = conversationRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(query, query, pageable)
        val content = page.content.map { ConversationDTO.fromEntity(it) }
        
        return PagedResponse(
            content = content,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            currentPage = page.number,
            size = page.size
        )
    }

    fun getConversationsByJlptLevel(level: String, pageable: Pageable): PagedResponse<ConversationDTO> {
        val page = conversationRepository.findByJlptLevel(level, pageable)
        val content = page.content.map { ConversationDTO.fromEntity(it) }
        
        return PagedResponse(
            content = content,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            currentPage = page.number,
            size = page.size
        )
    }

    fun getConversationById(conversationId: UUID): ConversationDTO {
        val conversation = conversationRepository.findById(conversationId)
            .orElseThrow { BusinessException("Conversation not found with id: $conversationId") }
        
        return ConversationDTO.fromEntity(conversation)
    }

    @Transactional
    fun createConversation(request: CreateConversationRequest): ConversationDTO {
        val conversation = ConversationEntity(
            title = request.title,
            description = request.description,
            jlptLevel = request.jlptLevel,
            unit = request.unit,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        val savedConversation = conversationRepository.save(conversation)
        
        // Save conversation lines
        if (request.lines.isNotEmpty()) {
            val conversationLines = request.lines.map { lineRequest ->
                ConversationLineEntity(
                    conversation = savedConversation,
                    speaker = lineRequest.speaker,
                    japaneseText = lineRequest.japaneseText,
                    vietnameseTranslation = lineRequest.vietnameseTranslation,
                    notes = lineRequest.notes,
                    importantVocab = lineRequest.importantVocab,
                    orderIndex = lineRequest.orderIndex
                )
            }
            
            conversationLineRepository.saveAll(conversationLines)
        }
        
        return getConversationById(savedConversation.convId!!)
    }

    @Transactional
    fun updateConversation(conversationId: UUID, request: UpdateConversationRequest): ConversationDTO {
        val conversation = conversationRepository.findById(conversationId)
            .orElseThrow { BusinessException("Conversation not found with id: $conversationId") }
        
        // Update conversation properties
        conversation.apply {
            title = request.title ?: title
            description = request.description ?: description
            jlptLevel = request.jlptLevel ?: jlptLevel
            unit = request.unit ?: unit
            updatedAt = LocalDateTime.now()
        }
        
        conversationRepository.save(conversation)
        
        // Update conversation lines if provided
        if (request.lines != null) {
            // Delete existing lines
            conversationLineRepository.deleteByConversationId(conversationId)
            
            // Create new lines
            val conversationLines = request.lines.map { lineRequest ->
                ConversationLineEntity(
                    conversation = conversation,
                    speaker = lineRequest.speaker,
                    japaneseText = lineRequest.japaneseText,
                    vietnameseTranslation = lineRequest.vietnameseTranslation,
                    notes = lineRequest.notes,
                    importantVocab = lineRequest.importantVocab,
                    orderIndex = lineRequest.orderIndex
                )
            }
            
            conversationLineRepository.saveAll(conversationLines)
        }
        
        return getConversationById(conversationId)
    }

    @Transactional
    fun deleteConversation(conversationId: UUID) {
        if (!conversationRepository.existsById(conversationId)) {
            throw BusinessException("Conversation not found with id: $conversationId")
        }
        
        // Delete associated lines first
        conversationLineRepository.deleteByConversationId(conversationId)
        
        // Delete the conversation
        conversationRepository.deleteById(conversationId)
    }
} 