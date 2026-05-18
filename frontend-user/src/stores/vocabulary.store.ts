import { create } from 'zustand'
import vocabularyService from '@/services/vocabulary.service'
import { extractApiError } from '@/types/common.types'
import type { VocabularyFilter, VocabularyItem } from '@/types/learning.types'

interface VocabularyState {
  currentVocabulary: VocabularyItem | null
  relatedVocabulary: VocabularyItem[]
  savedVocabulary: VocabularyItem[]
  totalSavedItems: number
  totalSavedPages: number
  loading: boolean
  savedLoading: boolean
  error: string | null

  fetchVocabularyById: (id: string) => Promise<VocabularyItem | null>
  fetchVocabularyByTerm: (term: string) => Promise<VocabularyItem | null>
  fetchSavedVocabulary: (
    page?: number,
    size?: number,
    keyword?: string,
    sort?: string,
  ) => Promise<void>
  toggleFavorite: () => Promise<void>
  removeSavedItem: (vocabId: string) => Promise<void>
  reset: () => void
  resetSaved: () => void
}

async function fetchRelated(vocab: VocabularyItem): Promise<VocabularyItem[]> {
  try {
    const res = await vocabularyService.getVocabulary({
      keyword: null,
      topicName: vocab.topicName ?? null,
      jlptLevel: vocab.jlptLevel,
      page: 0,
      size: 5,
    })
    return (res.content ?? [])
      .filter((item) => item.vocabId !== vocab.vocabId)
      .slice(0, 4)
  } catch {
    return []
  }
}

export const useVocabularyStore = create<VocabularyState>((set, get) => ({
  currentVocabulary: null,
  relatedVocabulary: [],
  savedVocabulary: [],
  totalSavedItems: 0,
  totalSavedPages: 0,
  loading: false,
  savedLoading: false,
  error: null,

  async fetchVocabularyById(id) {
    set({ loading: true, error: null })
    try {
      const vocab = await vocabularyService.getVocabularyById(id)
      const related = await fetchRelated(vocab)
      set({ currentVocabulary: vocab, relatedVocabulary: related })
      return vocab
    } catch (err) {
      const msg = extractApiError(err, 'Không tải được chi tiết từ vựng')
      set({ error: msg })
      throw new Error(msg)
    } finally {
      set({ loading: false })
    }
  },

  async fetchVocabularyByTerm(term) {
    set({ loading: true, error: null })
    try {
      const vocab = await vocabularyService.getVocabularyByTerm(term)
      const related = await fetchRelated(vocab)
      set({ currentVocabulary: vocab, relatedVocabulary: related })
      return vocab
    } catch (err) {
      const msg = extractApiError(err, 'Không tải được chi tiết từ vựng')
      set({ error: msg })
      throw new Error(msg)
    } finally {
      set({ loading: false })
    }
  },

  async fetchSavedVocabulary(page = 0, size = 12, keyword, sort) {
    set({ savedLoading: true, error: null })
    try {
      const filter: VocabularyFilter = {
        keyword: keyword || null,
        jlptLevel: null,
        topicName: null,
        page,
        size,
        sort: sort || null,
      }
      const res = await vocabularyService.getSavedVocabulary(filter)
      set({
        savedVocabulary: res.content ?? [],
        totalSavedItems: res.totalElements ?? 0,
        totalSavedPages: res.totalPages ?? 0,
      })
    } catch (err) {
      const msg = extractApiError(err, 'Không tải được danh sách đã lưu')
      set({ savedVocabulary: [], totalSavedItems: 0, totalSavedPages: 0, error: msg })
      throw new Error(msg)
    } finally {
      set({ savedLoading: false })
    }
  },

  async toggleFavorite() {
    const current = get().currentVocabulary
    if (!current) return
    try {
      if (current.isSaved) {
        await vocabularyService.removeSavedVocabulary(current.vocabId)
      } else {
        await vocabularyService.saveVocabulary(current.vocabId)
      }
      set({ currentVocabulary: { ...current, isSaved: !current.isSaved } })
    } catch (err) {
      const msg = extractApiError(err, 'Không cập nhật được trạng thái')
      throw new Error(msg)
    }
  },

  async removeSavedItem(vocabId) {
    try {
      await vocabularyService.removeSavedVocabulary(vocabId)
      const next = get().savedVocabulary.filter((item) => item.vocabId !== vocabId)
      set({
        savedVocabulary: next,
        totalSavedItems: Math.max(0, get().totalSavedItems - 1),
      })
    } catch (err) {
      const msg = extractApiError(err, 'Không xóa được mục đã lưu')
      throw new Error(msg)
    }
  },

  reset() {
    set({ currentVocabulary: null, relatedVocabulary: [], loading: false, error: null })
  },

  resetSaved() {
    set({
      savedVocabulary: [],
      savedLoading: false,
      totalSavedItems: 0,
      totalSavedPages: 0,
    })
  },
}))
