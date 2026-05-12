import type { NavigationGuardNext, RouteLocationNormalized } from "vue-router";
import { isTokenExpired, decodeToken } from "@/utils/jwt";
import { getAccessToken, clearAccessToken } from "@/utils/tokenStore";
import { ROLES } from "@/types/roles";

export const requireAuth = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext,
) => {
  const token = getAccessToken();
  if (!token || isTokenExpired(token)) {
    if (token) clearAccessToken();
    return next({ name: "login", query: { redirect: to.fullPath } });
  }
  return next();
};

export const requireAdmin = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext,
) => {
  const token = getAccessToken();
  if (!token || isTokenExpired(token)) {
    if (token) clearAccessToken();
    return next({ name: "login", query: { redirect: to.fullPath } });
  }
  const payload = decodeToken(token);
  if (!payload || payload.role !== ROLES.ADMIN) {
    return next({ name: "home" });
  }
  return next();
};

export const redirectIfAuthenticated = (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext,
) => {
  const token = getAccessToken();
  if (token && !isTokenExpired(token)) {
    next({ name: "home" });
  } else {
    next();
  }
};
