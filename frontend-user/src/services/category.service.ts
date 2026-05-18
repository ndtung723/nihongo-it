import api from '@/lib/api'
import type { Category } from '@/types/learning.types'

const categoryService = {
  getAllCategories: (): Promise<Category[]> =>
    api.get('/api/v1/learning/categories').then((r) => r.data),

  getCategoryById: (id: string): Promise<Category> =>
    api.get(`/api/v1/learning/categories/${id}`).then((r) => r.data),

  searchCategories: (query: string): Promise<Category[]> =>
    api
      .get('/api/v1/learning/categories/search', { params: { query } })
      .then((r) => r.data),
}

export default categoryService
