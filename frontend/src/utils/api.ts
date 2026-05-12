import axios from "axios";
import type { AxiosError, InternalAxiosRequestConfig } from "axios";
import { handleSessionExpiration } from "./sessionHandler";
import { getAccessToken, setAccessToken, clearAccessToken } from "./tokenStore";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

const MAX_RETRIES = 3;
const RETRY_DELAY_MS = 500;

function isRetryable(error: AxiosError): boolean {
  const status = error.response?.status;
  return !!status && status >= 500 && status < 600;
}

function getRetryDelay(attempt: number): number {
  return RETRY_DELAY_MS * 2 ** attempt;
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

const api = axios.create({
  baseURL: API_URL,
  withCredentials: true, // send httpOnly refresh_token cookie on auth endpoints
});

api.interceptors.request.use(
  (config) => {
    const token = getAccessToken();
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
  try {
    // Refresh token is sent automatically via the httpOnly cookie
    const response = await axios.post(
      `${API_URL}/api/v1/user/auth/refresh-token`,
      {},
      { withCredentials: true },
    );
    const { token } = response.data;
    setAccessToken(token);
    return token;
  } catch {
    return null;
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & {
      _retry?: boolean;
      _retryCount?: number;
    };

    if (isRetryable(error) && originalRequest && !originalRequest._retry) {
      const attempt = originalRequest._retryCount ?? 0;
      if (attempt < MAX_RETRIES) {
        originalRequest._retryCount = attempt + 1;
        await sleep(getRetryDelay(attempt));
        return api(originalRequest);
      }
    }

    if (!originalRequest) return Promise.reject(error);

    if (error.response?.status === 401) {
      const url: string = originalRequest?.url ?? "";

      const isTtsRequest =
        url.includes("/api/v1/tts/") ||
        url.includes("/api/v1/ai/tts/") ||
        url.includes("/tts/") ||
        originalRequest?.headers?.["X-Content-Language"] === "ja";

      const isAuthEndpoint =
        url.includes("/auth/refresh-token") ||
        url.includes("/auth/logout") ||
        url.includes("/auth/login");

      if (isTtsRequest) return Promise.reject(error);

      if (!isAuthEndpoint && !originalRequest._retry) {
        if (isRefreshing) {
          return new Promise((resolve) => {
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
        clearAccessToken();
        handleSessionExpiration(window.location.pathname);
      }
    }

    return Promise.reject(error);
  },
);

export default api;
