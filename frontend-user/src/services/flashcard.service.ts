import api from '@/lib/api'
import type { FlashcardDTO, FlashcardStats, ReviewResponse } from '@/types/learning.types'

/**
 * Backend wraps responses as `{ data: T }`. These helpers unwrap with a fallback
 * to the raw body for forward-compatibility.
 */
const unwrap = <T>(payload: unknown, fallback: T): T => {
  const obj = payload as { data?: T } | T
  if (obj && typeof obj === 'object' && 'data' in obj && obj.data !== undefined) {
    return obj.data as T
  }
  return (obj as T) ?? fallback
}

const flashcardService = {
  async getFlashcardsByVocabulary(vocabId: string): Promise<FlashcardDTO[]> {
    const res = await api.get(`/api/v1/learning/flashcards/vocabulary/${vocabId}`)
    return unwrap<FlashcardDTO[]>(res.data, [])
  },

  async reviewFlashcard(flashcardId: string, rating: number): Promise<ReviewResponse> {
    const res = await api.post(`/api/v1/learning/flashcards/${flashcardId}/review`, { rating })
    return res.data
  },

  async createFlashcardFromVocabulary(vocabId: string): Promise<FlashcardDTO> {
    const res = await api.post(`/api/v1/learning/flashcards/vocabulary/${vocabId}`, {})
    const card = unwrap<FlashcardDTO | null>(res.data, null)
    if (!card) throw new Error('Failed to create flashcard')
    return card
  },

  async getDueCards(): Promise<FlashcardDTO[]> {
    const res = await api.get('/api/v1/learning/flashcards/due')
    return unwrap<FlashcardDTO[]>(res.data, [])
  },

  /**
   * Returns the raw statistics object. The backend shape is broader than
   * FlashcardStats — typed as `unknown` and narrowed by the stats page.
   */
  async getStudyStatistics(): Promise<unknown> {
    const res = await api.get('/api/v1/learning/flashcards/statistics')
    return unwrap<unknown>(res.data, {})
  },

  /** Convenience accessor when the caller only needs the basic summary stats. */
  async getStudySummary(): Promise<FlashcardStats | null> {
    try {
      const raw = (await this.getStudyStatistics()) as
        | { summary?: Partial<FlashcardStats> }
        | null
      if (!raw?.summary) return null
      const s = raw.summary
      return {
        totalCards: s.totalCards ?? 0,
        dueToday: s.dueToday ?? 0,
        newCards: s.newCards ?? 0,
        learningCards: s.learningCards ?? 0,
        masteredCards: s.masteredCards ?? 0,
        averageRetention: s.averageRetention,
      }
    } catch {
      return null
    }
  },
}

export default flashcardService
