import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('@/lib/api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

import api from '@/lib/api'
import { useAuthStore } from '../auth.store'
import { clearAccessToken, getAccessToken } from '@/lib/tokenStore'

const mockedApi = api as unknown as {
  post: ReturnType<typeof vi.fn>
  get: ReturnType<typeof vi.fn>
}

const sampleUser = {
  userId: 'u1',
  email: 'test@example.com',
  fullName: 'Test',
  roleId: 2,
}

beforeEach(() => {
  // Reset Zustand store state between tests
  useAuthStore.setState({ user: null, loading: false, error: null, initialized: false })
  clearAccessToken()
  vi.clearAllMocks()
})

describe('auth.store', () => {
  describe('login', () => {
    it('stores token and fetches user on success', async () => {
      mockedApi.post.mockResolvedValueOnce({ data: { token: 'jwt-abc' } })
      mockedApi.get.mockResolvedValueOnce({ data: { status: 'OK', userInfo: sampleUser } })

      await useAuthStore.getState().login({ email: 'test@example.com', password: 'pw' })

      expect(getAccessToken()).toBe('jwt-abc')
      expect(useAuthStore.getState().user).toEqual(sampleUser)
      expect(useAuthStore.getState().error).toBeNull()
    })

    it('sets error message and throws on failure', async () => {
      mockedApi.post.mockRejectedValueOnce({
        response: { data: { message: 'Sai mật khẩu' } },
      })

      await expect(
        useAuthStore.getState().login({ email: 'x@y.com', password: 'wrong' }),
      ).rejects.toThrow('Sai mật khẩu')
      expect(useAuthStore.getState().error).toBe('Sai mật khẩu')
      expect(useAuthStore.getState().user).toBeNull()
    })
  })

  describe('logout', () => {
    it('clears user and token even if server call fails', async () => {
      useAuthStore.setState({ user: sampleUser })
      mockedApi.post.mockRejectedValueOnce(new Error('network'))

      await useAuthStore.getState().logout()

      expect(useAuthStore.getState().user).toBeNull()
      expect(getAccessToken()).toBeNull()
    })
  })

  describe('initialize', () => {
    it('restores session when refresh token is valid', async () => {
      mockedApi.post.mockResolvedValueOnce({ data: { token: 'refreshed-jwt' } })
      mockedApi.get.mockResolvedValueOnce({ data: { status: 'OK', userInfo: sampleUser } })

      await useAuthStore.getState().initialize()

      expect(getAccessToken()).toBe('refreshed-jwt')
      expect(useAuthStore.getState().user).toEqual(sampleUser)
      expect(useAuthStore.getState().initialized).toBe(true)
    })

    it('silently clears state when refresh fails', async () => {
      mockedApi.post.mockRejectedValueOnce(new Error('no cookie'))

      await useAuthStore.getState().initialize()

      expect(useAuthStore.getState().user).toBeNull()
      expect(useAuthStore.getState().initialized).toBe(true)
    })

    it('is idempotent — second call does not re-fetch', async () => {
      mockedApi.post.mockResolvedValueOnce({ data: { token: 'jwt' } })
      mockedApi.get.mockResolvedValueOnce({ data: { status: 'OK', userInfo: sampleUser } })

      await useAuthStore.getState().initialize()
      await useAuthStore.getState().initialize()

      expect(mockedApi.post).toHaveBeenCalledTimes(1)
    })
  })

  describe('register', () => {
    it('returns server message on success', async () => {
      mockedApi.post.mockResolvedValueOnce({ data: { message: 'Vui lòng kiểm tra email' } })

      const msg = await useAuthStore.getState().register({
        email: 'new@x.com',
        password: 'pw',
        fullName: 'New',
      })

      expect(msg).toBe('Vui lòng kiểm tra email')
    })
  })
})
