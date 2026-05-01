import { defineStore } from 'pinia';
import type { AxiosError } from 'axios';
import api from '@/utils/api';

export interface Flashcard {
  id: number | string
  front?: string
  back?: string
  notes?: string
}

export interface Review {
  card: Flashcard | null
  rating: number
  timestamp: Date
}

export interface FlashcardsState {
  dueCards: Flashcard[]
  currentCard: Flashcard | null
  reviewHistory: Review[]
  loading: boolean
  error: string | null
}

export const useFlashcardsStore = defineStore('flashcards', {
  state: (): FlashcardsState => ({
    dueCards: [],
    currentCard: null,
    reviewHistory: [],
    loading: false,
    error: null,
  }),

  getters: {
    hasDueCards: (state) => state.dueCards.length > 0,
    dueCardsCount: (state) => state.dueCards.length,
  },

  actions: {
    async fetchDueCards() {
      this.loading = true;
      try {
        const response = await api.get('/api/flashcards/due');
        this.dueCards = response.data;
        if (response.data.length > 0) {
          this.currentCard = response.data[0];
        } else {
          this.currentCard = null;
        }
        this.error = null;
      } catch (error) {
        const axiosError = error as AxiosError;
        this.error = axiosError.message;
      } finally {
        this.loading = false;
      }
    },

    async reviewCard({ cardId, rating }: { cardId: number | string; rating: number }) {
      this.loading = true;
      try {
        await api.post(`/api/flashcards/${cardId}/review`, { rating });

        const reviewedCard = this.currentCard;
        this.reviewHistory.push({
          card: reviewedCard,
          rating,
          timestamp: new Date(),
        });

        this.dueCards = this.dueCards.filter((card) => card.id !== cardId);

        if (this.dueCards.length > 0) {
          this.currentCard = this.dueCards[0];
        } else {
          this.currentCard = null;
        }

        this.error = null;
      } catch (error) {
        const axiosError = error as AxiosError;
        this.error = axiosError.message;
      } finally {
        this.loading = false;
      }
    },

    async createFlashcard(flashcardData: Partial<Flashcard>) {
      this.loading = true;
      try {
        await api.post('/api/flashcards', flashcardData);
        this.error = null;
      } catch (error) {
        const axiosError = error as AxiosError;
        this.error = axiosError.message;
      } finally {
        this.loading = false;
      }
    },
  },
});
