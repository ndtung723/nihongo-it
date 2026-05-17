import { create } from 'zustand'
import api from '@/lib/api'
import { clearAccessToken, getAccessToken, setAccessToken } from '@/lib/tokenStore'
import { decodeToken } from '@/lib/jwt'
import { ROLES } from '@/types/roles'
import { extractApiError } from '@/types/common.types'
import type {
  ChangePasswordRequest,
  GetCurrentUserResponse,
  LoginRequest,
  SignupRequest,
  UpdateProfileRequest,
  UserInfo,
} from '@/types/user.types'

interface AuthState {
  user: UserInfo | null
  loading: boolean
  error: string | null
  initialized: boolean
  initialize: () => Promise<void>
  login: (request: LoginRequest) => Promise<void>
  loginWithGoogle: (tokenId: string) => Promise<void>
  register: (request: SignupRequest) => Promise<string>
  logout: () => Promise<void>
  fetchCurrentUser: () => Promise<void>
  changePassword: (request: ChangePasswordRequest) => Promise<void>
  updateProfile: (request: UpdateProfileRequest) => Promise<void>
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  loading: false,
  error: null,
  initialized: false,

  async initialize() {
    if (get().initialized) return
    set({ loading: true })
    try {
      const res = await api.post<{ token: string }>('/api/v1/user/auth/refresh-token', {})
      setAccessToken(res.data.token)
      await get().fetchCurrentUser()
    } catch {
      clearAccessToken()
      set({ user: null })
    } finally {
      set({ loading: false, initialized: true })
    }
  },

  async login(request) {
    set({ loading: true, error: null })
    try {
      const res = await api.post<{ token: string }>('/api/v1/user/auth/login', request)
      setAccessToken(res.data.token)
      await get().fetchCurrentUser()
    } catch (err) {
      const msg = extractApiError(err, 'Đăng nhập thất bại')
      set({ error: msg })
      throw new Error(msg)
    } finally {
      set({ loading: false })
    }
  },

  async loginWithGoogle(tokenId) {
    set({ loading: true, error: null })
    try {
      const res = await api.post<{ token: string }>('/api/v1/user/auth/google-login', { tokenId })
      setAccessToken(res.data.token)
      await get().fetchCurrentUser()
    } catch (err) {
      const msg = extractApiError(err, 'Đăng nhập Google thất bại')
      set({ error: msg })
      throw new Error(msg)
    } finally {
      set({ loading: false })
    }
  },

  async register(request) {
    set({ loading: true, error: null })
    try {
      const res = await api.post<{ message: string }>('/api/v1/user/auth/signup', request)
      return res.data.message ?? 'Đăng ký thành công'
    } catch (err) {
      const msg = extractApiError(err, 'Đăng ký thất bại')
      set({ error: msg })
      throw new Error(msg)
    } finally {
      set({ loading: false })
    }
  },

  async logout() {
    // Local cleanup must always succeed even if the server call fails (offline, 5xx, etc).
    try {
      await api.post('/api/v1/user/auth/logout')
    } catch {
      // swallow — user intent is to log out locally
    }
    clearAccessToken()
    set({ user: null })
  },

  async fetchCurrentUser() {
    const res = await api.get<GetCurrentUserResponse>('/api/v1/user/auth/current')
    set({ user: res.data.userInfo ?? null })
  },

  async changePassword(request) {
    set({ loading: true, error: null })
    try {
      await api.post('/api/v1/user/auth/change-password', request)
    } catch (err) {
      const msg = extractApiError(err, 'Đổi mật khẩu thất bại')
      set({ error: msg })
      throw new Error(msg)
    } finally {
      set({ loading: false })
    }
  },

  async updateProfile(request) {
    set({ loading: true, error: null })
    try {
      await api.post('/api/v1/user/auth/update-profile', request)
      await get().fetchCurrentUser()
    } catch (err) {
      const msg = extractApiError(err, 'Cập nhật profile thất bại')
      set({ error: msg })
      throw new Error(msg)
    } finally {
      set({ loading: false })
    }
  },
}))

// Derived selectors (use these in components instead of decoding tokens repeatedly)
export function selectIsAuthenticated(state: AuthState): boolean {
  return !!state.user && !!getAccessToken()
}

export function selectIsAdmin(state: AuthState): boolean {
  if (!state.user) return false
  const token = getAccessToken()
  if (!token) return false
  return decodeToken(token)?.role === ROLES.ADMIN
}
