---
name: frontend-router-auth
description: Use when adding routes, modifying route protection, working with router/index.ts or router/guards.ts, or when implementing auth-related navigation flows. Enforces guard delegation pattern (requireAuth, requireAdmin, redirectIfAuthenticated) — beforeEach must NOT duplicate auth logic.
---

# Frontend Router & Auth — Nihongo IT

## When to invoke

- Adding a new route to `router/index.ts`
- Editing auth/role-checking logic
- When the user says "admin route", "protected route", "redirect after login"
- When `getAccessToken`/`isTokenExpired`/`ROLES` is imported inside `router/index.ts` — that's a violation

## Current pattern (consolidated)

### `router/guards.ts` — centralizes auth logic

Three guards are exported, each with a single responsibility:

```typescript
// guards.ts
export const requireAuth = (to, from, next) => {
  // Check token exists + not expired
  // On failure → redirect to login with ?redirect=originalPath
}

export const requireAdmin = (to, from, next) => {
  // Check token + role === ROLES.ADMIN
  // Token failure → login. Role failure → home
}

export const redirectIfAuthenticated = (to, from, next) => {
  // If already logged in → redirect to home
  // Used on /login and /register
}
```

### `router/index.ts` — NEVER duplicate logic

```typescript
// ✅ Correct — 5-line beforeEach, delegates
import { requireAuth, requireAdmin, redirectIfAuthenticated } from './guards'

router.beforeEach((to, from, next) => {
  if (to.meta.title) document.title = `${to.meta.title} | Nihongo IT`
  if (to.meta.requiresAdmin) return requireAdmin(to, from, next)
  if (to.meta.requiresAuth) return requireAuth(to, from, next)
  return next()
})
```

```typescript
// ❌ Wrong — previously 20+ lines in index.ts copying guards.ts logic
router.beforeEach((to, from, next) => {
  if (to.meta.title) document.title = `${to.meta.title} | Nihongo IT`
  if (to.meta.requiresAuth) {
    const token = getAccessToken()
    if (!token || isTokenExpired(token)) {
      if (token) clearAccessToken()
      return next({ name: 'login', query: { redirect: to.fullPath } })
    }
    if (to.meta.requiresAdmin) {
      const payload = decodeToken(token)
      if (!payload || payload.role !== ROLES.ADMIN) return next({ name: 'home' })
    }
    return next()
  }
  return next()
})
```

## Adding a route — patterns

### Public route

```typescript
{
  path: '/learning/vocabulary',
  name: 'vocabulary',
  component: () => import('@/views/learning/vocabulary/VocabularyView.vue'),
  meta: { title: 'Từ vựng' },
}
```

### Auth-required route (regular user)

```typescript
{
  path: '/profile',
  name: 'profile',
  component: () => import('@/views/auth/ProfileView.vue'),
  meta: { title: 'Hồ sơ', requiresAuth: true },
}
```

### Admin route

```typescript
{
  path: '/admin/users',
  name: 'adminUsers',
  component: () => import('@/views/admin/UserManagementView.vue'),
  meta: { title: 'Quản lý người dùng', requiresAuth: true, requiresAdmin: true },
}
```

`requiresAdmin: true` already implies `requiresAuth` (the guard checks the token itself). Still keep both flags for clarity of intent.

### Login/register routes (redirect if already authenticated)

```typescript
{
  path: '/login',
  name: 'login',
  component: () => import('@/views/auth/LoginView.vue'),
  meta: { title: 'Đăng nhập' },
  beforeEnter: redirectIfAuthenticated,
}
```

`redirectIfAuthenticated` sits on `beforeEnter`, not on a `meta` flag — its logic is the inverse of `requireAuth`.

## Token access — only via `tokenStore`

```typescript
// ✅ Correct
import { getAccessToken, setAccessToken, clearAccessToken } from '@/utils/tokenStore'

const token = getAccessToken()
```

```typescript
// ❌ Wrong — this was the bug fixed in ai.service.ts
const token = localStorage.getItem('auth_token')
```

`tokenStore` encapsulates the storage backend (currently localStorage, possibly cookie/IndexedDB later). Services and components MUST NOT know about the `localStorage` key.

## JWT decoding — only via `utils/jwt`

```typescript
// ✅ Correct
import { decodeToken, isTokenExpired } from '@/utils/jwt'

const payload = decodeToken(token)
if (payload?.role === ROLES.ADMIN) { ... }
```

`payload` is already typed with `role`, `userId`, `email`, `exp`.

## The roles constant

```typescript
// types/roles.ts
export const ROLES = {
  USER: 1,
  ADMIN: 2,
} as const
export type Role = typeof ROLES[keyof typeof ROLES]
```

Do NOT hard-code `roleId === 2`. Always `=== ROLES.ADMIN`.

## Session expiry — handled by the axios interceptor

`utils/sessionHandler.ts` is invoked from the axios response interceptor on 401:
- Shows a warning toast via `useAppToast`
- Clears the token
- Redirects to login

Components do NOT need to handle 401 — the interceptor already does.

## Verify

```bash
grep -n "getAccessToken\|isTokenExpired\|decodeToken\|ROLES" frontend/src/router/index.ts
# Only the import { requireAuth, requireAdmin, redirectIfAuthenticated } line is allowed
# No raw token checks
```
