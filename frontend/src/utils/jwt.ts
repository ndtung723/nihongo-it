import { jwtDecode } from 'jwt-decode';
import { ROLES } from '@/types/roles';

export interface JwtPayload {
  sub: string;
  userId: string;
  role: number; // 1 = ADMIN, 2 = USER
  email: string;
  fullName: string;
  profilePicture?: string;
  currentLevel?: string;
  jlptGoal?: string;
  lastLogin?: string;
  exp: number;
  iat: number;
}

export function decodeToken(token: string): JwtPayload | null {
  try {
    return jwtDecode<JwtPayload>(token);
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodeToken(token);
  if (!payload) return true;
  return Date.now() >= payload.exp * 1000;
}

export function getStoredPayload(): JwtPayload | null {
  const token = localStorage.getItem('auth_token');
  if (!token) return null;
  return decodeToken(token);
}

export function isAdmin(): boolean {
  const payload = getStoredPayload();
  return payload?.role === ROLES.ADMIN;
}
