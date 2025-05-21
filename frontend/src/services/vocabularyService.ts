import api from '../utils/api';
import type { AxiosResponse } from 'axios';

export interface Vocabulary {
  vocabId: string;
  term: string;
  meaning: string;
  pronunciation?: string;
  example?: string;
  exampleMeaning?: string;
  audioPath?: string;
  jlptLevel: string;
  topicId?: string;
  topicName?: string;
  createdAt?: string;
  isSaved?: boolean;
}

export interface PagedVocabularyResponse {
  content: Vocabulary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  lastPage: boolean;
}

export interface CreateVocabularyRequest {
  term: string;
  meaning: string;
  pronunciation?: string;
  example?: string;
  exampleMeaning?: string;
  audioPath?: string;
  topicName: string;
  jlptLevel: string;
}

export interface UpdateVocabularyRequest {
  term: string;
  meaning: string;
  pronunciation?: string;
  example?: string;
  exampleMeaning?: string;
  audioPath?: string;
  topicName?: string;
  jlptLevel?: string;
}

export interface VocabularyResponse {
  result: {
    status: string;
    message: string;
  };
  data?: Vocabulary;
}

const vocabularyService = {
  // Admin API endpoints
  adminGetAllVocabulary(page: number = 0, size: number = 20): Promise<AxiosResponse<PagedVocabularyResponse>> {
    return api.get(`/learning-service-api/v1/admin/vocabulary?page=${page}&size=${size}`);
  },

  adminGetVocabularyByTopicId(topicId: string, page: number = 0, size: number = 20): Promise<AxiosResponse<PagedVocabularyResponse>> {
    return api.get(`/learning-service-api/v1/admin/vocabulary/topic/${topicId}?page=${page}&size=${size}`);
  },

  adminGetVocabularyById(id: string): Promise<AxiosResponse<VocabularyResponse>> {
    return api.get(`/learning-service-api/v1/admin/vocabulary/${id}`);
  },

  adminCreateVocabulary(data: CreateVocabularyRequest): Promise<AxiosResponse<VocabularyResponse>> {
    return api.post('/learning-service-api/v1/admin/vocabulary', data);
  },

  adminUpdateVocabulary(id: string, data: UpdateVocabularyRequest): Promise<AxiosResponse<VocabularyResponse>> {
    return api.put(`/learning-service-api/v1/admin/vocabulary/${id}`, data);
  },

  adminDeleteVocabulary(id: string): Promise<AxiosResponse<any>> {
    return api.delete(`/learning-service-api/v1/admin/vocabulary/${id}`);
  },

  adminSearchVocabulary(query: string, topicId?: string, jlptLevel?: string, page: number = 0, size: number = 20): Promise<AxiosResponse<PagedVocabularyResponse>> {
    let url = `/learning-service-api/v1/admin/vocabulary/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`;

    if (topicId) {
      url += `&topicId=${topicId}`;
    }

    if (jlptLevel) {
      url += `&jlptLevel=${jlptLevel}`;
    }

    return api.get(url);
  },

  // Thêm phương thức để lấy vocabulary theo jlptLevel
  adminGetVocabularyByJlptLevel(jlptLevel: string, page: number = 0, size: number = 20): Promise<AxiosResponse<PagedVocabularyResponse>> {
    return api.get(`/learning-service-api/v1/admin/vocabulary?jlptLevel=${jlptLevel}&page=${page}&size=${size}`);
  }
};

export default vocabularyService;
