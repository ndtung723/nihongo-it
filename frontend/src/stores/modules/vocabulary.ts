import { defineStore } from "pinia";
import { ref, computed } from "vue";
import vocabularyService from "@/services/vocabulary.service.ts";
import type {
  VocabularyItem,
  VocabularyFilter,
  PagedResponse,
} from "@/services/vocabulary.service.ts";
import { useToast } from "vue-toast-notification";

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
    } catch (err: any) {
      error.value =
        err.response?.data?.message || "Failed to load vocabulary details";
      const toast = useToast();
      toast.error(error.value || "Failed to load vocabulary details", {
        position: "top",
        duration: 3000,
      });
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
    } catch (err: any) {
      error.value =
        err.response?.data?.message || "Failed to load vocabulary details";
      const toast = useToast();
      toast.error(error.value || "Failed to load vocabulary details", {
        position: "top",
        duration: 3000,
      });
      return null;
    } finally {
      loading.value = false;
    }
  }

  async function fetchSavedVocabulary(
    page = 0,
    size = 10,
    keyword?: string,
    sort?: string
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

        // Don't show error for empty results
        if (!response) {
          throw new Error("Invalid response format");
        }
      }

      return response;
    } catch (err: any) {
      error.value =
        err.response?.data?.message || "Failed to load saved vocabulary";
      savedVocabulary.value = [];
      totalSavedItems.value = 0;
      totalSavedPages.value = 0;

      const toast = useToast();
      toast.error(error.value || "Failed to load saved vocabulary", {
        position: "top",
        duration: 3000,
      });

      return null;
    } finally {
      savedLoading.value = false;
    }
  }

  async function fetchRelatedTerms(vocabulary: VocabularyItem) {
    try {
      // Fetch related terms based on topicName or JLPT level
      const filters: VocabularyFilter = {
        keyword: null,
        topicName: vocabulary.topicName || null,
        jlptLevel: vocabulary.jlptLevel,
        page: 0,
        size: 5,
      };

      const response = await vocabularyService.getVocabulary(filters);

      if (response && Array.isArray(response.content)) {
        // Filter out the current vocabulary item
        relatedVocabulary.value = response.content
          .filter((item) => item.vocabId !== vocabulary.vocabId)
          .slice(0, 4); // Limit to 4 items
      }
    } catch (err) {
      relatedVocabulary.value = [];
      // Don't show error toast, this is not critical
    }
  }

  async function toggleFavorite() {
    if (!currentVocabulary.value) return false;

    const toast = useToast();
    try {
      if (isFavorite.value) {
        await vocabularyService.removeSavedVocabulary(
          currentVocabulary.value.vocabId
        );
        toast.success("Removed from saved items", {
          position: "top",
          duration: 2000,
        });
      } else {
        await vocabularyService.saveVocabulary(currentVocabulary.value.vocabId);
        toast.success("Added to saved items", {
          position: "top",
          duration: 2000,
        });
      }

      // Toggle the state locally
      if (currentVocabulary.value) {
        currentVocabulary.value = {
          ...currentVocabulary.value,
          isSaved: !isFavorite.value,
        };
      }

      return true;
    } catch (err) {
      toast.error("Failed to update saved status", {
        position: "top",
        duration: 3000,
      });
      return false;
    }
  }

  async function removeSavedItem(vocabId: string) {
    const toast = useToast();
    try {
      await vocabularyService.removeSavedVocabulary(vocabId);

      // Remove the item from the local state
      savedVocabulary.value = savedVocabulary.value.filter(
        (item) => item.vocabId !== vocabId
      );

      // Decrement the total count
      if (totalSavedItems.value > 0) {
        totalSavedItems.value--;
      }

      toast.success("Removed from saved items", {
        position: "top",
        duration: 2000,
      });

      return true;
    } catch (err) {
      toast.error("Failed to remove item from saved list", {
        position: "top",
        duration: 3000,
      });
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
    // State
    currentVocabulary,
    relatedVocabulary,
    savedVocabulary,
    loading,
    savedLoading,
    error,
    processedExample,
    totalSavedItems,
    totalSavedPages,

    // Getters
    hasVocabulary,
    hasRelatedItems,
    hasSavedItems,
    isFavorite,

    // Actions
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
