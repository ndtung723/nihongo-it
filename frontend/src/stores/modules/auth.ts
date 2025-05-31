import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import authService from '@/services/auth.service';
import type { UserInfo, LoginRequest, SignupRequest, ChangePasswordRequest } from '@/services/auth.service';

export const useAuthStore = defineStore('auth', () => {
  // State
  const user = ref<UserInfo | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // Getters
  const isAuthenticated = computed(() => {
    const hasToken = !!authService.getToken();
    const hasUser = !!user.value;
    return hasToken;
  });

  // Actions
  async function login(credentials: LoginRequest) {
    loading.value = true;
    error.value = null;

    try {
      const response = await authService.login(credentials);

      if (response.result === 'OK' && response.token) {
        await fetchCurrentUser();
        return true;
      } else {
        error.value = 'Invalid credentials';
        return false;
      }
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
      const response = await authService.signup(userData);
      return response.status === 'OK';
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

      if (response.status === 'OK' && response.userInfo) {
        user.value = response.userInfo;
        return true;
      } else {
        user.value = null;
        return false;
      }
    } catch (err: any) {
      user.value = null;
      return false;
    } finally {
      loading.value = false;
    }
  }

  function logout() {
    authService.removeToken();
    user.value = null;
  }

  // Initialize user if token exists
  function initializeAuth() {
    if (authService.getToken()) {
      fetchCurrentUser();
    }
  }

  async function loginWithGoogle(tokenId: string) {
    loading.value = true;
    error.value = null;

    try {
      const response = await authService.loginWithGoogle(tokenId);

      if (response.result === 'OK' && response.token) {
        await fetchCurrentUser();
        return true;
      } else {
        error.value = response.message || 'Google login failed';
        return false;
      }
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
      const response = await authService.requestPasswordReset(email);
      return response.status === 'OK';
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Password reset request failed. Please try again.';
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function resetPassword(token: string, password: string) {
    loading.value = true;
    error.value = null;

    try {
      const response = await authService.resetPassword(token, password);
      return response.status === 'OK';
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
      const response = await authService.changePassword(request);
      return response.status === 'OK';
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
    changePassword
  };
});
