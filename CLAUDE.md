# CLAUDE.md

This file guides Claude Code (claude.ai/code) when working with this codebase.

## Project overview

**Nihongo IT** — a Japanese learning platform for IT professionals, built on microservice architecture.

- **Backend**: Kotlin + Spring Boot 3.4 (independent services, registered via Eureka, gateway centralizes JWT/rate-limit/CORS)
- **Frontend**: Vue 3 + TypeScript + Vuetify 3 + Pinia
- **NLP/Speech**: Python FastAPI service (SudachiPy, pronunciation analysis)
- **Infrastructure**: PostgreSQL 16 + Flyway, Prometheus + Grafana + Loki + Promtail

This is NOT a unified monorepo build — each Gradle service is independent, the frontend uses its own npm, and the python service uses its own pip.

## Documentation map

Modular skills hold layer-specific best practices. When working with:

| Layer | Skill to invoke |
|---|---|
| Frontend service file, types, axios | `frontend-conventions` |
| Pinia store (composition API, error flow) | `frontend-state-management` |
| Toast, debounce, confirm, async data fetching | `frontend-composables` |
| Router guards, auth flow | `frontend-router-auth` |
| Backend Kotlin service (controller, exception, security) | `backend-microservice` |
| Build verification, running tests, gradle workflow | `build-and-verify` |

Skills live at `.claude/skills/<name>/SKILL.md`. Invoke them via the `Skill` tool.

## Directory structure

```
nihongo-it/
├── services/                   # Backend Kotlin/Spring Boot
│   ├── common/                 # dto, exception, ext, logging, metrics, result, security
│   ├── api-gateway/            # :8080 — JWT validation, rate limit, routing
│   ├── eureka-server/          # :8761
│   ├── user-service/           # :8086 — auth, profile, OAuth2 Google
│   ├── learning-service/       # :8088 — flashcard, FSRS, vocab, conversation
│   ├── ai-service/             # :8087 — OpenAI chat, TTS
│   └── notification/           # :8089 — email + in-app notifications
├── frontend/                   # Vue 3 + TS + Vuetify
│   └── src/
│       ├── composables/        # useAppToast, useAsyncData, useConfirm, useDebounce, useAuth, usePagination
│       ├── services/           # axios wrappers — convention {name}.service.ts
│       ├── stores/modules/     # Pinia composition API
│       ├── types/              # common, user, learning, conversation, notification, ai, progress
│       ├── router/             # index.ts + guards.ts (requireAuth/requireAdmin/redirectIfAuthenticated)
│       ├── schemas/            # yup validation schemas (vee-validate)
│       └── views/              # admin/, auth/, learning/, conversation/
├── python/                     # FastAPI NLP service
├── docker/                     # docker-compose, prometheus, loki, grafana provisioning
├── deploy/                     # GCP deploy scripts
└── REFACTORING_PLAN.md         # Status of ongoing/completed refactoring
```

## Code conventions (overrides)

The rules below are the **distilled output** of the refactoring phases. Skills explain each one in detail.

### Frontend — must not be violated

1. **Service files**: `{name}.service.ts` lowercase, NOT `{Name}Service.ts`. Public methods return `Promise<T>` (already unwrapped). Admin methods may return `AxiosResponse<T>` if a component genuinely needs `.headers` access.
2. **Types**: defined under `src/types/*.types.ts`, NEVER inlined inside service files. Services import from `@/types/...types`.
3. **Error handling**: Use `extractApiError(err, fallback)` from `@/types/common.types`. Do NOT re-implement the `const e = err as { response?: ... }` cast pattern.
4. **Toasts**: Use `useAppToast()` from `@/composables/useAppToast`, NOT `useToast()` from `vue-toast-notification` directly. Do NOT pass `{ position, duration }` — defaults are already set.
5. **Stores**: Composition API only (`defineStore('name', () => { ... })`). Do NOT call `useToast` inside a store action — the store only sets `error.value` and `throw new Error(error.value)`; the component is responsible for catching + toasting.
6. **Router**: `beforeEach` only delegates to `requireAuth/requireAdmin` from `guards.ts`. Do NOT duplicate token-checking logic in `index.ts`.
7. **Token access**: Use `getAccessToken()` from `@/utils/tokenStore`, NOT raw `localStorage.getItem("auth_token")`.
8. **Debounce/Confirm**: Use the `useDebounce`/`useConfirm` composables, NOT manual `setTimeout` or `window.confirm()`.

### Backend — core conventions

1. **Common module**: Reuse `BusinessException`, `GlobalExceptionHandler`, `ErrorResponseDto`, `GatewayHeaderAuthFilter`, `JwtAuthenticationEntryPoint` from `services/common/`. Do NOT copy-paste them into individual services.
2. **Auth**: Services do NOT validate JWTs — the gateway has already done that and injected `X-User-Id`, `X-Role`, `X-Email` headers. Services read them via `GatewayHeaderAuthFilter`.
3. **Error response**: Throw `BusinessException(code, message, status)` — the centralized handler formats `ErrorResponseDto`. Do not return ad-hoc `ResponseEntity.badRequest()`.
4. **Logging**: Structured JSON (Logstash encoder) + correlation ID. Do NOT use `println` or `System.err.println`.
5. **Migrations**: Flyway under `src/main/resources/db/migration/V{version}__{name}.sql`. Versions increment; NEVER edit a migration that has already been merged.

## Daily workflow

### Compile-only verification (no bootRun required)

This is the workflow the user prefers — see the `build-and-verify` skill for details.

```bash
# Backend (in /services)
./gradlew build -x test           # compile all modules, skip tests

# Frontend (in /frontend)
npm run type-check                # vue-tsc --build, must be 0 errors
npm run build                     # vite build
npm run lint                      # eslint
```

Before committing: `type-check` + `build` MUST pass. The ~136 floating-promise lint warnings are pre-existing — they do not block commits.

### Running the full local stack

```bash
cd docker && docker compose up -d   # bring up postgres + all services
cd frontend && npm run dev          # localhost:5173
```

### Git workflow

- Main branch: `main`. Feature branches: `feature/<scope>-<short-desc>`.
- Commit format: `<type>: <summary>` — `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`.
- Do NOT use `--no-verify`, do NOT `--amend` (always create a new commit).
- Pre-commit hook: husky runs lint-staged (eslint --fix + prettier) on frontend files.

## Project status

- **6 backend sprints complete**: common module, rate limiting, structured logging, monitoring (Prom/Grafana/Loki), cleanup, notification API.
- **5 frontend refactoring phases complete**: type system unified, services renamed + merged, stores moved to composition API, composables integrated, router consolidated. See `REFACTORING_PLAN.md`.
- **Remaining P3 items**: useAsyncData integration, a few backend alignments (flashcard response shape, AdminUserDetailDto fields), test coverage.

## Principles when editing code

- **Respect existing conventions.** Before creating a new file, grep for the existing pattern (`composables/`, `types/`, `services/`).
- **Don't abstract prematurely.** Three similar lines is fine; only extract when there's a clear reason.
- **Don't write comments that explain WHAT** — the code already says it. Only write WHY when it isn't obvious.
- **Communication language**: Vietnamese for user-facing messages (toasts, labels, errors). English for code identifiers and documentation.

## Anti-patterns previously refactored — do not reintroduce

- ❌ `useToast()` called directly in views → ✅ `useAppToast()`
- ❌ Repeated `const e = err as { response?: ... }` → ✅ `extractApiError(err, fallback)`
- ❌ Manual `setTimeout` debounce in admin views → ✅ `useDebounce(query, 400)` + `watch`
- ❌ `window.confirm()` → ✅ `useConfirm()`
- ❌ Service named `categoryService.ts` (camelCase, no `.service`) → ✅ `category.service.ts`
- ❌ Types inlined inside service files → ✅ `@/types/{domain}.types.ts`
- ❌ Stores calling `useToast` → ✅ Store throws, component catches + toasts
- ❌ `router.beforeEach` with 20 lines of token checks → ✅ Delegate to `requireAuth/requireAdmin`
- ❌ Raw `localStorage.getItem("auth_token")` → ✅ `getAccessToken()` from `tokenStore`
