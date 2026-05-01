import axios from 'axios';
import { handleSessionExpiration } from './sessionHandler';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_URL,
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

// --- Token refresh state ---
let isRefreshing = false;
let refreshQueue: Array<(token: string) => void> = [];

async function tryRefreshToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refresh_token');
  if (!refreshToken) return null;

  try {
    // Use raw axios (not api instance) to avoid interceptor loop
    const response = await axios.post(`${API_URL}/api/v1/user/auth/refresh-token`, { refreshToken });
    const { token, refreshToken: newRefreshToken } = response.data;
    localStorage.setItem('auth_token', token);
    localStorage.setItem('refresh_token', newRefreshToken);
    return token;
  } catch {
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    if (error.response?.status === 401) {
      const url: string = originalRequest?.url ?? '';

      const isTtsRequest =
        url.includes('/api/v1/tts/') ||
        url.includes('/api/v1/ai/tts/') ||
        url.includes('/tts/') ||
        originalRequest?.headers?.['X-Content-Language'] === 'ja';

      const isAuthEndpoint =
        url.includes('/auth/refresh-token') ||
        url.includes('/auth/logout') ||
        url.includes('/auth/login');

      if (isTtsRequest) return Promise.reject(error);

      if (!isAuthEndpoint && !originalRequest._retry) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            refreshQueue.push((newToken) => {
              originalRequest.headers.Authorization = `Bearer ${newToken}`;
              originalRequest._retry = true;
              resolve(api(originalRequest));
            });
          });
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
          const newToken = await tryRefreshToken();
          if (newToken) {
            refreshQueue.forEach((cb) => cb(newToken));
            refreshQueue = [];
            isRefreshing = false;
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return api(originalRequest);
          }
        } catch {
          // fall through to session expiration
        }

        refreshQueue = [];
        isRefreshing = false;
        localStorage.removeItem('auth_token');
        localStorage.removeItem('refresh_token');
        handleSessionExpiration(window.location.pathname);
      }
    }

    return Promise.reject(error);
  },
);

export default api;
