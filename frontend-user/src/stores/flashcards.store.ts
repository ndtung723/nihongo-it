import { create } from 'zustand'
import flashcardService from '@/services/flashcard.service'
import { extractApiError } from '@/types/common.types'
import type { FlashcardDTO, FlashcardStats } from '@/types/learning.types'

interface FlashcardsState {
  dueCards: FlashcardDTO[]
  stats: FlashcardStats | null
  loading: boolean
  error: string | null
  fetchDueCards: () => Promise<void>
  fetchStats: () => Promise<void>
  /** Optimistically remove a card from the local due list after it's been reviewed. */
  removeFromDue: (cardId: string) => void
}

export const useFlashcardsStore = create<FlashcardsState>((set) => ({
  dueCards: [],
  stats: null,
  loading: false,
  error: null,

  async fetchDueCards() {
    set({ loading: true, error: null })
    try {
      const cards = await flashcardService.getDueCards()
      set({ dueCards: cards })
    } catch (err) {
      const msg = extractApiError(err, 'Không tải được thẻ ghi nhớ')
      set({ error: msg, dueCards: [] })
      throw new Error(msg)
    } finally {
      set({ loading: false })
    }
  },

  async fetchStats() {
    try {
      const stats = await flashcardService.getStudySummary()
      set({ stats })
    } catch {
      // Stats are non-critical — silent fail
    }
  },

  removeFromDue(cardId) {
    set((s) => ({ dueCards: s.dueCards.filter((c) => c.id !== cardId) }))
  },
}))
