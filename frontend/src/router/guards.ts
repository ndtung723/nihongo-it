import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router';
import authService from '@/services/auth.service';

export const requireAuth = async (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  // Check if token exists first
  if (!authService.isLoggedIn()) {
    // No token, redirect to login
    return next({ name: 'login', query: { redirect: to.fullPath } });
  }

  try {
    // Validate token with the server
    const isValid = await authService.isTokenValid();

    if (isValid) {
      // Token is valid, proceed
      return next();
    } else {
      // Token validation failed, but we don't know if it's because:
      // 1. The token is truly invalid/expired, or
      // 2. The server is down/unreachable

      // Check if the error came from a network error (server down)
      const connectionError = (window.navigator && !window.navigator.onLine);

      if (connectionError) {
        // If it's a connection issue, let the user proceed - we'll handle 401 errors via interceptor if needed
        return next();
      } else {
        // Otherwise assume the token is invalid, clear it, and redirect to login
        authService.removeToken();
        return next({
          name: 'login',
          query: {
            redirect: to.fullPath,
            error: 'Your session has expired. Please log in again.'
          }
        });
      }
    }
  } catch (error) {
    // Error during validation, proceed anyway but may get 401 later
    return next();
  }
};

export const redirectIfAuthenticated = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  if (authService.isLoggedIn()) {
    next({ name: 'home' });
  } else {
    next();
  }
};
