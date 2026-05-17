# Next.js Migration Plan — Nihongo IT Frontend

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the existing Vue 3 + Vuetify + Pinia frontend to two independent Next.js 16 apps (`frontend-user/` and `frontend-admin/`) using Tailwind + shadcn/ui in place of Vuetify/SCSS, while preserving every existing feature and the JWT auth flow (httpOnly refresh cookie + in-memory access token).

**Status:** Phase 1 ✅ complete (2026-05-17) · Phase 2 ✅ complete (2026-05-17) · Phase 3 ⏳ in progress.

**Architecture:**
- Two standalone Next.js **16.2.6** apps (App Router, TypeScript, React 19.2). No monorepo — each app owns its own `package.json`, types, and api client (duplicated, like `sample_project/`).
- shadcn/ui + Radix primitives + Tailwind CSS 4 for UI. Each shadcn component is copy-pasted into `src/components/ui/` so we own the source.
- Zustand for global state (closest to Pinia composition API). Server Components for static lists, Client Components for interactive views.
- Axios instance with request/response interceptors mirroring current `src/utils/api.ts` (Bearer token attach, 401 → refresh → retry).
- **Proxy-based** route protection (`proxy.ts`, NOT `middleware.ts` — renamed in Next.js 16) replacing `router.beforeEach` + `requireAuth/requireAdmin` guards.
- Backend API unchanged — both Next.js apps hit the same `http://localhost:8080` gateway.

### Next.js 16 breaking changes that affect this plan

Discovered during Phase 1 scaffolding. **Every subsequent task must follow these rules:**

| Change | Implication |
|---|---|
| `middleware.ts` → `proxy.ts` | File at app root is `proxy.ts`. Function exported as `proxy`, not `middleware`. Runtime is Node.js (not Edge). |
| Turbopack is default | No `--turbopack` flag needed in scripts. `next dev` and `next build` already use it. |
| `next lint` removed | Use ESLint CLI directly (`eslint .`). Scaffold's `package.json` reflects this. |
| Async Request APIs are MANDATORY | `cookies()`, `headers()`, `draftMode()`, `params`, `searchParams` ALL return Promises. Must `await` them. Use `PageProps<'/route'>`, `LayoutProps<'/route'>`, `RouteContext<'/route'>` type helpers (generate via `npx next typegen`). |
| Parallel routes require `default.js` | Not relevant — we don't use parallel routes. |
| `images.domains` deprecated | Use `images.remotePatterns` if loading remote images. |
| ESLint Flat Config default | Scaffold uses `eslint.config.mjs` already. |
| Node.js 20.9+ required | Project uses Node 24, fine. |

### React 19 + React Compiler lint rules (Phase 2 discoveries)

Next.js 16 ships with new React Compiler-aware lint rules that flag patterns common in older React code. Affect every component/hook from Phase 2 onwards:

| Rule | What it flags | How we handle it |
|---|---|---|
| `react-hooks/use-memo` | `useCallback`/`useMemo` with non-literal dep array (e.g. `}, deps)`) | Caller-provided dep arrays don't pass. Force callers to memoize inputs themselves. Our `useAsyncData` now takes a stable `fetcher` (caller wraps with `useCallback`) instead of accepting `deps: unknown[]`. |
| `react-hooks/set-state-in-effect` | `setState()` called inside `useEffect` body | For fetch-on-mount hooks (like `useAsyncData`), suppress with `// eslint-disable-next-line react-hooks/set-state-in-effect` immediately above the offending line, with a comment explaining intent. |
| Disable comment placement | Must be on the same logical line as the offending call, NOT on the `useEffect(...)` wrapper | When suppressing, place the comment right above `void run()`, not above `useEffect(() => {`. |

### shadcn / sonner setup quirks (Phase 2)

| Discovery | Action |
|---|---|
| `shadcn add` registry no longer has `toast` — only `sonner` | Add `sonner` directly. Plan's Task 1.3 was updated to use `sonner` (already correct in current plan). |
| Generated `src/components/ui/sonner.tsx` imports from `next-themes` | Must install `next-themes` (shadcn auto-installs it) AND wrap root in `<ThemeProvider attribute="class" defaultTheme="light" enableSystem={false}>`. Otherwise `useTheme()` throws. |
| `shadcn add calendar` generates a `table: "..."` key in classNames that `react-day-picker@10` doesn't accept | Delete the `table:` line from the generated `calendar.tsx`. |

### Zustand vs Pinia API differences (Phase 2)

| Pinia (Vue) | Zustand (React) | Note |
|---|---|---|
| Composition API `defineStore(name, () => { ... })` with `computed()` getters | `create<State>((set, get) => ({ ... }))` — no built-in computed | For derived values, export named **selector functions** (`selectIsAuthenticated`, `selectIsAdmin`) and call as `useAuthStore(selectIsAuthenticated)`. This also gives subscribers granular re-render control. |
| `storeToRefs(store)` for destructured reactivity | Pass selector to hook: `const user = useAuthStore(s => s.user)` | Avoid `const { user } = useAuthStore()` — re-renders on any state change. |
| `$reset()` built-in | None | If needed, expose an explicit `reset` action that calls `set(initialState)`. |
| `$onAction` for side effects | None | Subscribe with `useAuthStore.subscribe(...)` outside React. |

**Logout pattern lesson:** Local cleanup must always succeed regardless of server response. Use `try { post } catch {} finally cleanup` — never re-throw a logout failure to the caller, or the UI can get stuck in a stale auth state when offline.

**Tech Stack (decisions locked):**
| Concern | Vue (current) | Next.js (target) | Reason |
|---|---|---|---|
| Framework | Vue 3 + Vite | Next.js **16.2.6** + App Router | Modern standard, layouts, proxy |
| Language | TypeScript 5.8 | TypeScript 5 + React 19.2 | Latest stable |
| UI library | Vuetify 3 | shadcn/ui + Radix UI | Full control, Tailwind-native |
| Styling | SCSS + Vuetify theme | Tailwind CSS 4 | User explicitly requested |
| State | Pinia | Zustand | Closest API to Pinia composition |
| Forms | vee-validate + yup | react-hook-form + zod | Better TS inference than yup |
| HTTP | axios | axios | Reuse interceptor pattern |
| Routing | vue-router | App Router | Built into Next.js |
| Toast | vue-toast-notification | sonner | Modern, accessible, low API surface |
| Charts | chart.js + vue-chartjs | chart.js + react-chartjs-2 | Reuse chart.js knowledge |
| Drag-drop | vuedraggable | @dnd-kit/sortable | Modern, accessible, headless |
| Google OAuth | vue3-google-login | @react-oauth/google | Same library, React wrapper |
| JWT decode | jwt-decode | jwt-decode | Framework-agnostic |
| Speech SDK | microsoft-cognitiveservices-speech-sdk | (same) | Works in browser, no wrapper needed |
| Audio record | recorder-js | recorder-js | Works fine in React |
| Japanese text | kuroshiro + wanakana | (same) | Framework-agnostic |
| Date | date-fns | date-fns | Framework-agnostic |
| Validation | yup | zod | Modern, better TS |
| i18n | vue-i18n (unused) | (skip) | Not active, defer to later |

**Out of scope:**
- Backend changes — gateway/services stay as-is.
- The existing `frontend/` directory — keep it running in parallel until cutover (Phase 12). No deletions until then.
- New features — strictly migrate existing functionality.
- i18n — vue-i18n is installed but not used; defer to a separate initiative.

---

## File Structure (target)

```
nihongo-it/
├── frontend/                  ← existing Vue app, kept running until Phase 12
├── frontend-user/             ← NEW — Next.js 15 user app (port 3000)
│   ├── src/
│   │   ├── app/
│   │   │   ├── (public)/              ← group: no auth required
│   │   │   │   ├── page.tsx           ← HomeView
│   │   │   │   ├── furigana/page.tsx  ← FuriganaView
│   │   │   │   └── translation/page.tsx
│   │   │   ├── (auth)/                ← group: redirect-if-authenticated
│   │   │   │   ├── login/page.tsx
│   │   │   │   ├── register/page.tsx
│   │   │   │   ├── forgot-password/page.tsx
│   │   │   │   └── reset-password/page.tsx
│   │   │   ├── (app)/                 ← group: requireAuth
│   │   │   │   ├── layout.tsx         ← Header + nav
│   │   │   │   ├── profile/page.tsx
│   │   │   │   ├── account/
│   │   │   │   │   ├── settings/page.tsx
│   │   │   │   │   ├── notifications/page.tsx
│   │   │   │   │   └── change-password/page.tsx
│   │   │   │   ├── vocabulary/
│   │   │   │   │   ├── page.tsx                  ← VocabularyView
│   │   │   │   │   ├── learning/page.tsx         ← VocabularyLearningView
│   │   │   │   │   ├── saved/page.tsx            ← VocabularyStorageView
│   │   │   │   │   ├── category/[id]/page.tsx    ← CategoryDetailView
│   │   │   │   │   ├── topic/[id]/page.tsx       ← VocabularyTopicView
│   │   │   │   │   └── [id]/page.tsx             ← VocabularyDetailView
│   │   │   │   ├── conversation/
│   │   │   │   │   ├── page.tsx                  ← ConversationLearningView
│   │   │   │   │   └── [id]/practice/page.tsx    ← ConversationPracticeView
│   │   │   │   ├── flashcards/
│   │   │   │   │   ├── study/page.tsx            ← FlashcardStudyView
│   │   │   │   │   └── stats/page.tsx            ← FlashcardStatsView
│   │   │   │   ├── speech/page.tsx               ← SpeechAnalyzer
│   │   │   │   └── statistics/page.tsx           ← StatisticsView
│   │   │   ├── layout.tsx                        ← root layout
│   │   │   ├── error.tsx
│   │   │   ├── not-found.tsx
│   │   │   └── loading.tsx
│   │   ├── components/
│   │   │   ├── ui/                    ← shadcn copy-pasted (button.tsx, dialog.tsx, etc.)
│   │   │   ├── layout/                ← Header.tsx, Footer.tsx, Nav.tsx
│   │   │   ├── flashcard/             ← FlashcardReview.tsx, RatingButtons.tsx
│   │   │   ├── vocabulary/            ← VocabularyCard.tsx, VocabularyFilter.tsx
│   │   │   ├── conversation/          ← ConversationPlayer.tsx, AudioRecorder.tsx
│   │   │   └── common/                ← ConfirmDialog.tsx, Loader.tsx, ErrorBoundary
│   │   ├── stores/                    ← Zustand stores
│   │   │   ├── auth.store.ts
│   │   │   ├── vocabulary.store.ts
│   │   │   ├── flashcards.store.ts
│   │   │   └── notifications.store.ts
│   │   ├── services/                  ← axios wrappers (same naming as Vue)
│   │   │   ├── auth.service.ts
│   │   │   ├── vocabulary.service.ts
│   │   │   ├── flashcard.service.ts
│   │   │   ├── conversation.service.ts
│   │   │   ├── ai.service.ts
│   │   │   ├── user.service.ts
│   │   │   ├── statistics.service.ts
│   │   │   ├── notification.service.ts
│   │   │   ├── feedback.service.ts
│   │   │   └── speechRecognition.service.ts
│   │   ├── hooks/                     ← React hooks (replaces composables/)
│   │   │   ├── useAppToast.ts
│   │   │   ├── useDebounce.ts
│   │   │   ├── useAsyncData.ts
│   │   │   ├── useConfirm.ts
│   │   │   ├── usePagination.ts
│   │   │   └── useAuth.ts
│   │   ├── lib/
│   │   │   ├── api.ts                 ← axios instance + interceptors
│   │   │   ├── tokenStore.ts          ← in-memory access token
│   │   │   ├── jwt.ts                 ← decode + isExpired
│   │   │   ├── extractApiError.ts
│   │   │   ├── cn.ts                  ← clsx + tailwind-merge helper
│   │   │   └── japanese.ts            ← kuroshiro/wanakana wrappers
│   │   ├── types/
│   │   │   ├── common.types.ts        ← UUID, DateString, JlptLevel, PagedResponse
│   │   │   ├── user.types.ts
│   │   │   ├── learning.types.ts
│   │   │   ├── conversation.types.ts
│   │   │   ├── ai.types.ts
│   │   │   ├── notification.types.ts
│   │   │   └── progress.types.ts
│   │   └── schemas/                   ← zod validation schemas
│   │       ├── auth.schema.ts
│   │       └── profile.schema.ts
│   ├── public/
│   ├── proxy.ts                  ← auth check at edge
│   ├── next.config.mjs
│   ├── tailwind.config.ts
│   ├── tsconfig.json
│   └── package.json
│
└── frontend-admin/            ← NEW — Next.js 15 admin app (port 3001)
    ├── src/
    │   ├── app/
    │   │   ├── (auth)/login/page.tsx        ← admin login (separate)
    │   │   ├── (admin)/
    │   │   │   ├── layout.tsx               ← Sidebar + Header
    │   │   │   ├── page.tsx                 ← DashboardView
    │   │   │   ├── users/
    │   │   │   │   ├── page.tsx             ← UsersView
    │   │   │   │   └── [id]/page.tsx        ← UserDetailView
    │   │   │   ├── categories/page.tsx      ← CategoryManagementView
    │   │   │   ├── topics/page.tsx          ← TopicManagementView
    │   │   │   ├── vocabulary/page.tsx      ← VocabularyManagementView
    │   │   │   ├── conversations/
    │   │   │   │   ├── page.tsx             ← ConversationManagementView
    │   │   │   │   ├── [id]/page.tsx        ← ConversationDetailView
    │   │   │   │   └── [id]/edit/page.tsx   ← ConversationLinesEditView
    │   │   │   └── statistics/
    │   │   │       ├── page.tsx             ← StatisticsOverviewView
    │   │   │       ├── users/page.tsx       ← UserStatisticsView
    │   │   │       └── users/[id]/page.tsx  ← UserStatisticsDetailView
    │   │   ├── layout.tsx
    │   │   ├── error.tsx
    │   │   └── not-found.tsx
    │   ├── components/
    │   │   ├── ui/
    │   │   ├── layout/                ← Sidebar.tsx, AdminHeader.tsx
    │   │   ├── data-table/            ← reusable table (TanStack Table)
    │   │   └── admin/
    │   ├── stores/                    ← Zustand
    │   │   ├── auth.store.ts
    │   │   └── admin.store.ts
    │   ├── services/                  ← admin-scoped service files
    │   ├── hooks/
    │   ├── lib/
    │   ├── types/
    │   └── schemas/
    ├── public/
    ├── proxy.ts                  ← requires ADMIN role
    ├── next.config.mjs
    ├── tailwind.config.ts
    └── package.json
```

---

## Phased Execution Order

The plan is split into **12 phases**. Phase 1-2 are foundation and MUST be done first. Phases 3-7 (user app features) and 8-11 (admin features) can be done in any order, but recommended top-to-bottom. Phase 12 is the cutover and is irreversible.

**Estimated effort (rough):** 6-8 weeks of focused work for a single experienced engineer, OR 3-4 weeks with subagent-driven parallel execution per feature.

```
Phase 1  → Scaffold both Next.js apps (1 day)
Phase 2  → Shared foundation: api, auth, tokenStore, middleware, layouts (3-4 days)
Phase 3  → Auth flow: login, register, password reset, Google OAuth (2-3 days)
Phase 4  → User app: Vocabulary feature (browse, detail, AI chat, saved) (4-5 days)
Phase 5  → User app: Flashcard study + stats (2-3 days)
Phase 6  → User app: Conversation practice + speech analysis (4-5 days)
Phase 7  → User app: Tools (Furigana, Translation, Speech, Statistics) (2-3 days)
Phase 8  → Admin app: Dashboard + Users CRUD (2-3 days)
Phase 9  → Admin app: Category/Topic/Vocabulary CRUD (3-4 days)
Phase 10 → Admin app: Conversation management + drag-drop editor (3-4 days)
Phase 11 → Admin app: Statistics views (2 days)
Phase 12 → Cutover: docker, reverse proxy, decommission old frontend (1-2 days)
```

---

## Verification After Each Phase

After every phase, run in each affected app directory:

```bash
npm run type-check    # tsc --noEmit, must be 0 errors
npm run lint          # next lint, must be 0 errors
npm run build         # next build, must succeed
```

Manual smoke test checklist updated per phase. **Never merge a phase to main with build/type errors.**

---

# Phase 1: Scaffold Both Next.js Apps ✅ COMPLETED 2026-05-17

**Goal:** Create the two app skeletons with Tailwind, shadcn/ui, ESLint, Prettier, and a working `hello world` page.

**Outcome:**
- Branch `feature/nextjs-migration` created
- `frontend-user/` (port 3000) and `frontend-admin/` (port 3001) scaffolded with Next.js 16.2.6, React 19.2, Tailwind 4
- shadcn/ui initialized with Radix base; 19 components in user app, 24 in admin (extras: table, sheet, command, popover, calendar)
- 3 commits: `0ea983f` (user scaffold), `e457a6b` (admin scaffold), `a2df9b3` (shadcn components)

**Files:**
- Create: `frontend-user/` (entire scaffolded directory)
- Create: `frontend-admin/` (entire scaffolded directory)

### Task 1.1: Scaffold `frontend-user/`

- [x] **Step 1: Run create-next-app**

```bash
cd D:/workspace/nihongo-it
npx create-next-app@latest frontend-user --typescript --tailwind --app --eslint --src-dir --import-alias "@/*" --no-turbopack
```

When prompted for "Would you like to customize the import alias?": yes, use `@/*`.

- [ ] **Step 2: Verify scaffold runs**

```bash
cd frontend-user && npm run dev
```

Expected: dev server starts at `http://localhost:3000`, default Next.js home renders.

- [ ] **Step 3: Initialize shadcn/ui**

```bash
npx shadcn@latest init
```

Choose: TypeScript, Default style, Slate base color, `src/app/globals.css`, CSS variables for colors, `@/components`, `@/lib/utils`, React Server Components yes.

Expected output: `components.json` created, `src/lib/utils.ts` created (with `cn()` helper), `src/app/globals.css` updated with Tailwind theme tokens.

- [ ] **Step 4: Install foundational dependencies**

```bash
npm install axios zustand react-hook-form @hookform/resolvers zod sonner jwt-decode date-fns clsx tailwind-merge
npm install -D @types/node prettier prettier-plugin-tailwindcss eslint-config-prettier
```

- [ ] **Step 5: Add Prettier config**

Create `frontend-user/.prettierrc.json`:

```json
{
  "semi": false,
  "singleQuote": true,
  "trailingComma": "all",
  "printWidth": 100,
  "plugins": ["prettier-plugin-tailwindcss"]
}
```

Create `frontend-user/.prettierignore`:

```
node_modules
.next
out
public
```

- [ ] **Step 6: Add base scripts to package.json**

Edit `frontend-user/package.json` scripts:

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "type-check": "tsc --noEmit",
    "format": "prettier --write \"src/**/*.{ts,tsx,css}\""
  }
}
```

- [ ] **Step 7: Verify type-check + build pass**

```bash
npm run type-check && npm run build
```

Expected: both pass with 0 errors.

- [ ] **Step 8: Commit**

```bash
git add frontend-user/
git commit -m "feat(frontend-user): scaffold Next.js 15 app with Tailwind + shadcn/ui"
```

### Task 1.2: Scaffold `frontend-admin/`

- [ ] **Step 1: Repeat Task 1.1 steps for `frontend-admin/`**

Same commands as Task 1.1 but in directory `frontend-admin/`. After scaffold runs, edit `frontend-admin/package.json` to add `"dev": "next dev -p 3001"` so it runs on a different port from the user app.

- [ ] **Step 2: Verify**

```bash
cd frontend-admin && npm run dev
```

Expected: starts at `http://localhost:3001`.

- [ ] **Step 3: Commit**

```bash
git add frontend-admin/
git commit -m "feat(frontend-admin): scaffold Next.js 15 app with Tailwind + shadcn/ui"
```

### Task 1.3: Add shared shadcn components used everywhere

For BOTH apps, run:

- [ ] **Step 1: Add base components**

```bash
cd frontend-user
npx shadcn@latest add button input label form dialog dropdown-menu select textarea checkbox switch tabs card avatar badge toast skeleton separator alert
```

Repeat in `frontend-admin/` plus admin-specific:

```bash
cd ../frontend-admin
npx shadcn@latest add button input label form dialog dropdown-menu select textarea checkbox switch tabs card avatar badge toast skeleton separator alert
npx shadcn@latest add table sheet command popover calendar
```

- [ ] **Step 2: Commit**

```bash
git add frontend-user/src/components/ui frontend-admin/src/components/ui
git commit -m "feat(ui): add shadcn base components to both apps"
```

---

# Phase 2: Shared Foundation (per-app) ✅ COMPLETED 2026-05-17

**Goal:** Each app has working axios + token store + auth proxy + root layout. Done in parallel for both apps (same code, duplicated).

**Apply every task in this phase to BOTH `frontend-user/` and `frontend-admin/`.** The diffs between them are minimal — admin's proxy requires ADMIN role, user's does not.

**Outcome:**
- 7 type files ported from Vue to both apps
- `lib/tokenStore.ts`, `lib/jwt.ts`, `lib/api.ts` (single-flight refresh + 5xx exp-backoff retry)
- Root layout + `Providers` with `ThemeProvider` + `Toaster` + `ConfirmProvider`
- 5 hooks: `useAppToast`, `useDebounce`, `useAsyncData`, `useConfirm` (provider+hook), `usePagination`
- Vitest setup; 11 passing tests (tokenStore × 4, auth.store × 7)
- `proxy.ts` per app — user has 7 public paths, admin has only `/login`
- Zustand auth store with login/logout/initialize/register/loginWithGoogle/changePassword/updateProfile
- Verification on both apps: type-check ✓ · lint ✓ · 11/11 tests ✓ · build ✓
- 1 commit: `4955fef` (55 files changed, +5871 −196)

### Task 2.1: Port type definitions

**Files (per app):**
- Create: `src/types/common.types.ts`
- Create: `src/types/user.types.ts`
- Create: `src/types/learning.types.ts`
- Create: `src/types/conversation.types.ts`
- Create: `src/types/ai.types.ts`
- Create: `src/types/notification.types.ts`
- Create: `src/types/progress.types.ts`

- [ ] **Step 1: Copy types verbatim from Vue project**

Source: `frontend/src/types/*.types.ts`. Copy each file as-is — they are framework-agnostic TypeScript. The `extractApiError(err, fallback)` helper in `common.types.ts` is reused everywhere.

- [ ] **Step 2: Type-check**

```bash
npm run type-check
```

Expected: 0 errors.

- [ ] **Step 3: Commit (after both apps done)**

```bash
git add frontend-user/src/types frontend-admin/src/types
git commit -m "feat(types): port shared type definitions to both Next.js apps"
```

### Task 2.2: Build token store + JWT utils

**Files:**
- Create: `src/lib/tokenStore.ts`
- Create: `src/lib/jwt.ts`
- Test: `src/lib/__tests__/tokenStore.test.ts` (after vitest setup in Task 2.6)

- [ ] **Step 1: Write `tokenStore.ts` — in-memory access token**

```typescript
// src/lib/tokenStore.ts
let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}

export function clearAccessToken(): void {
  accessToken = null
}
```

- [ ] **Step 2: Write `jwt.ts` — decode + expiry check**

```typescript
// src/lib/jwt.ts
import { jwtDecode } from 'jwt-decode'

export interface JwtPayload {
  sub: string
  email: string
  role: number
  exp: number
  iat: number
}

export function decodeToken(token: string): JwtPayload | null {
  try {
    return jwtDecode<JwtPayload>(token)
  } catch {
    return null
  }
}

export function isTokenExpired(token: string): boolean {
  const payload = decodeToken(token)
  if (!payload) return true
  return payload.exp * 1000 < Date.now()
}
```

- [ ] **Step 3: Commit**

```bash
git add src/lib/tokenStore.ts src/lib/jwt.ts
git commit -m "feat(auth): add tokenStore + JWT utils"
```

### Task 2.3: Build axios client with refresh interceptor

**Files:**
- Create: `src/lib/api.ts`
- Create: `src/lib/extractApiError.ts` (or move from `types/common.types.ts`)

- [ ] **Step 1: Write the axios instance**

```typescript
// src/lib/api.ts
import axios, { AxiosError, AxiosRequestConfig } from 'axios'
import { getAccessToken, setAccessToken, clearAccessToken } from './tokenStore'

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true,  // send httpOnly refresh cookie
  timeout: 30000,
})

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Refresh-token rotation logic — single-flight to prevent races
let refreshPromise: Promise<string> | null = null

async function refreshAccessToken(): Promise<string> {
  if (refreshPromise) return refreshPromise
  refreshPromise = axios
    .post<{ token: string }>(`${BASE_URL}/api/v1/user/auth/refresh-token`, {}, { withCredentials: true })
    .then((res) => {
      setAccessToken(res.data.token)
      return res.data.token
    })
    .finally(() => {
      refreshPromise = null
    })
  return refreshPromise
}

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as AxiosRequestConfig & { _retry?: boolean }
    if (error.response?.status === 401 && !original._retry && !original.url?.includes('/auth/')) {
      original._retry = true
      try {
        const newToken = await refreshAccessToken()
        if (original.headers) {
          original.headers.Authorization = `Bearer ${newToken}`
        }
        return api(original)
      } catch (refreshErr) {
        clearAccessToken()
        if (typeof window !== 'undefined') {
          window.location.href = '/login'
        }
        return Promise.reject(refreshErr)
      }
    }
    return Promise.reject(error)
  },
)

export default api
```

- [ ] **Step 2: Add `.env.local` template**

Create `frontend-user/.env.local.example`:

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
NEXT_PUBLIC_GOOGLE_CLIENT_ID=
NEXT_PUBLIC_MS_SPEECH_KEY=
NEXT_PUBLIC_MS_SPEECH_REGION=
```

Same file in `frontend-admin/.env.local.example`. Add both to `.gitignore` exceptions (the `.example` is committed).

- [ ] **Step 3: Commit**

```bash
git add src/lib/api.ts .env.local.example
git commit -m "feat(api): axios client with refresh-token rotation"
```

### Task 2.4: Build root layout + providers

**Files:**
- Modify: `src/app/layout.tsx`
- Create: `src/app/providers.tsx`

- [ ] **Step 1: Write providers wrapper**

```typescript
// src/app/providers.tsx
'use client'

import { Toaster } from 'sonner'
import { ReactNode } from 'react'

export function Providers({ children }: { children: ReactNode }) {
  return (
    <>
      {children}
      <Toaster position="top-right" richColors closeButton />
    </>
  )
}
```

- [ ] **Step 2: Wire into root layout**

```typescript
// src/app/layout.tsx
import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import { Providers } from './providers'
import './globals.css'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'Nihongo IT',
  description: 'Học tiếng Nhật chuyên ngành IT',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi">
      <body className={inter.className}>
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}
```

- [ ] **Step 3: Commit**

```bash
git add src/app/layout.tsx src/app/providers.tsx
git commit -m "feat(layout): root layout + Sonner toaster"
```

### Task 2.5: Build `useAppToast` and other hooks

**Files:**
- Create: `src/hooks/useAppToast.ts`
- Create: `src/hooks/useDebounce.ts`
- Create: `src/hooks/useAsyncData.ts`
- Create: `src/hooks/useConfirm.ts`
- Create: `src/hooks/usePagination.ts`

- [ ] **Step 1: Write `useAppToast.ts`**

```typescript
// src/hooks/useAppToast.ts
import { toast } from 'sonner'

export function useAppToast() {
  return {
    success: (message: string) => toast.success(message),
    error: (message: string) => toast.error(message),
    info: (message: string) => toast.info(message),
    warning: (message: string) => toast.warning(message),
  }
}
```

- [ ] **Step 2: Write `useDebounce.ts`**

```typescript
// src/hooks/useDebounce.ts
import { useEffect, useState } from 'react'

export function useDebounce<T>(value: T, delay = 400): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(t)
  }, [value, delay])
  return debounced
}
```

- [x] **Step 3: Write `useAsyncData.ts`** — **REVISED in Phase 2:** React Compiler's `react-hooks/use-memo` rule forbids non-literal dep arrays. The hook no longer accepts `deps`; the caller must memoize the fetcher.

```typescript
// src/hooks/useAsyncData.ts
import { useCallback, useEffect, useState } from 'react'
import { extractApiError } from '@/types/common.types'

/**
 * Fetch data on mount and on `fetcher` reference change.
 * The caller is responsible for memoizing `fetcher` (via `useCallback`).
 *
 * Example:
 *   const fetcher = useCallback(() => api.get(`/items/${id}`), [id])
 *   const { data, loading, error, refresh } = useAsyncData(fetcher)
 */
export function useAsyncData<T>(fetcher: () => Promise<T>) {
  const [data, setData] = useState<T | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const run = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setData(await fetcher())
    } catch (e) {
      setError(extractApiError(e, 'Đã xảy ra lỗi'))
    } finally {
      setLoading(false)
    }
  }, [fetcher])

  useEffect(() => {
    // Intentional fetch-on-mount: React Compiler's set-state-in-effect rule flags
    // this pattern, but invoking the fetcher here is the whole point of the hook.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void run()
  }, [run])

  return { data, loading, error, refresh: run }
}
```

- [ ] **Step 4: Write `useConfirm.ts`** — use shadcn AlertDialog. See `src/components/common/ConfirmDialog.tsx` (created later in Task 2.7) for the rendering side; the hook exposes a promise-based API.

- [ ] **Step 5: Write `usePagination.ts`** — port the logic from `frontend/src/composables/usePagination.ts`. Returns `{ page, size, totalPages, setPage, setSize }`.

- [ ] **Step 6: Commit**

```bash
git add src/hooks
git commit -m "feat(hooks): port composables to React hooks"
```

### Task 2.6: Setup Vitest for unit tests

**Files:**
- Create: `vitest.config.ts`
- Create: `vitest.setup.ts`

- [ ] **Step 1: Install vitest + RTL**

```bash
npm install -D vitest @vitejs/plugin-react @testing-library/react @testing-library/jest-dom jsdom @vitest/coverage-v8
```

- [ ] **Step 2: Write `vitest.config.ts`**

```typescript
import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import path from 'path'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    globals: true,
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
})
```

- [ ] **Step 3: Write `vitest.setup.ts`**

```typescript
import '@testing-library/jest-dom/vitest'
```

- [ ] **Step 4: Add scripts to package.json**

```json
{
  "scripts": {
    "test": "vitest run",
    "test:watch": "vitest",
    "test:coverage": "vitest run --coverage"
  }
}
```

- [ ] **Step 5: Write a smoke test for `tokenStore`**

```typescript
// src/lib/__tests__/tokenStore.test.ts
import { describe, it, expect, beforeEach } from 'vitest'
import { getAccessToken, setAccessToken, clearAccessToken } from '../tokenStore'

describe('tokenStore', () => {
  beforeEach(() => clearAccessToken())

  it('starts with no token', () => {
    expect(getAccessToken()).toBeNull()
  })

  it('stores and retrieves a token', () => {
    setAccessToken('jwt-abc')
    expect(getAccessToken()).toBe('jwt-abc')
  })

  it('clears the token', () => {
    setAccessToken('jwt-abc')
    clearAccessToken()
    expect(getAccessToken()).toBeNull()
  })
})
```

- [ ] **Step 6: Run tests**

```bash
npm test
```

Expected: 3 passing.

- [ ] **Step 7: Commit**

```bash
git add vitest.config.ts vitest.setup.ts src/lib/__tests__
git commit -m "test: setup vitest + tokenStore smoke test"
```

### Task 2.7: Common UI components

**Files:**
- Create: `src/components/common/Loader.tsx`
- Create: `src/components/common/ConfirmDialog.tsx`
- Create: `src/components/common/ErrorBoundary.tsx`

- [ ] **Step 1: Write `Loader.tsx`**

```typescript
// src/components/common/Loader.tsx
import { Loader2 } from 'lucide-react'

export function Loader({ size = 24 }: { size?: number }) {
  return (
    <div className="flex items-center justify-center p-4">
      <Loader2 className="animate-spin text-primary" size={size} />
    </div>
  )
}
```

- [ ] **Step 2: Write `ConfirmDialog.tsx` + `useConfirm` integration**

Implement with shadcn `AlertDialog` (run `npx shadcn@latest add alert-dialog` if not present). `useConfirm()` opens the dialog and resolves a promise.

- [ ] **Step 3: Write `ErrorBoundary.tsx`** — class component (must be class for error boundaries in React).

- [ ] **Step 4: Commit**

```bash
git add src/components/common
git commit -m "feat(ui): common Loader, ConfirmDialog, ErrorBoundary"
```

### Task 2.8: Build auth proxy (per app)

**Files:**
- Create: `frontend-user/proxy.ts` — checks for `refresh_token` cookie presence; if absent on protected route, redirects to `/login`
- Create: `frontend-admin/proxy.ts` — same, BUT also decodes JWT (via header from gateway? or via separate `/me` call done client-side) to check role

In Next.js 16 the proxy runs on the **Node.js runtime** (the old `edge` runtime is NOT supported for `proxy.ts`). It can read cookies and call backend APIs, but synchronous calls slow every request. Strategy:

**User app proxy:** just check cookie presence — actual JWT validation happens in the API call. If cookie missing → redirect to login.

**Admin app proxy:** same as user app for now. Role check happens in `(admin)/layout.tsx` — that's a client-rendered guard that calls `/api/v1/user/auth/current`, checks `roleId === 1` (ADMIN), and redirects if not.

- [ ] **Step 1: Write `frontend-user/proxy.ts`**

```typescript
// frontend-user/proxy.ts  (Next.js 16: file renamed from middleware.ts, function renamed from middleware)
import { NextResponse, type NextRequest } from 'next/server'

const PUBLIC_PATHS = ['/', '/login', '/register', '/forgot-password', '/reset-password', '/furigana', '/translation']

export function proxy(req: NextRequest) {
  const { pathname } = req.nextUrl
  if (PUBLIC_PATHS.some((p) => pathname === p || pathname.startsWith(`${p}/`))) {
    return NextResponse.next()
  }
  const hasCookie = req.cookies.has('refresh_token')
  if (!hasCookie) {
    const url = req.nextUrl.clone()
    url.pathname = '/login'
    url.searchParams.set('redirect', pathname)
    return NextResponse.redirect(url)
  }
  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next|api|favicon.ico|.*\\..*).*)'],
}
```

- [ ] **Step 2: Write `frontend-admin/proxy.ts`**

Same as above but with `PUBLIC_PATHS = ['/login']` — every admin route requires auth.

- [ ] **Step 3: Commit**

```bash
git add proxy.ts
git commit -m "feat(auth): proxy-based route protection"
```

### Task 2.9: Auth store (Zustand)

**Files:**
- Create: `src/stores/auth.store.ts`
- Test: `src/stores/__tests__/auth.store.test.ts`

- [ ] **Step 1: Write the store**

```typescript
// src/stores/auth.store.ts
import { create } from 'zustand'
import type { UserInfo } from '@/types/user.types'
import { getAccessToken, setAccessToken, clearAccessToken } from '@/lib/tokenStore'
import { decodeToken } from '@/lib/jwt'
import { ROLES } from '@/types/user.types'
import api from '@/lib/api'
import { extractApiError } from '@/types/common.types'

interface AuthState {
  user: UserInfo | null
  loading: boolean
  error: string | null
  isAuthenticated: boolean
  isAdmin: boolean
  initialize: () => Promise<void>
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  fetchCurrentUser: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  loading: false,
  error: null,
  get isAuthenticated() {
    return !!get().user && !!getAccessToken()
  },
  get isAdmin() {
    const token = getAccessToken()
    if (!token) return false
    return decodeToken(token)?.role === ROLES.ADMIN
  },

  async initialize() {
    set({ loading: true })
    try {
      const res = await api.post<{ token: string }>('/api/v1/user/auth/refresh-token', {})
      setAccessToken(res.data.token)
      await get().fetchCurrentUser()
    } catch {
      clearAccessToken()
      set({ user: null })
    } finally {
      set({ loading: false })
    }
  },

  async login(email, password) {
    set({ loading: true, error: null })
    try {
      const res = await api.post<{ token: string }>('/api/v1/user/auth/login', { email, password })
      setAccessToken(res.data.token)
      await get().fetchCurrentUser()
    } catch (err) {
      const msg = extractApiError(err, 'Đăng nhập thất bại')
      set({ error: msg })
      throw new Error(msg)
    } finally {
      set({ loading: false })
    }
  },

  async logout() {
    try {
      await api.post('/api/v1/user/auth/logout')
    } finally {
      clearAccessToken()
      set({ user: null })
    }
  },

  async fetchCurrentUser() {
    const res = await api.get<{ userInfo: UserInfo }>('/api/v1/user/auth/current')
    set({ user: res.data.userInfo })
  },
}))
```

- [ ] **Step 2: Write tests covering login success/failure, logout, initialize success/failure**

Mock axios with `vi.mock('@/lib/api')`. Pattern matches existing `frontend/src/__tests__/auth.store.test.ts`.

- [ ] **Step 3: Commit**

```bash
git add src/stores
git commit -m "feat(auth): Zustand auth store with refresh-token initialization"
```

### Task 2.10: Verify Phase 2 end-to-end

- [ ] **Step 1: Run all checks per app**

```bash
cd frontend-user && npm run type-check && npm run lint && npm run build && npm test
cd ../frontend-admin && npm run type-check && npm run lint && npm run build && npm test
```

Expected: all pass. **Do NOT proceed to Phase 3 if any check fails.**

---

# Phase 3: Auth Flow (frontend-user) ✅ COMPLETED 2026-05-17

**Goal:** Working login, register, logout, password reset, Google OAuth in user app. Admin login (Phase 8) reuses these patterns.

**Outcome:**
- 6 zod schemas: login, signup, forgotPassword, resetPassword, changePassword, updateProfile
- 4 auth pages under `(auth)/`: login, register, forgot-password, reset-password (centered card layout)
- `AuthInitializer` mounted in `Providers` — silently restores session via refresh cookie on app mount
- `GoogleOAuthProvider` wraps tree conditionally (only when `NEXT_PUBLIC_GOOGLE_CLIENT_ID` set)
- `(app)/` route group with `Header` (logo + nav + user dropdown with logout) → applies to all authenticated routes
- Profile page (`/profile`) + Change password page (`/account/change-password`)
- Home page (`/`) — landing with Đăng nhập/Đăng ký buttons
- All forms use react-hook-form + zod with consistent error display + `aria-invalid`
- Verification: type-check ✓ · lint ✓ · 11/11 tests ✓ · build ✓ (8 routes prerendered)

### Discoveries (Phase 3)

| Discovery | Impact / Fix |
|---|---|
| `useSearchParams()` in pages causes build failure: `useSearchParams() should be wrapped in a suspense boundary` | Next.js 16 prerendering needs the hook inside `<Suspense>` boundary. **Pattern:** split the page — `page.tsx` is a server component that wraps `<Suspense fallback={<Loader />}>{<Form />}</Suspense>`, and `XxxForm.tsx` is the `'use client'` component using `useSearchParams`. Applied to login + reset-password. |
| `auth.store.login(request: LoginRequest)` signature | Plan's draft code sample showed `login(email, password)`. Actual store takes a `LoginRequest` object. Pages updated to `await login(values)`. |
| `GoogleOAuthProvider` requires `clientId` not empty | Wrap conditionally based on `process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID` to avoid console errors in dev without credentials. |
| `Header` only mounted in `(app)/layout.tsx` | Auth pages and home use their own layouts (no header). When user not yet loaded (during AuthInitializer), Header shows login/register buttons as fallback. |
| Profile form needs `useEffect(() => reset(user-derived defaults), [user, reset])` | User data arrives asynchronously after AuthInitializer completes. Reset form when it does so the inputs populate. |

### Task 3.1: Auth schemas (zod)

**Files:**
- Create: `src/schemas/auth.schema.ts`

- [ ] **Step 1: Write schemas**

```typescript
// src/schemas/auth.schema.ts
import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(6, 'Mật khẩu phải có ít nhất 6 ký tự'),
})
export type LoginInput = z.infer<typeof loginSchema>

export const signupSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(8, 'Mật khẩu phải có ít nhất 8 ký tự'),
  fullName: z.string().min(2, 'Tên không hợp lệ'),
  currentLevel: z.enum(['N1', 'N2', 'N3', 'N4', 'N5']).optional(),
  jlptGoal: z.enum(['N1', 'N2', 'N3', 'N4', 'N5']).optional(),
})
export type SignupInput = z.infer<typeof signupSchema>

export const forgotPasswordSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
})

export const resetPasswordSchema = z
  .object({
    token: z.string(),
    newPassword: z.string().min(8),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })
```

- [ ] **Step 2: Commit**

```bash
git add src/schemas
git commit -m "feat(auth): zod schemas for login/register/password reset"
```

### Task 3.2: Login page

**Files:**
- Create: `src/app/(auth)/layout.tsx` — minimal centered layout
- Create: `src/app/(auth)/login/page.tsx`

- [ ] **Step 1: Write layout**

```typescript
// src/app/(auth)/layout.tsx
export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <div className="w-full max-w-md">{children}</div>
    </div>
  )
}
```

- [ ] **Step 2: Write login page**

```typescript
// src/app/(auth)/login/page.tsx
'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { useRouter, useSearchParams } from 'next/navigation'
import Link from 'next/link'
import { loginSchema, type LoginInput } from '@/schemas/auth.schema'
import { useAuthStore } from '@/stores/auth.store'
import { useAppToast } from '@/hooks/useAppToast'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

export default function LoginPage() {
  const router = useRouter()
  const search = useSearchParams()
  const redirect = search.get('redirect') || '/'
  const toast = useAppToast()
  const login = useAuthStore((s) => s.login)
  const loading = useAuthStore((s) => s.loading)

  const { register, handleSubmit, formState: { errors } } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
  })

  async function onSubmit(values: LoginInput) {
    try {
      await login(values.email, values.password)
      toast.success('Đăng nhập thành công')
      router.push(redirect)
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Đăng nhập thất bại')
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Đăng nhập</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" {...register('email')} />
            {errors.email && <p className="text-sm text-red-500">{errors.email.message}</p>}
          </div>
          <div>
            <Label htmlFor="password">Mật khẩu</Label>
            <Input id="password" type="password" {...register('password')} />
            {errors.password && <p className="text-sm text-red-500">{errors.password.message}</p>}
          </div>
          <Button type="submit" className="w-full" disabled={loading}>
            {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
          </Button>
          <div className="flex justify-between text-sm">
            <Link href="/forgot-password" className="text-primary">Quên mật khẩu?</Link>
            <Link href="/register" className="text-primary">Tạo tài khoản</Link>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
```

- [ ] **Step 3: Manual test**

```bash
npm run dev
```

Visit `http://localhost:3000/login`, submit invalid credentials → expect error toast. Submit valid credentials → expect redirect.

- [ ] **Step 4: Commit**

```bash
git add src/app/\(auth\)
git commit -m "feat(auth): login page with react-hook-form + zod"
```

### Task 3.3: Register page

Follow same pattern as login. Form fields: email, password, fullName, currentLevel (Select), jlptGoal (Select). On success → toast "Vui lòng kiểm tra email" → redirect to `/login`.

- [ ] Implement, manual-test, commit.

### Task 3.4: Forgot/Reset password pages

Two pages following same form pattern. Forgot calls `POST /api/v1/user/auth/forgot-password`, Reset reads `?token=` from URL and calls `POST /api/v1/user/auth/set-new-password`.

- [ ] Implement, manual-test, commit.

### Task 3.5: Google OAuth login

**Files:**
- Modify: `src/app/(auth)/login/page.tsx` — add Google button
- Modify: `src/app/providers.tsx` — wrap with `<GoogleOAuthProvider>`

- [ ] **Step 1: Install + setup**

```bash
npm install @react-oauth/google
```

- [ ] **Step 2: Wrap providers**

```typescript
// src/app/providers.tsx
'use client'
import { GoogleOAuthProvider } from '@react-oauth/google'
import { Toaster } from 'sonner'

export function Providers({ children }: { children: React.ReactNode }) {
  return (
    <GoogleOAuthProvider clientId={process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID!}>
      {children}
      <Toaster position="top-right" richColors closeButton />
    </GoogleOAuthProvider>
  )
}
```

- [ ] **Step 3: Add `GoogleLogin` button to login page**, wire to `POST /api/v1/user/auth/google-login` then call `fetchCurrentUser`.

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat(auth): Google OAuth login"
```

### Task 3.6: Auth initialization on app mount

The user app needs to silently restore session on first load (existing `initializeAuth` action). Wire it into a Client Component near the root.

- [ ] **Step 1: Create `src/components/AuthInitializer.tsx`**

```typescript
'use client'
import { useEffect } from 'react'
import { useAuthStore } from '@/stores/auth.store'

export function AuthInitializer() {
  const initialize = useAuthStore((s) => s.initialize)
  useEffect(() => {
    void initialize()
  }, [initialize])
  return null
}
```

- [ ] **Step 2: Mount in `Providers`**

```typescript
// providers.tsx — add inside provider tree
<AuthInitializer />
```

- [ ] **Step 3: Verify** — open app cold (server restart, cookie still set) → user info populates without login.

- [ ] **Step 4: Commit**

### Task 3.7: Logout + profile

- [ ] **Step 1: Add Header component** at `src/components/layout/Header.tsx` with user dropdown + logout button.

- [ ] **Step 2: Add `(app)` route group** at `src/app/(app)/layout.tsx` that includes Header.

- [ ] **Step 3: Build `src/app/(app)/profile/page.tsx`** with profile form (fullName, currentLevel, jlptGoal). Submit calls `POST /api/v1/user/auth/update-profile`.

- [ ] **Step 4: Manual test full flow** — login → see header → click profile → edit → save → see toast → click logout → redirect to home.

- [ ] **Step 5: Commit**

---

# Phase 4: User App — Vocabulary Feature

**Goal:** All 6 vocabulary screens working (browse, learning by category, category detail, topic detail, item detail with AI chat, saved storage).

### Task 4.1: Vocabulary service + types

**Files:**
- Create: `src/services/vocabulary.service.ts` — port from `frontend/src/services/vocabulary.service.ts`. Use the `api` instance.
- Modify: `src/types/learning.types.ts` — already exists from Task 2.1.

- [ ] Port all methods (getVocabulary, getVocabularyById, saveVocabulary, removeSavedVocabulary, getSavedVocabulary).
- [ ] Commit.

### Task 4.2: Vocabulary store (Zustand)

**Files:**
- Create: `src/stores/vocabulary.store.ts`

Port from `frontend/src/stores/modules/vocabulary.ts` but as Zustand. Pattern: actions set `loading`, set `error`, throw `Error(error)` on failure (component catches + toasts).

- [ ] Implement, test, commit.

### Task 4.3: Vocabulary browse page

**Files:**
- Create: `src/app/(app)/vocabulary/page.tsx`
- Create: `src/components/vocabulary/VocabularyCard.tsx`
- Create: `src/components/vocabulary/VocabularyFilter.tsx`

Server Component renders the page shell. Client Component (`VocabularyList.tsx`) handles search/filter/pagination state and calls the service. Use `useDebounce` on search input.

- [ ] **Step 1: Build `VocabularyCard.tsx`** — displays term, pronunciation, meaning, JLPT badge, save button.
- [ ] **Step 2: Build `VocabularyFilter.tsx`** — JLPT level select, topic select, search input.
- [ ] **Step 3: Build the page** with grid layout, pagination.
- [ ] **Step 4: Manual test** — search, filter, paginate, save/unsave.
- [ ] **Step 5: Commit**

### Task 4.4: Vocabulary detail page + AI chat

**Files:**
- Create: `src/app/(app)/vocabulary/[id]/page.tsx`
- Create: `src/services/ai.service.ts` (port from Vue)
- Create: `src/components/vocabulary/AIChat.tsx`

The detail page shows the term, AI explanation, related vocabulary, examples, save button, and a chat interface for asking follow-up questions. Use SSE for streaming AI responses (matches existing `streamVocabularyChat`).

- [ ] Build, manual test (test AI streaming carefully), commit.

### Task 4.5: Category/Topic listing pages

**Files:**
- Create: `src/app/(app)/vocabulary/learning/page.tsx` — list of categories
- Create: `src/app/(app)/vocabulary/category/[id]/page.tsx`
- Create: `src/app/(app)/vocabulary/topic/[id]/page.tsx`
- Create: `src/services/category.service.ts`
- Create: `src/services/topic.service.ts`

- [ ] Build, manual test, commit.

### Task 4.6: Saved vocabulary page

**Files:**
- Create: `src/app/(app)/vocabulary/saved/page.tsx`

Reuses `VocabularyCard` and `useAsyncData(() => vocabularyService.getSavedVocabulary(filter), [filter])`.

- [ ] Build, manual test, commit.

---

# Phase 5: User App — Flashcard Study + Stats

**Goal:** Spaced-repetition study session + stats chart.

### Task 5.1: Flashcard service + store

**Files:**
- Create: `src/services/flashcard.service.ts`
- Create: `src/stores/flashcards.store.ts`

Port from existing Vue versions. Methods: `getDueCards`, `reviewFlashcard(cardId, rating)`, `getStudyStatistics`.

- [ ] Implement, test, commit.

### Task 5.2: Flashcard study page

**Files:**
- Create: `src/app/(app)/flashcards/study/page.tsx`
- Create: `src/components/flashcard/FlashcardReview.tsx`
- Create: `src/components/flashcard/RatingButtons.tsx`

Card flip animation with Tailwind (`group/transform/rotate-y-180`). Rating buttons: Again / Hard / Good / Easy. Optimistic UI: advance to next card immediately, send review in background.

- [ ] Build, manual test (test full study session end-to-end), commit.

### Task 5.3: Flashcard stats page

**Files:**
- Create: `src/app/(app)/flashcards/stats/page.tsx`
- Install: `chart.js react-chartjs-2`

```bash
npm install chart.js react-chartjs-2
```

- [ ] Build line chart (cards reviewed over time) + summary stats (due today, new, learning, mastered).
- [ ] Commit.

---

# Phase 6: User App — Conversation Practice + Speech

**Goal:** Conversation listing + practice with audio recording + speech analysis.

### Task 6.1: Conversation service

- [ ] Port `conversation.service.ts` (excluding admin methods — those go to admin app).
- [ ] Commit.

### Task 6.2: Conversation listing page

**Files:**
- Create: `src/app/(app)/conversation/page.tsx`

Grouped by JLPT level. Each card shows title, description, line count, "Practice" button.

- [ ] Build, commit.

### Task 6.3: AudioRecorder component

**Files:**
- Create: `src/components/conversation/AudioRecorder.tsx`
- Install: `recorder-js`

```bash
npm install recorder-js
```

Wraps `recorder-js` in a React component. Start/Stop button, recording indicator, returns Blob on stop.

- [ ] **Step 1: Build component with `useState` for recording state, `useRef` for Recorder instance.**
- [ ] **Step 2: Test in isolation page (`/test/recorder`) — verify start/stop/playback works.**
- [ ] **Step 3: Commit.**

### Task 6.4: Speech recognition service (Microsoft SDK)

**Files:**
- Create: `src/services/speechRecognition.service.ts`
- Install: `microsoft-cognitiveservices-speech-sdk`

```bash
npm install microsoft-cognitiveservices-speech-sdk
```

The SDK works in browser. Port the existing service file as-is (no Vue-specific code). All methods are class-based, framework-agnostic.

- [ ] Commit.

### Task 6.5: Conversation practice page

**Files:**
- Create: `src/app/(app)/conversation/[id]/practice/page.tsx`
- Create: `src/components/conversation/ConversationPlayer.tsx`

Shows each line in sequence. User clicks "Speak" → records → service analyzes via `ai.service.analyzeSpeech()` → shows score + per-word feedback. Save attempts, summarize at end.

- [ ] Build, manual test with real microphone, commit.

### Task 6.6: Furigana/wanakana integration for Japanese input

Install + wrap kuroshiro:

```bash
npm install kuroshiro kuroshiro-analyzer-kuromoji wanakana
```

- [ ] Create `src/lib/japanese.ts` wrapper that initializes kuroshiro once (lazy), exposes `toFurigana(text)`, `toRomaji(text)`.
- [ ] Commit.

---

# Phase 7: User App — Tools + Statistics

### Task 7.1: Furigana converter page

**Files:**
- Create: `src/app/(public)/furigana/page.tsx`

Textarea input → output with ruby tags showing furigana above kanji. Uses `lib/japanese.ts`.

- [ ] Commit.

### Task 7.2: Translation page

**Files:**
- Create: `src/app/(public)/translation/page.tsx`

Calls AI translation endpoint, displays history (localStorage).

- [ ] Commit.

### Task 7.3: Speech analyzer page (standalone)

**Files:**
- Create: `src/app/(app)/speech/page.tsx`

Standalone version of Phase 6 speech analysis — user types or pastes Japanese text, records themselves saying it, gets pronunciation feedback.

- [ ] Commit.

### Task 7.4: Statistics page

**Files:**
- Create: `src/app/(app)/statistics/page.tsx`
- Create: `src/services/statistics.service.ts`

Charts: cards reviewed per day, accuracy trend, JLPT level distribution, time spent. Use react-chartjs-2.

- [ ] Commit.

### Task 7.5: Phase 7 end verification

- [ ] Full smoke test: login → browse vocab → save → study flashcards → practice conversation → check stats → logout. **All flows must work.**
- [ ] `npm run type-check && npm run lint && npm run build && npm test` — all pass.

---

# Phase 8: Admin App — Foundation + Auth + Dashboard

**Goal:** Admin app reaches same shape as user app's Phase 3. Reuses 90% of code structure.

### Task 8.1: Port shared foundation to admin app

Repeat Phase 2 tasks (Task 2.1 through 2.10) for `frontend-admin/`. Most code is identical — only `proxy.ts` differs (all routes protected).

- [ ] Bulk port, verify all checks pass.

### Task 8.2: Admin login page

Similar to user login but with admin warning. After login, fetch current user and check `roleId === 1` (ADMIN). If not, logout + redirect to error page.

- [ ] Implement, commit.

### Task 8.3: Admin layout (Sidebar + Header)

**Files:**
- Create: `src/app/(admin)/layout.tsx`
- Create: `src/components/layout/Sidebar.tsx`
- Create: `src/components/layout/AdminHeader.tsx`

Use shadcn `Sheet` for mobile drawer, regular `<nav>` for desktop sidebar. Sidebar items: Dashboard, Users, Categories, Topics, Vocabulary, Conversations, Statistics.

- [ ] **Step 1: Build Sidebar with active route highlighting via `usePathname`.**
- [ ] **Step 2: Build layout integrating Sidebar + Header + main content.**
- [ ] **Step 3: Add role guard** — layout fetches current user, redirects if not admin.
- [ ] **Step 4: Commit.**

### Task 8.4: Dashboard page

**Files:**
- Create: `src/app/(admin)/page.tsx`
- Create: `src/services/admin.service.ts`

Stat cards (total users, vocabulary count, active sessions today, flashcards reviewed today) + recent activity list. Use `getDashboardStats()` from admin.service.

- [ ] Build, commit.

### Task 8.5: Users management

**Files:**
- Create: `src/app/(admin)/users/page.tsx`
- Create: `src/app/(admin)/users/[id]/page.tsx`
- Create: `src/components/data-table/DataTable.tsx`
- Install: `@tanstack/react-table`

```bash
npm install @tanstack/react-table
```

Build a reusable DataTable with sorting, pagination, search. Users page uses it. UserDetail page shows full user info + edit form + actions (activate/deactivate, change role).

- [ ] **Step 1: Build DataTable component (generic).**
- [ ] **Step 2: Build Users list page.**
- [ ] **Step 3: Build UserDetail page.**
- [ ] **Step 4: Manual test full CRUD.**
- [ ] **Step 5: Commit.**

---

# Phase 9: Admin App — Category / Topic / Vocabulary CRUD

### Task 9.1: Category management

**Files:**
- Create: `src/app/(admin)/categories/page.tsx`
- Create: `src/services/category.service.ts`

Table + create/edit dialogs (shadcn Dialog + react-hook-form). Use `useDebounce` for search.

- [ ] Commit.

### Task 9.2: Topic management

**Files:**
- Create: `src/app/(admin)/topics/page.tsx`
- Create: `src/services/topic.service.ts`

Similar to categories. Each topic belongs to a category — dropdown shows categories.

- [ ] Commit.

### Task 9.3: Vocabulary management

**Files:**
- Create: `src/app/(admin)/vocabulary/page.tsx`
- Create: `src/services/vocabulary.service.ts` (admin-only methods)

Most complex CRUD: term, meaning, pronunciation, example, exampleMeaning, audioPath, JLPT level, topic. Includes "Generate audio" button that triggers TTS.

- [ ] Commit.

---

# Phase 10: Admin App — Conversations + Drag-Drop Editor

### Task 10.1: Conversation list + detail

**Files:**
- Create: `src/app/(admin)/conversations/page.tsx`
- Create: `src/app/(admin)/conversations/[id]/page.tsx`

List with filters. Detail shows all lines read-only with "Edit" button → goes to edit page.

- [ ] Commit.

### Task 10.2: Conversation lines edit (drag-drop)

**Files:**
- Create: `src/app/(admin)/conversations/[id]/edit/page.tsx`
- Install: `@dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities`

```bash
npm install @dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities
```

Each line is a sortable item. Drag to reorder. Inline-edit speaker + japaneseText + vietnameseTranslation + notes. Add/delete line buttons. Save persists `orderIndex`.

- [ ] **Step 1: Setup `<DndContext>` + `<SortableContext>`.**
- [ ] **Step 2: Build SortableLine component using `useSortable` hook.**
- [ ] **Step 3: Wire reorder to local state, save batch on "Save" button.**
- [ ] **Step 4: Manual test drag-drop with keyboard accessibility.**
- [ ] **Step 5: Commit.**

---

# Phase 11: Admin App — Statistics

### Task 11.1: Statistics overview

**Files:**
- Create: `src/app/(admin)/statistics/page.tsx`

Aggregate charts: total users over time, total cards reviewed, active users heatmap.

- [ ] Commit.

### Task 11.2: User statistics list + detail

**Files:**
- Create: `src/app/(admin)/statistics/users/page.tsx`
- Create: `src/app/(admin)/statistics/users/[id]/page.tsx`

DataTable of users with key stats (streakCount, points, flashcardCount). Detail page shows per-user charts.

- [ ] Commit.

### Task 11.3: Phase 11 end verification

- [ ] Full admin smoke test: login → dashboard → create category → create topic → create vocabulary → create conversation → reorder lines → check user stats → logout.
- [ ] `npm run type-check && npm run lint && npm run build && npm test` — all pass.

---

# Phase 12: Cutover

**Goal:** Replace the old Vue frontend in production. **Destructive** — do this only after Phases 1-11 are 100% green and manually verified.

### Task 12.1: Docker images for both Next.js apps

**Files:**
- Create: `frontend-user/Dockerfile`
- Create: `frontend-admin/Dockerfile`
- Modify: `docker/docker-compose.yml`

- [ ] **Step 1: Multi-stage Dockerfile per app** — `node:24-alpine` builder, `node:24-alpine` runner running `next start`. Standalone output mode in `next.config.mjs`.

- [ ] **Step 2: Add services to compose file:**

```yaml
  frontend-user:
    build: ../frontend-user
    ports: ["3000:3000"]
    environment:
      NEXT_PUBLIC_API_BASE_URL: http://api-gateway:8080
    depends_on: [api-gateway]

  frontend-admin:
    build: ../frontend-admin
    ports: ["3001:3000"]
    environment:
      NEXT_PUBLIC_API_BASE_URL: http://api-gateway:8080
    depends_on: [api-gateway]
```

- [ ] **Step 3: Test `docker compose up --build`** locally, verify both apps reachable.

- [ ] **Step 4: Commit.**

### Task 12.2: Reverse proxy setup

**Decision needed before this task:** subdomain (admin.nihongo-it.com vs nihongo-it.com) OR path-based (/admin/* vs /*)?

**Recommended: subdomain** — cleaner CORS, separate cookies, separate session lifetimes.

- [ ] **Step 1: Add nginx/traefik routing rule** for the chosen strategy.
- [ ] **Step 2: Update CORS config in api-gateway** to allow both origins.
- [ ] **Step 3: Test end-to-end via the proxy.**
- [ ] **Step 4: Commit.**

### Task 12.3: Production smoke test in staging

- [ ] Deploy to staging.
- [ ] Run the full smoke test checklist (auth, vocab, flashcards, conversation, admin CRUD).
- [ ] Verify monitoring (Prometheus, Loki) catches Next.js logs.
- [ ] Performance check: Lighthouse score > 90 for both apps.

### Task 12.4: Decommission Vue frontend

**Only after staging is green for at least 1 week.**

- [ ] **Step 1: Remove `frontend/` service from `docker-compose.yml`.**
- [ ] **Step 2: Delete `frontend/` directory.**
- [ ] **Step 3: Update CI workflows (`.github/workflows/frontend.yml`) to point at `frontend-user/` and `frontend-admin/` instead.**
- [ ] **Step 4: Update `CLAUDE.md` and `README.md` to reflect new structure.**
- [ ] **Step 5: Commit + tag the release.**

---

## Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Microsoft Speech SDK pulls in large bundle (~1 MB) | Lazy-load via dynamic import only on conversation/speech pages |
| Kuroshiro init is slow (downloads dict ~10 MB on first use) | Lazy-init on first `toFurigana` call, show loader, cache in IndexedDB |
| SSR mismatch errors from auth state (server doesn't know user) | Mark auth-dependent components `'use client'`, defer auth-aware UI until mounted |
| httpOnly cookie not sent cross-origin in dev | Configure Next.js dev server to proxy `/api/*` to backend, OR set `withCredentials: true` + correct backend CORS `Access-Control-Allow-Credentials` |
| Drag-drop accessibility regression | Use `@dnd-kit/sortable` (built-in keyboard support), include screen-reader announcements |
| Bundle size of 2 separate Next.js apps duplicating shared code | Acceptable trade-off per user decision; revisit monorepo only if it becomes painful |
| chart.js with React 19 compat | Verify `react-chartjs-2` peer-dep range; pin to known-good version if needed |
| react-hook-form with Server Components | All forms must be `'use client'` — already planned in every form task |

---

## What This Plan Doesn't Cover

- **i18n (vue-i18n)** — not actively used in current frontend, skipped.
- **PWA** — current frontend uses `vite-plugin-pwa`; Next.js has `next-pwa` but it's a separate decision. Defer to post-cutover.
- **E2E tests** — current frontend has none. Adding Playwright is recommended but not in this plan.
- **Performance tuning** (RSC streaming, ISR for vocabulary, image optimization) — basic Next.js defaults apply; deep optimization is post-cutover work.
- **Analytics / error tracking** (Sentry, PostHog) — not in current frontend; recommend adding after cutover.

---

## Execution Handoff

**Plan saved to:** `docs/superpowers/plans/2026-05-17-nextjs-migration.md`.

When ready to execute, two options:

1. **Subagent-Driven (recommended for a plan this size)** — dispatch fresh subagents per phase or per task. Use `superpowers:subagent-driven-development`. Per-phase commits + verification keeps context windows manageable.

2. **Inline Execution** — execute tasks sequentially in one long session using `superpowers:executing-plans`. Risk: context exhaustion after Phase 3-4.

**Strong recommendation:** Phase 1-2 inline (foundation needs continuity), then subagent-per-feature-phase from Phase 3 onwards.
