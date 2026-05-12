import axios from "axios";
import { defineStore } from "pinia";
import { ref, computed } from "vue";
import authService from "@/services/auth.service";
import { isTokenExpired } from "@/utils/jwt";
import { setAccessToken } from "@/utils/tokenStore";
import type {
  UserInfo,
  LoginRequest,
  SignupRequest,
  ChangePasswordRequest,
} from "@/types/user.types";
import { extractApiError } from "@/types/common.types";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

export const useAuthStore = defineStore("auth", () => {
  // State
  const user = ref<UserInfo | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // Getters
  const isAuthenticated = computed(() => {
    const token = authService.getToken();
    return !!token && !isTokenExpired(token);
  });

  // Actions
  async function login(credentials: LoginRequest) {
    loading.value = true;
    error.value = null;

    try {
      const response = await authService.login(credentials);

      if (response.token) {
        await fetchCurrentUser();
        return true;
      }
      error.value = "Invalid credentials";
      return false;
    } catch (err: unknown) {
      error.value = extractApiError(err, "Login failed. Please try again.");
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function register(userData: SignupRequest) {
    loading.value = true;
    error.value = null;

    try {
      await authService.signup(userData);
      return true;
    } catch (err: unknown) {
      error.value = extractApiError(
        err,
        "Registration failed. Please try again.",
      );
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function fetchCurrentUser() {
    loading.value = true;

    try {
      const response = await authService.getCurrentUser();

      if (response.userInfo) {
        user.value = response.userInfo;
        return true;
      }
      user.value = null;
      return false;
    } catch {
      user.value = null;
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function logout() {
    await authService.logout();
    user.value = null;
  }

  // Silently restore session on page load using the httpOnly refresh_token cookie
  async function initializeAuth() {
    try {
      const response = await axios.post(
        `${API_URL}/api/v1/user/auth/refresh-token`,
        {},
        { withCredentials: true },
      );
      const { token } = response.data;
      if (token && !isTokenExpired(token)) {
        setAccessToken(token);
        await fetchCurrentUser();
      }
    } catch {
      // No valid refresh token — user is not authenticated, that's fine
    }
  }

  async function loginWithGoogle(tokenId: string) {
    loading.value = true;
    error.value = null;

    try {
      const response = await authService.loginWithGoogle(tokenId);

      if (response.token) {
        await fetchCurrentUser();
        return true;
      }
      error.value = "Google login failed";
      return false;
    } catch (err: unknown) {
      error.value = extractApiError(
        err,
        "Google login failed. Please try again.",
      );
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function requestPasswordReset(email: string) {
    loading.value = true;
    error.value = null;

    try {
      await authService.requestPasswordReset(email);
      return true;
    } catch (err: unknown) {
      error.value = extractApiError(
        err,
        "Password reset request failed. Please try again.",
      );
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function resetPassword(token: string, password: string) {
    loading.value = true;
    error.value = null;

    try {
      await authService.resetPassword(token, password);
      return true;
    } catch (err: unknown) {
      error.value = extractApiError(
        err,
        "Password reset failed. Please try again.",
      );
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function changePassword(request: ChangePasswordRequest) {
    loading.value = true;
    error.value = null;

    try {
      await authService.changePassword(request);
      return true;
    } catch (err: unknown) {
      error.value = extractApiError(
        err,
        "Password change failed. Please try again.",
      );
      return false;
    } finally {
      loading.value = false;
    }
  }

  return {
    user,
    loading,
    error,
    isAuthenticated,
    login,
    loginWithGoogle,
    register,
    fetchCurrentUser,
    logout,
    initializeAuth,
    requestPasswordReset,
    resetPassword,
    changePassword,
  };
});
