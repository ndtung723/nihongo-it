import { defineStore } from "pinia";
import { ref, computed } from "vue";
import vocabularyService from "@/services/vocabulary.service";
import type { VocabularyItem, VocabularyFilter } from "@/types/learning.types";
import { extractApiError } from "@/types/common.types";

export const useVocabularyStore = defineStore("vocabulary", () => {
  // State
  const currentVocabulary = ref<VocabularyItem | null>(null);
  const relatedVocabulary = ref<VocabularyItem[]>([]);
  const savedVocabulary = ref<VocabularyItem[]>([]);
  const loading = ref(false);
  const savedLoading = ref(false);
  const error = ref<string | null>(null);
  const processedExample = ref("");
  const totalSavedItems = ref(0);
  const totalSavedPages = ref(0);

  // Getters
  const hasVocabulary = computed(() => !!currentVocabulary.value);
  const hasRelatedItems = computed(() => relatedVocabulary.value.length > 0);
  const hasSavedItems = computed(() => savedVocabulary.value.length > 0);
  const isFavorite = computed(() => currentVocabulary.value?.isSaved || false);

  // Actions
  async function fetchVocabularyById(id: string) {
    loading.value = true;
    error.value = null;
    try {
      const vocabulary = await vocabularyService.getVocabularyById(id);
      currentVocabulary.value = vocabulary;
      await fetchRelatedTerms(vocabulary);
      return vocabulary;
    } catch (err: unknown) {
      error.value = extractApiError(err, "Failed to load vocabulary details");
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function fetchVocabularyByTerm(term: string) {
    loading.value = true;
    error.value = null;
    try {
      const vocabulary = await vocabularyService.getVocabularyByTerm(term);
      currentVocabulary.value = vocabulary;
      await fetchRelatedTerms(vocabulary);
      return vocabulary;
    } catch (err: unknown) {
      error.value = extractApiError(err, "Failed to load vocabulary details");
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function fetchSavedVocabulary(
    page = 0,
    size = 10,
    keyword?: string,
    sort?: string,
  ) {
    savedLoading.value = true;
    error.value = null;
    try {
      const filter: VocabularyFilter = {
        keyword: keyword || null,
        jlptLevel: null,
        topicName: null,
        page,
        size,
        sort: sort || null,
      };
      const response = await vocabularyService.getSavedVocabulary(filter);
      if (response && Array.isArray(response.content)) {
        savedVocabulary.value = response.content;
        totalSavedItems.value = response.totalElements;
        totalSavedPages.value = response.totalPages;
      } else {
        savedVocabulary.value = [];
        totalSavedItems.value = 0;
        totalSavedPages.value = 0;
      }
      return response;
    } catch (err: unknown) {
      error.value = extractApiError(err, "Failed to load saved vocabulary");
      savedVocabulary.value = [];
      totalSavedItems.value = 0;
      totalSavedPages.value = 0;
      return null;
    } finally {
      savedLoading.value = false;
    }
  }

  async function fetchRelatedTerms(vocabulary: VocabularyItem) {
    try {
      const filters: VocabularyFilter = {
        keyword: null,
        topicName: vocabulary.topicName || null,
        jlptLevel: vocabulary.jlptLevel,
        page: 0,
        size: 5,
      };
      const response = await vocabularyService.getVocabulary(filters);
      if (response && Array.isArray(response.content)) {
        relatedVocabulary.value = response.content
          .filter((item) => item.vocabId !== vocabulary.vocabId)
          .slice(0, 4);
      }
    } catch {
      relatedVocabulary.value = [];
    }
  }

  async function toggleFavorite(): Promise<boolean> {
    if (!currentVocabulary.value) return false;
    try {
      if (isFavorite.value) {
        await vocabularyService.removeSavedVocabulary(
          currentVocabulary.value.vocabId,
        );
      } else {
        await vocabularyService.saveVocabulary(currentVocabulary.value.vocabId);
      }
      if (currentVocabulary.value) {
        currentVocabulary.value = {
          ...currentVocabulary.value,
          isSaved: !isFavorite.value,
        };
      }
      return true;
    } catch {
      return false;
    }
  }

  async function removeSavedItem(vocabId: string): Promise<boolean> {
    try {
      await vocabularyService.removeSavedVocabulary(vocabId);
      savedVocabulary.value = savedVocabulary.value.filter(
        (item) => item.vocabId !== vocabId,
      );
      if (totalSavedItems.value > 0) {
        totalSavedItems.value--;
      }
      return true;
    } catch {
      return false;
    }
  }

  function reset() {
    currentVocabulary.value = null;
    relatedVocabulary.value = [];
    loading.value = false;
    error.value = null;
    processedExample.value = "";
  }

  function resetSaved() {
    savedVocabulary.value = [];
    savedLoading.value = false;
    totalSavedItems.value = 0;
    totalSavedPages.value = 0;
  }

  function setProcessedExample(text: string) {
    processedExample.value = text;
  }

  return {
    currentVocabulary,
    relatedVocabulary,
    savedVocabulary,
    loading,
    savedLoading,
    error,
    processedExample,
    totalSavedItems,
    totalSavedPages,
    hasVocabulary,
    hasRelatedItems,
    hasSavedItems,
    isFavorite,
    fetchVocabularyById,
    fetchVocabularyByTerm,
    fetchRelatedTerms,
    fetchSavedVocabulary,
    toggleFavorite,
    removeSavedItem,
    reset,
    resetSaved,
    setProcessedExample,
  };
});
