import api from '@/lib/api'
import type {
  DashboardStats,
  UserCreateRequest,
  UserDetailInfo,
  UserInfo,
  UserListResponse,
  UserUpdateRequest,
} from '@/types/user.types'

interface SimpleResult {
  status: string
  message: string
}

const adminService = {
  async getDashboardStats(): Promise<DashboardStats> {
    const res = await api.get('/api/v1/learning/admin/dashboard/stats')
    return res.data?.data ?? res.data
  },

  async getUsers(
    page = 0,
    size = 10,
    search?: string,
    sortBy: string = 'email',
    sortDir: 'asc' | 'desc' = 'asc',
  ): Promise<UserListResponse> {
    const params: Record<string, string | number> = { page, size, sortBy, sortDir }
    if (search?.trim()) params.search = search.trim()
    const res = await api.get('/api/v1/user/admin/users', { params })
    return res.data
  },

  async getUserById(userId: string): Promise<UserDetailInfo> {
    const res = await api.get(`/api/v1/user/admin/users/${userId}`)
    return res.data
  },

  async createUser(data: UserCreateRequest): Promise<UserInfo> {
    const res = await api.post('/api/v1/user/admin/users', data)
    return res.data
  },

  async updateUser(userId: string, data: UserUpdateRequest): Promise<UserInfo> {
    const res = await api.put(`/api/v1/user/admin/users/${userId}`, data)
    return res.data
  },

  async deactivateUser(userId: string): Promise<SimpleResult> {
    const res = await api.delete(`/api/v1/user/admin/users/${userId}`)
    return res.data
  },

  async activateUser(userId: string): Promise<SimpleResult> {
    const res = await api.put(`/api/v1/user/admin/users/${userId}/activate`)
    return res.data
  },

  async changeUserRole(userId: string, roleId: number): Promise<SimpleResult> {
    const res = await api.put(`/api/v1/user/admin/users/${userId}/change-role`, null, {
      params: { roleId },
    })
    return res.data
  },
}

export default adminService
