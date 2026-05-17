import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { clearAccessToken, getAccessToken, setAccessToken } from './tokenStore'

const API_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'
const MAX_RETRIES = 3
const RETRY_DELAY_MS = 500

const api = axios.create({
  baseURL: API_URL,
  withCredentials: true, // send httpOnly refresh_token cookie
  timeout: 30000,
})

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// --- Single-flight refresh ---
let isRefreshing = false
let refreshQueue: Array<(token: string) => void> = []

async function tryRefreshToken(): Promise<string | null> {
  try {
    const res = await axios.post<{ token: string }>(
      `${API_URL}/api/v1/user/auth/refresh-token`,
      {},
      { withCredentials: true },
    )
    setAccessToken(res.data.token)
    return res.data.token
  } catch {
    return null
  }
}

function isRetryableServerError(error: AxiosError): boolean {
  const status = error.response?.status
  return !!status && status >= 500 && status < 600
}

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms))

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean; _retryCount?: number })
      | undefined
    if (!original) return Promise.reject(error)

    // 5xx exponential backoff retry
    if (isRetryableServerError(error) && !original._retry) {
      const attempt = original._retryCount ?? 0
      if (attempt < MAX_RETRIES) {
        original._retryCount = attempt + 1
        await sleep(RETRY_DELAY_MS * 2 ** attempt)
        return api(original)
      }
    }

    if (error.response?.status === 401) {
      const url = original.url ?? ''
      const isAuthEndpoint =
        url.includes('/auth/refresh-token') ||
        url.includes('/auth/logout') ||
        url.includes('/auth/login')
      const isTts =
        url.includes('/tts/') || original.headers?.['X-Content-Language'] === 'ja'

      if (isTts) return Promise.reject(error)

      if (!isAuthEndpoint && !original._retry) {
        if (isRefreshing) {
          return new Promise((resolve) => {
            refreshQueue.push((newToken) => {
              original.headers.Authorization = `Bearer ${newToken}`
              original._retry = true
              resolve(api(original))
            })
          })
        }

        original._retry = true
        isRefreshing = true

        try {
          const newToken = await tryRefreshToken()
          if (newToken) {
            refreshQueue.forEach((cb) => cb(newToken))
            refreshQueue = []
            isRefreshing = false
            original.headers.Authorization = `Bearer ${newToken}`
            return api(original)
          }
        } catch {
          // fall through to session expiration
        }

        refreshQueue = []
        isRefreshing = false
        clearAccessToken()
        if (typeof window !== 'undefined') {
          const redirect = encodeURIComponent(window.location.pathname)
          window.location.href = `/login?redirect=${redirect}`
        }
      }
    }

    return Promise.reject(error)
  },
)

export default api
