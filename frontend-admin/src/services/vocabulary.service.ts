import api from '@/lib/api'
import type { PagedResponse } from '@/types/common.types'
import type {
  CreateVocabularyRequest,
  UpdateVocabularyRequest,
  VocabularyItem,
} from '@/types/learning.types'

interface ListParams {
  page?: number
  size?: number
  jlptLevel?: string
  topicId?: string
}

const vocabularyService = {
  list: (params: ListParams = {}): Promise<PagedResponse<VocabularyItem>> => {
    const { page = 0, size = 20, jlptLevel, topicId } = params
    if (topicId) {
      return api
        .get(`/api/v1/learning/admin/vocabulary/topic/${topicId}`, { params: { page, size } })
        .then((r) => r.data)
    }
    const queryParams: Record<string, string | number> = { page, size }
    if (jlptLevel) queryParams.jlptLevel = jlptLevel
    return api
      .get('/api/v1/learning/admin/vocabulary', { params: queryParams })
      .then((r) => r.data)
  },

  getById: (id: string): Promise<VocabularyItem> =>
    api.get(`/api/v1/learning/admin/vocabulary/${id}`).then((r) => r.data),

  create: (data: CreateVocabularyRequest): Promise<VocabularyItem> =>
    api.post('/api/v1/learning/admin/vocabulary', data).then((r) => r.data),

  update: (id: string, data: UpdateVocabularyRequest): Promise<VocabularyItem> =>
    api.put(`/api/v1/learning/admin/vocabulary/${id}`, data).then((r) => r.data),

  delete: (id: string): Promise<void> =>
    api.delete(`/api/v1/learning/admin/vocabulary/${id}`).then(() => undefined),

  search: (
    query: string,
    options: { topicId?: string; jlptLevel?: string; page?: number; size?: number } = {},
  ): Promise<PagedResponse<VocabularyItem>> => {
    const { topicId, jlptLevel, page = 0, size = 20 } = options
    const params: Record<string, string | number> = { query, page, size }
    if (topicId) params.topicId = topicId
    if (jlptLevel) params.jlptLevel = jlptLevel
    return api
      .get('/api/v1/learning/admin/vocabulary/search', { params })
      .then((r) => r.data)
  },
}

export default vocabularyService
