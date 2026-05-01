import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router';
import { isTokenExpired } from '@/utils/jwt';

export const requireAuth = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext,
) => {
  const token = localStorage.getItem('auth_token');
  if (!token || isTokenExpired(token)) {
    if (token) localStorage.removeItem('auth_token');
    return next({ name: 'login', query: { redirect: to.fullPath } });
  }
  return next();
};

export const redirectIfAuthenticated = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext,
) => {
  const token = localStorage.getItem('auth_token');
  if (token && !isTokenExpired(token)) {
    next({ name: 'home' });
  } else {
    next();
  }
};
