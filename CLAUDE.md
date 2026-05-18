# CLAUDE.md

This file guides Claude Code (claude.ai/code) when working with this codebase.

## Project overview

**Nihongo IT** — a Japanese learning platform for IT professionals, built on microservice architecture.

- **Backend**: Kotlin 2.3.0 + Spring Boot 4.0.2 + Spring Cloud 2025.1.1 (independent services, registered via Eureka, gateway centralizes JWT/rate-limit/CORS). JDK 25.
- **Frontend**: Two standalone Next.js 16 apps with App Router + React 19 + TypeScript:
  - `frontend-user/` — user-facing app (port 3000)
  - `frontend-admin/` — admin app (port 3001 internal, 3002 host in docker)
  - Tailwind CSS 4 + shadcn/ui (Radix UI primitives) + Zustand + react-hook-form + zod + sonner
- **NLP/Speech**: Python FastAPI service (SudachiPy, pronunciation analysis)
- **Infrastructure**: PostgreSQL 16 + Flyway, Prometheus + Grafana + Loki + Promtail

This is NOT a unified monorepo build — each Gradle service is independent, each frontend has its own npm, and the python service uses its own pip. Types/api-client are intentionally duplicated between `frontend-user` and `frontend-admin` (no shared package) — same pattern as `sample_project/`.

## Documentation map

Modular skills hold layer-specific best practices. When working with:

| Layer | Skill to invoke |
|---|---|
| Backend Kotlin service (controller, exception, security) | `backend-microservice` |
| Build verification, running tests, gradle/npm workflow | `build-and-verify` |
| End-to-end feature spanning BE + FE | `feature-implementation-workflow` |

Skills live at `.claude/skills/<name>/SKILL.md`. Invoke them via the `Skill` tool.

> **Note on frontend skills:** The previous `frontend-conventions`, `frontend-state-management`, `frontend-composables`, `frontend-router-auth`, `frontend-error-handling` skills were Vue/Pinia/Vuetify-specific and were removed when the frontend was rewritten to Next.js. Patterns for the new stack live inline in this document below — keep it that way until enough Next.js-specific quirks accumulate to warrant a dedicated skill.

## Directory structure

```
nihongo-it/
├── services/                   # Backend Kotlin/Spring Boot
│   ├── common/                 # dto, exception, ext, logging, metrics, result, security
│   ├── api-gateway/            # :8080 — JWT validation, rate limit, routing, CORS
│   ├── eureka-server/          # :8761
│   ├── user-service/           # :8086 — auth, profile, OAuth2 Google
│   ├── learning-service/       # :8088 — flashcard, FSRS, vocab, conversation
│   ├── ai-service/             # :8087 — OpenAI chat, TTS
│   └── notification/           # :8089 — email + in-app notifications
├── frontend-user/              # Next.js 16 user app — port 3000
│   └── src/
│       ├── app/                # App Router: (public)/, (auth)/, (app)/ groups
│       ├── components/         # ui/ (shadcn), common/, layout/, vocabulary/, flashcard/, conversation/
│       ├── hooks/              # useAppToast, useDebounce, useAsyncData, useConfirm, usePagination, useAudioRecorder
│       ├── stores/             # Zustand: auth.store, vocabulary.store, flashcards.store
│       ├── services/           # axios wrappers — convention {name}.service.ts
│       ├── types/              # common, user, learning, conversation, ai, notification, roles
│       ├── schemas/            # zod validation schemas (used with react-hook-form)
│       ├── lib/                # api.ts (axios + refresh), tokenStore, jwt, charts, utils
│       └── proxy.ts            # Next.js 16 proxy (NOT middleware) for auth redirect
├── frontend-admin/             # Next.js 16 admin app — port 3001 internal / 3002 host
│   └── src/                    # Same structure; (admin)/ route group, layout-level role guard
├── python/                     # FastAPI NLP service
├── docker/                     # docker-compose, prometheus, loki, grafana provisioning
├── deploy/                     # GCP deploy scripts
└── docs/superpowers/plans/     # Migration / refactoring plans
```

## Code conventions (overrides)

### Frontend — must not be violated (Next.js 16 / React 19 / Tailwind 4)

1. **Next.js 16 — file naming**: route protection lives in `proxy.ts` at app root (NOT `middleware.ts` — renamed in v16). The exported function is `proxy`, not `middleware`. Runtime is Node.js (not Edge).
2. **Async params**: `params` and `searchParams` are Promises in v16. Server pages: `params: Promise<{id: string}>` then `await params`. Use `npx next typegen` to generate `PageProps<'/route'>` helpers.
3. **`useSearchParams` requires Suspense**: pages using `useSearchParams()` will fail prerendering unless wrapped in `<Suspense>`. Pattern: split `page.tsx` (server, wraps `<Suspense fallback>`) and `XxxForm.tsx` (`'use client'`, uses the hook).
4. **Service files**: `{name}.service.ts` lowercase. Methods return `Promise<T>` (already unwrapped). Use `unwrap<T>(payload, fallback)` helper when backend envelope is `{data: T}` inconsistently.
5. **Types**: defined under `src/types/*.types.ts`, NEVER inlined inside service files. Services import from `@/types/...types`.
6. **Error handling**: Use `extractApiError(err, fallback)` from `@/types/common.types`. Do NOT re-implement the `const e = err as { response?: ... }` cast pattern.
7. **Toasts**: Use `useAppToast()` from `@/hooks/useAppToast`, NOT `toast` from `sonner` directly. Sonner is the underlying lib; the hook is the project's stable surface.
8. **Stores (Zustand)**: actions only set `error` state and `throw new Error(error)` on failure. Do NOT call `useAppToast` inside a store action — the component catches + toasts. For derived state, export named selector functions (`selectIsAuthenticated`, `selectIsAdmin`) since Zustand has no computed getters.
9. **Logout pattern**: Local cleanup must always succeed. Use `try { post } catch {} finally cleanup` — never re-throw a logout failure (UI gets stuck in stale auth state when offline).
10. **`react-hooks/use-memo` rule**: `useCallback`/`useMemo` dep arrays must be array literals, not variables. If a hook needs caller-controlled deps, take a memoized fetcher (caller does `useCallback`) instead of accepting `deps: unknown[]`.
11. **`react-hooks/set-state-in-effect` rule**: fires on fetch-on-mount patterns. Suppress with `// eslint-disable-next-line react-hooks/set-state-in-effect` on the offending line (not above the `useEffect` wrapper) when the pattern is intentional (e.g. `void fetchData()` inside an effect).
12. **Token access**: Use `getAccessToken()` from `@/lib/tokenStore` (in-memory). Refresh token lives in httpOnly cookie set by backend. NEVER use `localStorage.getItem` for auth tokens.
13. **Forms**: react-hook-form + zod via `@hookform/resolvers/zod`. For number inputs use `register('field', { valueAsNumber: true })` + `z.number().or(z.nan()).transform(v => isNaN(v) ? undefined : v).optional()` — `z.coerce.number()` breaks RHF resolver typing.
14. **Radix Select**: cannot accept `value=""`. Use a sentinel like `__all__` mapped to `null` in onChange for "all" options.
15. **Forms with `valueAsNumber`** that arrive as NaN when empty must be transformed; never send `NaN` to the API.
16. **Confirm/debounce**: Use `useConfirm` (promise-based, returns boolean) and `useDebounce` from `@/hooks/`. NEVER `window.confirm()` or hand-rolled `setTimeout`.
17. **Server Components vs Client Components**: pages that only render data can be `async function Page()` (Server). Pages with form state, dialogs, effects need `'use client'`. Auth cookie forwards automatically because axios uses `withCredentials: true`.
18. **TanStack Table v8 incompatibility with React Compiler**: suppress `react-hooks/incompatible-library` directly above `useReactTable(...)`. Library limitation, not a bug.
19. **3D card flip + Tailwind 4**: custom utilities required (`perspective-card`, `transform-3d`, `backface-hidden`, `rotate-y-180`). Declared in `globals.css` via `@utility`. Brackets in `@utility` names not allowed — use plain identifiers.
20. **Chart.js**: centralized registration in `lib/charts.ts`. Each chart page does `import '@/lib/charts'` as a side effect once.

### Backend — core conventions

1. **Common module**: Reuse `BusinessException`, `GlobalExceptionHandler`, `ErrorResponseDto`, `GatewayHeaderAuthFilter`, `JwtAuthenticationEntryPoint` from `services/common/`. Do NOT copy-paste them into individual services.
2. **Auth**: Services do NOT validate JWTs — the gateway has already done that and injected `X-User-Id`, `X-Role`, `X-Email` headers. Services read them via `GatewayHeaderAuthFilter`.
3. **Error response**: Throw `BusinessException(code, message, status)` — the centralized handler formats `ErrorResponseDto`. Do not return ad-hoc `ResponseEntity.badRequest()`.
4. **Logging**: Structured JSON (Logstash encoder) + correlation ID. Do NOT use `println` or `System.err.println`.
5. **Migrations**: Flyway under `src/main/resources/db/migration/V{version}__{name}.sql`. Versions increment; NEVER edit a migration that has already been merged.
6. **CORS**: `app.cors.allowed-origins` default covers `localhost:3000` (user) + `localhost:3002` (admin). Set `CORS_ALLOWED_ORIGINS` env var in staging/prod for real domains.
7. **Frontend URL for emails**: `APP_FRONTEND_URL` env var (default `http://localhost:3000`) is used by `NotificationService` to build email action links (verify-email, reset-password, disable-notifications, study-reminder). MUST point at `frontend-user`.

## Daily workflow

### Compile-only verification (no bootRun, no dev server)

This is the workflow the user prefers — see the `build-and-verify` skill for details.

```bash
# Backend (in /services)
./gradlew build -x test           # compile all modules, skip tests

# Frontend (per app)
cd frontend-user && npm run type-check && npm run lint && npm run build
cd frontend-admin && npm run type-check && npm run lint && npm run build
```

Before committing: `type-check` + `lint` + `build` MUST pass on both apps. Tests must pass too (`npm test` — 11 tests each).

### Running the full local stack

```bash
cd docker && docker compose up -d   # postgres + all services + both frontends
# → frontend-user: http://localhost:3000
# → frontend-admin: http://localhost:3002
# → api gateway:   http://localhost:8080
```

Dev mode (without docker, for hot reload):
```bash
cd frontend-user && npm run dev      # http://localhost:3000
cd frontend-admin && npm run dev     # http://localhost:3001 (note: 3001 in dev, 3002 via docker)
```

### Git workflow

- Main branch: `main`. Feature branches: `feature/<scope>-<short-desc>`.
- Commit format: `<type>(scope): <summary>` — `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, `build:`, `test:`.
- Do NOT use `--no-verify`, do NOT `--amend` (always create a new commit).

## Project status

- **Backend**: 6 sprints complete (common module, rate limiting, structured logging, monitoring, cleanup, notification API). Migrated to Kotlin 2.3 + Spring Boot 4 + Java 25.
- **Frontend**: Vue → Next.js migration complete. `frontend-user` has 21 routes, `frontend-admin` has 14 routes. Full feature parity with the old Vue app.
- **Migration plan**: `docs/superpowers/plans/2026-05-17-nextjs-migration.md` documents all 12 phases + ~60 discoveries.

## Principles when editing code

- **Respect existing conventions.** Before creating a new file, grep for the existing pattern (`hooks/`, `types/`, `services/`).
- **Don't abstract prematurely.** Three similar lines is fine; only extract when there's a clear reason.
- **Don't write comments that explain WHAT** — the code already says it. Only write WHY when it isn't obvious.
- **Communication language**: Vietnamese for user-facing messages (toasts, labels, errors). English for code identifiers and documentation.
- **Document deviations in the plan**: when you discover a Next.js 16 / React 19 / library quirk that requires a workaround, add it to the relevant Phase's "Discoveries" section in `docs/superpowers/plans/2026-05-17-nextjs-migration.md`.

## Anti-patterns previously refactored — do not reintroduce

- ❌ `toast()` from sonner directly in views → ✅ `useAppToast()` from `@/hooks/`
- ❌ Repeated `const e = err as { response?: ... }` → ✅ `extractApiError(err, fallback)`
- ❌ Manual `setTimeout` debounce → ✅ `useDebounce(value, 400)`
- ❌ `window.confirm()` → ✅ `useConfirm()`
- ❌ Service named `categoryService.ts` (camelCase, no `.service`) → ✅ `category.service.ts`
- ❌ Types inlined inside service files → ✅ `@/types/{domain}.types.ts`
- ❌ Stores calling `useAppToast` → ✅ Store throws, component catches + toasts
- ❌ Raw `localStorage.getItem("auth_token")` → ✅ `getAccessToken()` from `@/lib/tokenStore`
- ❌ `middleware.ts` (Next.js 15 era) → ✅ `proxy.ts` (Next.js 16)
- ❌ Sync `params.id` in dynamic routes → ✅ `await params` then destructure
- ❌ `z.coerce.number()` in form schemas → ✅ `z.number().or(z.nan()).transform(...).optional()` + `valueAsNumber: true`
- ❌ Empty-string `<SelectItem value="">` → ✅ sentinel `__all__` mapped to `null`
- ❌ `useReactTable` without lint suppression → ✅ `// eslint-disable-next-line react-hooks/incompatible-library`
