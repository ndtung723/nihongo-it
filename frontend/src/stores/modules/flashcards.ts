import { defineStore } from "pinia";
import { ref, computed } from "vue";
import flashcardService from "@/services/flashcard.service";
import type { FlashcardDTO, FlashcardStats } from "@/types/learning.types";
import { extractApiError } from "@/types/common.types";

export const useFlashcardsStore = defineStore("flashcards", () => {
  const dueCards = ref<FlashcardDTO[]>([]);
  const stats = ref<FlashcardStats | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  const hasDueCards = computed(() => dueCards.value.length > 0);
  const dueCardsCount = computed(() => dueCards.value.length);

  async function fetchDueCards() {
    loading.value = true;
    error.value = null;
    try {
      dueCards.value = await flashcardService.getDueCards();
    } catch (err) {
      error.value = extractApiError(err, "Failed to load due cards");
    } finally {
      loading.value = false;
    }
  }

  async function fetchStats() {
    try {
      stats.value =
        (await flashcardService.getStudyStatistics()) as FlashcardStats;
    } catch {
      // stats are non-critical — silent fail
    }
  }

  return {
    dueCards,
    stats,
    loading,
    error,
    hasDueCards,
    dueCardsCount,
    fetchDueCards,
    fetchStats,
  };
});
