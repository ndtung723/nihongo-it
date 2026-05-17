import api from '@/lib/api'
import type { Conversation } from '@/types/conversation.types'
import type { PagedResponse } from '@/types/common.types'

const conversationService = {
  getConversations(
    page = 0,
    size = 12,
    search = '',
    sortBy = 'title',
    sortDir: 'asc' | 'desc' = 'asc',
  ): Promise<PagedResponse<Conversation>> {
    const params: Record<string, string | number> = { page, size, sortBy, sortDir }
    if (search.trim()) params.search = search
    return api.get('/api/v1/learning/conversations', { params }).then((r) => r.data)
  },

  getConversationById(id: string): Promise<Conversation> {
    return api.get(`/api/v1/learning/conversations/${id}`).then((r) => r.data)
  },

  getConversationsByJlptLevel(
    level: string,
    page = 0,
    size = 12,
  ): Promise<PagedResponse<Conversation>> {
    return api
      .get(`/api/v1/learning/conversations/jlpt/${level}`, { params: { page, size } })
      .then((r) => r.data)
  },

  saveConversation(conversationId: string): Promise<void> {
    return api
      .post(`/api/v1/user/user/saved-conversations/${conversationId}`)
      .then(() => undefined)
  },

  unsaveConversation(conversationId: string): Promise<void> {
    return api
      .delete(`/api/v1/user/user/saved-conversations/${conversationId}`)
      .then(() => undefined)
  },

  checkSavedConversation(conversationId: string): Promise<{ saved: boolean }> {
    return api
      .get(`/api/v1/user/user/saved-conversations/check/${conversationId}`)
      .then((r) => r.data)
  },

  getSavedConversations(page = 0, size = 12): Promise<PagedResponse<Conversation>> {
    return api
      .get('/api/v1/user/user/saved-conversations', { params: { page, size } })
      .then((r) => r.data)
  },
}

export default conversationService
