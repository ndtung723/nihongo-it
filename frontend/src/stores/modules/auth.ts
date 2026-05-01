import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import authService from '@/services/auth.service.ts';
import { isTokenExpired } from '@/utils/jwt';
import type {
  UserInfo,
  LoginRequest,
  SignupRequest,
  ChangePasswordRequest,
} from '@/services/auth.service.ts';

export const useAuthStore = defineStore('auth', () => {
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
      error.value = 'Invalid credentials';
      return false;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Login failed. Please try again.';
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
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Registration failed. Please try again.';
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
    } catch (err: any) {
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

  // Initialize user if token exists and is valid
  function initializeAuth() {
    const token = authService.getToken();
    if (token && !isTokenExpired(token)) {
      fetchCurrentUser();
    } else if (token) {
      authService.removeToken();
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
      error.value = 'Google login failed';
      return false;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Google login failed. Please try again.';
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
    } catch (err: any) {
      error.value =
        err.response?.data?.message || 'Password reset request failed. Please try again.';
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
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Password reset failed. Please try again.';
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
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Password change failed. Please try again.';
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
