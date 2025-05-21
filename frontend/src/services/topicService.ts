import api from '../utils/api';
import type { AxiosResponse } from 'axios';

export interface Topic {
  topicId: string;
  name: string;
  meaning: string;
  displayOrder: number;
  categoryId: string;
  categoryName?: string;
  vocabularyCount?: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateTopicRequest {
  name: string;
  meaning: string;
  description?: string;
  displayOrder?: number;
  isActive?: boolean;
  categoryId: string;
}

export interface UpdateTopicRequest {
  name?: string;
  meaning?: string;
  description?: string;
  displayOrder?: number;
  isActive?: boolean;
  categoryId?: string;
}

const topicService = {
  // Public API endpoints
  getAllTopics(): Promise<AxiosResponse<Topic[]>> {
    return api.get('/learning-service-api/v1/topics');
  },

  getTopicById(id: string): Promise<AxiosResponse<Topic>> {
    return api.get(`/learning-service-api/v1/topics/${id}`);
  },

  getTopicsByCategoryId(categoryId: string): Promise<AxiosResponse<Topic[]>> {
    return api.get(`/learning-service-api/v1/topics/category/${categoryId}`);
  },

  // Admin API endpoints
  adminGetAllTopics(): Promise<AxiosResponse<Topic[]>> {
    return api.get('/learning-service-api/v1/admin/topics');
  },

  adminGetTopicById(id: string): Promise<AxiosResponse<Topic>> {
    return api.get(`/learning-service-api/v1/admin/topics/${id}`);
  },

  adminGetTopicsByCategoryId(categoryId: string): Promise<AxiosResponse<Topic[]>> {
    return api.get(`/learning-service-api/v1/admin/topics/category/${categoryId}`);
  },

  adminCreateTopic(data: CreateTopicRequest): Promise<AxiosResponse<Topic>> {
    return api.post('/learning-service-api/v1/admin/topics', data);
  },

  adminUpdateTopic(id: string, data: UpdateTopicRequest): Promise<AxiosResponse<Topic>> {
    return api.put(`/learning-service-api/v1/admin/topics/${id}`, data);
  },

  adminDeleteTopic(id: string): Promise<AxiosResponse<any>> {
    return api.delete(`/learning-service-api/v1/admin/topics/${id}`);
  },

  adminToggleTopicStatus(id: string): Promise<AxiosResponse<Topic>> {
    return api.patch(`/learning-service-api/v1/admin/topics/${id}/toggle-status`);
  },

  adminSearchTopics(categoryId: string, query: string): Promise<AxiosResponse<Topic[]>> {
    return api.get(`/learning-service-api/v1/admin/topics/search?categoryId=${categoryId}&query=${encodeURIComponent(query)}`);
  }
};

export default topicService;
