import api from '@/lib/api'
import type {
  AdminStatisticsOverview,
  UserStatisticsDetail,
  UserStatisticsListResponse,
} from '@/types/statistics.types'

// Backend wraps responses inconsistently. The overview + user-detail endpoints
// nest under `.data.data`; the list endpoint may return either shape.
const unwrap = <T>(payload: unknown, fallback: T): T => {
  const obj = payload as { data?: T } | T
  if (obj && typeof obj === 'object' && 'data' in obj && obj.data !== undefined) {
    return obj.data as T
  }
  return (obj as T) ?? fallback
}

interface UsersListParams {
  page?: number
  size?: number
  sortBy?: string
  sortDir?: 'asc' | 'desc'
  search?: string
}

const statisticsService = {
  async getOverview(): Promise<AdminStatisticsOverview | null> {
    try {
      const res = await api.get('/api/v1/learning/admin/statistics/overview')
      return unwrap<AdminStatisticsOverview | null>(res.data, null)
    } catch {
      return null
    }
  },

  async getUsers(params: UsersListParams = {}): Promise<UserStatisticsListResponse> {
    const { page = 0, size = 10, sortBy = 'email', sortDir = 'asc', search } = params
    const qp: Record<string, string | number> = { page, size, sortBy, sortDir }
    if (search?.trim()) qp.search = search.trim()
    const res = await api.get('/api/v1/learning/admin/statistics/users', { params: qp })
    // Endpoint sometimes returns shape directly, sometimes wraps in {data: {...}}
    return unwrap<UserStatisticsListResponse>(res.data, {
      users: [],
      totalItems: 0,
      totalPages: 0,
      currentPage: 0,
    })
  },

  async getUserDetail(userId: string): Promise<UserStatisticsDetail | null> {
    try {
      const res = await api.get(`/api/v1/learning/admin/statistics/users/${userId}`)
      return unwrap<UserStatisticsDetail | null>(res.data, null)
    } catch {
      return null
    }
  },
}

export default statisticsService
