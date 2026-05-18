import api from '@/lib/api'
import type { CreateTopicRequest, Topic, UpdateTopicRequest } from '@/types/learning.types'

const topicService = {
  getAll: (): Promise<Topic[]> =>
    api.get('/api/v1/learning/admin/topics').then((r) => r.data),

  getById: (id: string): Promise<Topic> =>
    api.get(`/api/v1/learning/admin/topics/${id}`).then((r) => r.data),

  getByCategoryId: (categoryId: string): Promise<Topic[]> =>
    api.get(`/api/v1/learning/admin/topics/category/${categoryId}`).then((r) => r.data),

  create: (data: CreateTopicRequest): Promise<Topic> =>
    api.post('/api/v1/learning/admin/topics', data).then((r) => r.data),

  update: (id: string, data: UpdateTopicRequest): Promise<Topic> =>
    api.put(`/api/v1/learning/admin/topics/${id}`, data).then((r) => r.data),

  delete: (id: string): Promise<void> =>
    api.delete(`/api/v1/learning/admin/topics/${id}`).then(() => undefined),

  toggleStatus: (id: string): Promise<Topic> =>
    api.patch(`/api/v1/learning/admin/topics/${id}/toggle-status`).then((r) => r.data),

  search: (categoryId: string, query: string): Promise<Topic[]> =>
    api
      .get('/api/v1/learning/admin/topics/search', { params: { categoryId, query } })
      .then((r) => r.data),
}

export default topicService
