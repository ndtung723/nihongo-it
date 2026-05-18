import api from '@/lib/api'
import type { VocabularyItem, VocabularyFilter } from '@/types/learning.types'
import type { PagedResponse } from '@/types/common.types'

const vocabularyService = {
  getVocabulary: (filter: VocabularyFilter): Promise<PagedResponse<VocabularyItem>> =>
    api
      .get('/api/v1/learning/vocabulary', {
        params: {
          keyword: filter.keyword || undefined,
          jlptLevel: filter.jlptLevel || undefined,
          topicName: filter.topicName || undefined,
          page: filter.page,
          size: filter.size,
          sort: filter.sort || undefined,
        },
      })
      .then((r) => r.data),

  getVocabularyById: (id: string): Promise<VocabularyItem> =>
    api.get(`/api/v1/learning/vocabulary/${id}`).then((r) => r.data.data ?? r.data),

  getVocabularyByTerm: (term: string): Promise<VocabularyItem> =>
    api
      .get(`/api/v1/learning/vocabulary/term/${encodeURIComponent(term)}`)
      .then((r) => r.data.data ?? r.data),

  saveVocabulary: (id: string): Promise<void> =>
    api.post(`/api/v1/learning/vocabulary/${id}/save`, {}).then(() => undefined),

  removeSavedVocabulary: (id: string): Promise<void> =>
    api.delete(`/api/v1/learning/vocabulary/${id}/save`).then(() => undefined),

  getSavedVocabulary: (filter: VocabularyFilter): Promise<PagedResponse<VocabularyItem>> =>
    api
      .get('/api/v1/learning/vocabulary/saved', {
        params: {
          keyword: filter.keyword || undefined,
          page: filter.page,
          size: filter.size,
          sort: filter.sort || undefined,
        },
      })
      .then((r) => r.data),
}

export default vocabularyService
