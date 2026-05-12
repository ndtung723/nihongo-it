import api from "@/utils/api";
import type { AxiosResponse } from "axios";
import type {
  VocabularyItem,
  VocabularyFilter,
  CreateVocabularyRequest,
  UpdateVocabularyRequest,
} from "@/types/learning.types";
import type { PagedResponse } from "@/types/common.types";

// Public endpoints — return unwrapped Promise<T>
const vocabularyService = {
  getVocabulary: (
    filter: VocabularyFilter,
  ): Promise<PagedResponse<VocabularyItem>> =>
    api
      .get("/api/v1/learning/vocabulary", {
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
    api
      .get(`/api/v1/learning/vocabulary/${id}`)
      .then((r) => r.data.data ?? r.data),

  getVocabularyByTerm: (term: string): Promise<VocabularyItem> =>
    api
      .get(`/api/v1/learning/vocabulary/term/${term}`)
      .then((r) => r.data.data ?? r.data),

  saveVocabulary: (id: string): Promise<void> =>
    api
      .post(`/api/v1/learning/vocabulary/${id}/save`, {})
      .then(() => undefined),

  removeSavedVocabulary: (id: string): Promise<void> =>
    api.delete(`/api/v1/learning/vocabulary/${id}/save`).then(() => undefined),

  getSavedVocabulary: (
    filter: VocabularyFilter,
  ): Promise<PagedResponse<VocabularyItem>> =>
    api
      .get("/api/v1/learning/vocabulary/saved", {
        params: {
          keyword: filter.keyword || undefined,
          page: filter.page,
          size: filter.size,
          sort: filter.sort || undefined,
        },
      })
      .then((r) => r.data),

  // Metadata helpers (vocabulary-specific endpoints used by learning views)
  getCategories: (): Promise<unknown> =>
    api.get("/api/v1/learning/vocabulary/categories").then((r) => r.data),

  getJlptLevels: (): Promise<unknown> =>
    api.get("/api/v1/learning/vocabulary/jlpt-levels").then((r) => r.data),

  getTopics: (): Promise<unknown> =>
    api.get("/api/v1/learning/topics").then((r) => r.data),

  getTopicsByCategory: (categoryId: string): Promise<unknown> =>
    api
      .get(`/api/v1/learning/vocabulary/categories/${categoryId}/topics`)
      .then((r) => r.data),

  // Admin endpoints — keep AxiosResponse<T> to match VocabularyManagementView usage
  adminGetAllVocabulary: (
    page = 0,
    size = 20,
  ): Promise<AxiosResponse<PagedResponse<VocabularyItem>>> =>
    api.get("/api/v1/learning/admin/vocabulary", { params: { page, size } }),

  adminGetVocabularyByTopicId: (
    topicId: string,
    page = 0,
    size = 20,
  ): Promise<AxiosResponse<PagedResponse<VocabularyItem>>> =>
    api.get(`/api/v1/learning/admin/vocabulary/topic/${topicId}`, {
      params: { page, size },
    }),

  adminGetVocabularyByJlptLevel: (
    jlptLevel: string,
    page = 0,
    size = 20,
  ): Promise<AxiosResponse<PagedResponse<VocabularyItem>>> =>
    api.get("/api/v1/learning/admin/vocabulary", {
      params: { jlptLevel, page, size },
    }),

  adminGetVocabularyById: (
    id: string,
  ): Promise<AxiosResponse<VocabularyItem>> =>
    api.get(`/api/v1/learning/admin/vocabulary/${id}`),

  adminCreateVocabulary: (
    data: CreateVocabularyRequest,
  ): Promise<AxiosResponse<VocabularyItem>> =>
    api.post("/api/v1/learning/admin/vocabulary", data),

  adminUpdateVocabulary: (
    id: string,
    data: UpdateVocabularyRequest,
  ): Promise<AxiosResponse<VocabularyItem>> =>
    api.put(`/api/v1/learning/admin/vocabulary/${id}`, data),

  adminDeleteVocabulary: (id: string): Promise<AxiosResponse<unknown>> =>
    api.delete(`/api/v1/learning/admin/vocabulary/${id}`),

  adminSearchVocabulary: (
    query: string,
    topicId?: string,
    jlptLevel?: string,
    page = 0,
    size = 20,
  ): Promise<AxiosResponse<PagedResponse<VocabularyItem>>> => {
    const params: Record<string, unknown> = { query, page, size };
    if (topicId) params.topicId = topicId;
    if (jlptLevel) params.jlptLevel = jlptLevel;
    return api.get("/api/v1/learning/admin/vocabulary/search", { params });
  },
};

export default vocabularyService;
