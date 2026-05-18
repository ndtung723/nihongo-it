import api from '@/lib/api'
import type {
  Category,
  CreateCategoryRequest,
  UpdateCategoryRequest,
} from '@/types/learning.types'

const categoryService = {
  getAll: (): Promise<Category[]> =>
    api.get('/api/v1/learning/admin/categories').then((r) => r.data),

  getById: (id: string): Promise<Category> =>
    api.get(`/api/v1/learning/admin/categories/${id}`).then((r) => r.data),

  create: (data: CreateCategoryRequest): Promise<Category> =>
    api.post('/api/v1/learning/admin/categories', data).then((r) => r.data),

  update: (id: string, data: UpdateCategoryRequest): Promise<Category> =>
    api.put(`/api/v1/learning/admin/categories/${id}`, data).then((r) => r.data),

  delete: (id: string): Promise<void> =>
    api.delete(`/api/v1/learning/admin/categories/${id}`).then(() => undefined),

  toggleStatus: (id: string): Promise<Category> =>
    api.patch(`/api/v1/learning/admin/categories/${id}/toggle-status`).then((r) => r.data),

  search: (nameQuery?: string, meaningQuery?: string): Promise<Category[]> => {
    const params: Record<string, string> = {}
    if (nameQuery) params.query = nameQuery
    if (meaningQuery) params.meaningQuery = meaningQuery
    return api
      .get('/api/v1/learning/admin/categories/search', { params })
      .then((r) => r.data)
  },
}

export default categoryService
