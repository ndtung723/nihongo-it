# Improvement Plan — Nihongo IT

Audit date: 2026-05-12
Scope: backend microservices, frontend, infrastructure, security, testing, feature parity.
Constraint: pragmatic for a Japanese-learning product — avoid enterprise-grade overhead that doesn't pay off.

---

## TL;DR — current state

| Dimension | State | Verdict |
|---|---|---|
| Architecture | 7 services (5 Spring, 1 gateway, 1 Eureka) + Python NLP + Vue frontend | Solid foundation |
| Common module | Shared `dto/exception/security/logging/metrics/result` | Well-factored |
| Backend test coverage | **5 test files / 226 Kotlin files (~2%)** | **Critically low** |
| Frontend test coverage | **2 test files / 103 source files (~2%)** | **Critically low** |
| Observability | Prom + Grafana + Loki + structured JSON + correlation ID | Production-grade |
| CI/CD | Per-service matrix on backend, lint+type-check+build on frontend | Good, but no deploy automation |
| Migrations | Flyway, 6 user-service / 3 learning / 2 notification | Healthy |
| Security | JWT validated at gateway, header injection, rate limiting, refresh-token rotation, reuse detection | Strong |
| Code quality | Refactored conventions enforced (services, types, composables) | Strong |
| Feature parity FE/BE | A few BE-defined fields missing in admin DTO; flashcard response shape ambiguous | Minor gaps |

**Key finding:** the architecture is sound; the gap is **tests, observable SLIs, and 4–5 missing learner-facing features** that would significantly improve product value.

---

## Priority matrix (effort × impact)

```
                  Low effort                Medium effort               High effort
High impact   ┌─────────────────────┬──────────────────────────┬───────────────────────┐
              │ ★ P0-1 Test backbone│ ★ P1-1 Lesson/exam track │   P2-1 Mobile PWA     │
              │ ★ P0-2 BE-FE align  │ ★ P1-2 Audit log surface │   P2-2 Real-time chat │
              │ ★ P0-3 OpenAPI gen  │ ★ P1-3 Daily streak/gam. │                       │
              ├─────────────────────┼──────────────────────────┼───────────────────────┤
Med impact    │   P0-4 Error i18n   │   P1-4 Search server-rk  │   P2-3 ML-tuned FSRS  │
              │   P0-5 Health UI    │   P1-5 Offline cache PWA │                       │
              ├─────────────────────┼──────────────────────────┼───────────────────────┤
Low impact    │   P3 small cleanups │   (skip)                 │   (skip)              │
              └─────────────────────┴──────────────────────────┴───────────────────────┘
```

Recommend execution order: **P0 → P1 → P2**. Each P0 block is 1–2 days of focused work.

---

## P0 — Foundation (must do in next 2 weeks)

### P0-1 — Test backbone

**Problem:** 5 Kotlin tests cover only `AuthService`, `PasswordService`, and 3 flashcard-progressive paths. Critical paths (vocabulary CRUD, conversation, AI service, notification, gateway filter, FSRS scheduling) have **zero** test coverage.

**Plan:**

| Layer | Add | Target coverage |
|---|---|---|
| `common/security/GatewayHeaderAuthFilter` | Unit test — header injection sets `Authentication` | 100% |
| `common/exception/GlobalExceptionHandler` | Tests for each handled exception → expected DTO shape | 100% |
| `user-service` | `UserService.updateProfile`, `RefreshTokenService` (rotation + reuse detection) | ≥70% on services/ |
| `learning-service` | `VocabularyService` (save/unsave/search), `FSRSService` (rating calc), `CategoryService` (toggle status idempotency) | ≥60% on services/ |
| `ai-service` | `ChatController` (mock OpenAI), `TTSController` cache hit | Smoke + 1 happy + 1 error per controller |
| `notification` | `NotificationService.list/markAsRead`, scheduled tasks (mocked clock) | ≥70% |
| `api-gateway` | `GatewayJwtFilter` (valid/expired/missing token), `CorrelationIdGlobalFilter` | 100% |
| Frontend | Minimum: `extractApiError`, `tokenStore`, `useDebounce`, `useAsyncData`, router guards | ≥60% on `utils/`, `composables/`, `router/guards.ts` |
| Frontend stores | `auth.ts`, `vocabulary.ts`, `flashcards.ts` — happy + error paths | ≥70% |

**Tooling decisions:**
- Backend: keep JUnit 5 + MockK (already in use). Add Testcontainers Postgres for repository tests on at least `UserRepository`, `FlashcardRepository`. Don't mock JPA in services where SQL semantics matter.
- Frontend: Vitest already configured. Add `@vue/test-utils` for store tests. Skip component snapshot tests — too brittle for Vuetify.
- Coverage report: enable `kover` (Kotlin) and `vitest --coverage`. Publish HTML to CI artifact.
- **Do NOT chase 80% coverage globally** — target critical paths only. 90% on `service/` + `security/` + `composables/` matters; 30% on controllers is fine if integration tests exist.

**Acceptance:** `./gradlew test` passes; CI surfaces coverage delta on PR comments.

---

### P0-2 — Backend ↔ Frontend contract alignment

**Problems verified during refactoring but still pending:**

1. **`AdminUserDetailDto` missing fields** — frontend `UserDetailInfo` already declares `streakCount`, `points`, `flashcardCount`, `newCards`, `learningCards`, `masteredCards` (optional). Backend doesn't return them yet. Result: admin user detail page shows blank stats.
2. **Flashcard response shape ambiguous** — frontend has fallback `response.data?.data ?? response.data`. Confirm whether backend returns flat `FlashcardDTO[]` or wrapped `{ data: ... }` and remove the fallback.
3. **Conversation sort field** — frontend already fixed to `sortBy = "title"`; verify backend default matches.
4. **JLPT level filter on vocabulary** — backend supports it; some FE filter dropdowns hardcode levels. Centralize using `JlptLevel` type.

**Plan:**

- [ ] `learning-service.AdminUserController.getUserById` → join with `flashcard_stats` view to populate the 6 fields. Add `flashcard_stats_per_user` SQL view in V4 migration.
- [ ] `learning-service.FlashcardController.getDueCards` → return flat list. Update frontend to remove fallback.
- [ ] Add integration test asserting JSON contract for both endpoints.
<!-- - [ ] **skip Generate TypeScript types from OpenAPI** (see P0-3) so this kind of drift cannot recur. -->

**Acceptance:** open admin user detail page — all stats visible. `grep "response.data?.data ?? response.data" frontend/src` returns empty.

---

<!-- skip ### P0-3 — Generated OpenAPI → TypeScript contract

**Why:** today the FE manually mirrors BE DTOs in `types/`. Every BE field rename silently breaks the FE until someone notices. The 5 services already include `springdoc-openapi-starter-webmvc-ui` — schemas are exposed at `/v3/api-docs`.

**Plan:**

1. Add Gradle task per service: `./gradlew :{service}:generateOpenApiSpec` (uses `springdoc-openapi-gradle-plugin`) → writes `build/openapi.yaml`.
2. Aggregate at root: `tools/build-openapi.sh` calls each service, writes `frontend/src/api-contract/{service}.yaml`.
3. Add `frontend/package.json` script: `"gen-types": "openapi-typescript src/api-contract/*.yaml -o src/types/generated/api.ts"`.
4. Migrate one domain first (`learning.types.ts` → `generated/api.ts` re-export) to validate the toolchain.
5. CI: fail the build if `generated/api.ts` differs from what `gen-types` produces — forces regen on PR.

**Scope guard:** don't migrate all types in one go. Start with `Category`, `Topic`, `Vocabulary` because they have the most fields. Hand-written types (`common.types.ts` aliases like `JlptLevel`, `DateString`) stay hand-written.

**Acceptance:** 1 domain fully generated; CI gate active. -->

---

### P0-4 — Error code i18n + structured machine codes

**Today:** `BusinessException("VOCAB_DUPLICATE", "Term đã tồn tại")` returns the Vietnamese message verbatim. Frontend displays it. Codes exist but aren't used anywhere on the FE.

**Plan:**

- [ ] Standardize the code dictionary in `common/exception/ErrorCode.kt` (sealed class, kept in one place). Today codes are stringly typed at throw sites.
- [ ] Frontend: `src/types/errors.ts` → map `ERROR_CODE → user-friendly Vietnamese`. Fallback to server `message` if code not mapped.
- [ ] When you add an English locale later, just translate this map — server doesn't need to change.

**Acceptance:** every `BusinessException` throw site uses a constant from `ErrorCode`; grep finds 0 raw strings.

---

<!-- skip ### P0-5 — Service health UI

**Today:** `/actuator/health` exists per service; only Grafana surfaces it. There is no admin UI for "is the system healthy right now?".

**Plan:**

- Add `views/admin/SystemHealthView.vue` (admin-only). Pulls `/actuator/health` from each service via gateway, displays a 7-card matrix (gateway, eureka, user, learning, ai, notification, python). Shows DB connection state, disk space, last build version.
- Polls every 30s with `useAsyncData` + manual refresh.

**Why:** for a learning product with 7 backends, an at-a-glance view saves hours when something goes wrong in staging.

**Acceptance:** Admin can see all 7 dots green/red in one place. -->

---

## P1 — Learner-facing features that increase product value

These are real product gaps. None of them require new infrastructure.

### P1-1 — Structured lessons & exams

**Today:** the app has vocabulary + flashcards + conversation + speech, but no concept of a **lesson** that bundles them. Learners get a flat list of topics, not a guided path.

**Plan:**

- New entity in `learning-service`: `LessonEntity` { lessonId, jlptLevel, orderIndex, topicId, title, description, estimatedMinutes }
- `LessonStepEntity` { stepId, lessonId, type (vocab/grammar/conversation/quiz), refId, orderIndex }
- New endpoints: `GET /api/v1/learning/lessons?level=N5`, `GET /lessons/{id}/steps`, `POST /lessons/{id}/complete`.
- Frontend: `views/learning/LessonView.vue` already a stub; flesh it out — sequential step navigation, progress bar, completion celebration toast.
- "Exam mode" = a `Lesson` with steps all `type=quiz` and a timer. Reuse `FlashcardDTO` rendering.

**Effort:** ~3 days. Most plumbing exists. Migration: 1 table + step junction.

### P1-2 — Audit log + admin activity feed

**Today:** `audit_logs` table exists in `user-service` (V5 migration) — nobody surfaces it.

**Plan:**

- `AdminAuditService.listRecent(filter)` → paginated query.
- `views/admin/AuditLogView.vue` → table filtered by user / action / date.
- Add audit entries for: vocabulary CRUD by admin, role change, user deactivation, password change.

**Effort:** 1 day. Pure plumbing.

### P1-3 — Daily streak, points, gamification

**Today:** `streak_count`, `points` columns exist on user entity. Nothing increments or surfaces them.

**Plan:**

- `learning-service.StreakService.recordReview(userId)` — increments streak if last review was yesterday, resets if gap > 1 day, idempotent within same day.
- Hook into `FlashcardReviewService` after `recordReview` call.
- Frontend: `Header.vue` chip showing 🔥 streak. `ProfileView.vue` shows lifetime stats.
- Award points: 1pt per review, 5pt per lesson complete, 10pt per JLPT level achievement.

**Effort:** 1.5 days. Adds learner engagement without touching FSRS logic.

### P1-4 — Server-side ranked search

**Today:** vocabulary search hits Postgres `LIKE` — slow for >50k entries and doesn't rank by relevance.

**Plan:**

- Add Postgres `pg_trgm` extension + GIN index on `vocabulary(term, meaning, pronunciation)`.
- Migration: `CREATE EXTENSION IF NOT EXISTS pg_trgm; CREATE INDEX vocab_search_idx ON vocabulary USING gin (...);`
- Update repository query to `SELECT ... ORDER BY similarity(term, :keyword) DESC LIMIT 50`.
- No frontend change — just speedier search.

**Effort:** 0.5 day. Stays within Postgres — no Elasticsearch overhead.

### P1-5 — PWA offline cache for vocabulary

**Today:** user with spotty mobile coverage can't review on commute.

**Plan:**

- Add Vite PWA plugin to frontend.
- Service worker caches saved-vocabulary list + their TTS audio.
- IndexedDB stores last-fetched flashcards (`dueCards`).
- On reconnect, queued review actions sync up.

**Effort:** 2 days. Big learner-quality-of-life win.

---

## P2 — Nice-to-have, after P0/P1

### P2-1 — Mobile-first navigation polish
Floating bottom-nav for the 4 main flows (Học / Ôn tập / Hội thoại / Tôi). Current TheNavigation is desktop-leaning.

### P2-2 — Real-time conversation chat with AI tutor
WebSocket endpoint on `ai-service`. Replaces request/response chat with streaming tokens. Higher engagement, but only worth it once basics work.

### P2-3 — ML-tuned FSRS parameters
Today FSRS uses default weights. Later: per-user weight optimization via small offline job. Skip until 1000+ active users.

---

## Cross-cutting improvements

### Security hardening

- [ ] **Secret scanning in CI** — add `gitleaks` to `.github/workflows`. Already had a near-miss with the old `auth_token` localStorage bug.
- [ ] **Rate-limit registration** — currently login is 5/min, but `POST /register` has no limit. Spam risk.
- [ ] **Email verification flow** — `is_email_verified` column exists but no verification endpoint. Add `GET /verify-email/{token}` and unify with notification service.
- [ ] **CSP header on frontend** — Vite supports it, configure in `vite.config.ts` for production build.
- [ ] **CORS allowlist** — currently set via env. Verify production value is not `*`.
- [ ] **Content-Security-Policy & Referrer-Policy** added at gateway response. 10-minute change.
- [ ] **Dependency audit job** — `./gradlew dependencyCheckAnalyze` (OWASP) + `npm audit` on FE in weekly CI.
- [ ] **Password breach check** at registration — call `haveibeenpwned` k-anonymity API (free, no key). Reject top-leaked passwords.

### Code quality / DX

- [ ] **Dead-code scan** — services have `feedback.service.ts`, `feedback` controller, `feedback` entity but no view consuming them. Either build the feedback UI or remove.
- [ ] **Frontend lint warning cleanup** — 136 floating-promise warnings are real. Fix in batches of 20 per PR.
- [ ] **Pre-commit hook on backend** — add `ktlintFormat` as husky pre-commit so style mismatches don't reach CI.
- [ ] **Detekt baseline** — generate `detekt-baseline.xml` so existing tech debt doesn't block new work, while preventing regressions.
- [ ] **Service rename consistency** — folder `services/notification/` but image `notification-service` and class `notify`. Pick one — recommend `notification-service` everywhere.
- [ ] **README per service** — currently only root README. Each backend service needs a 20-line README: port, env vars, dependencies, how to run.

### Observability extensions

- [ ] **SLO definitions** — pick 3 user-journey SLOs: "login completes in <2s p95", "vocabulary page loads <1.5s p95", "TTS first byte <3s p95". Define error budgets.
- [ ] **Custom Prometheus metrics** for product KPIs: `flashcards_reviewed_total`, `vocabulary_saved_total`, `lesson_completed_total`. Already have `metrics/` helpers in common.
- [ ] **Loki log retention** — currently default. Set to 14 days for cost.
- [ ] **Frontend Sentry-like error reporting** — even self-hosted `glitchtip` is enough. Currently FE errors disappear silently after the toast.
- [ ] **Database slow query log** — turn on `log_min_duration_statement = 500` in Postgres config. Surfaces N+1 patterns.

### Feature parity audit

These are listed where backend exposes something the frontend doesn't use, or vice versa:

| Endpoint / column | BE | FE | Action |
|---|---|---|---|
| `audit_logs` table | ✅ | ❌ | Build audit view (P1-2) |
| `streak_count`, `points` | ✅ schema | ❌ logic | Increment + display (P1-3) |
| `is_email_verified` | ✅ column | ❌ verify flow | Email verification endpoint |
| `notification_preferences` table | ✅ | partial | NotificationSettingsView only toggles flags; expose per-type |
| `feedback` controller | ✅ | ❌ | Either build view or remove |
| `FuriganaController` | ✅ | ✅ (FuriganaView) | OK |
| Conversation `unit` field | sort by it | sort by `title` | Decide canonical sort; consider exposing `unit` filter |
| Admin statistics aggregates | ✅ (`AdminStatisticsController`) | partial | `StatisticsOverviewView`/`UserStatisticsDetailView` exist; verify all charts wired up |

### Database

- [ ] **Index audit** — V3 already added performance indexes for learning. Audit user-service and notification: are there indexes for `flashcards.due`, `notifications.user_id+is_read+sent_at`, `refresh_tokens.user_id`?
- [ ] **Soft delete pattern** — `Vocabulary`/`User` use hard delete. For users especially, add `deleted_at TIMESTAMP NULL` and exclude in repository default queries. Audit trail.
- [ ] **Connection pool tuning** — HikariCP default 10. Verify production env raises this to 20–30 per service.
- [ ] **Backup strategy doc** — root README mentions Postgres named volume; document how to back up and restore in `deploy/` folder.

### Frontend architecture polish

- [ ] **Schemas folder underused** — `schemas/auth.schema.ts` + `schemas/flashcard.schema.ts` exist for vee-validate. Add schemas for admin CRUD forms (Category, Topic, Vocabulary) to eliminate inline validation.
- [ ] **Generated routes type** — `unplugin-vue-router` for typed `router.push({ name: 'profile' })`. Optional but eliminates a class of bugs.
- [ ] **`useAsyncData` adoption** — only StatisticsView uses it; 8+ other views still have manual `loading/error/onMounted` pattern. Migrate `FlashcardStatsView`, `CategoryDetailView`, etc.
- [ ] **A11y pass** — Vuetify is mostly accessible but custom widgets (vocabulary card flip, audio player) need ARIA labels. Quick audit with Lighthouse.

---

## Things explicitly NOT in scope (avoid scope creep)

- ❌ **Microfrontends** — overkill for one Vue app
- ❌ **Kubernetes** — docker-compose is fine; GCP deploy scripts exist
- ❌ **Event sourcing / CQRS** — domain doesn't warrant it
- ❌ **GraphQL** — REST + OpenAPI generation covers the contract pain
- ❌ **Service mesh (Istio)** — too much ops overhead for 5 services
- ❌ **Multi-region** — single Vietnam region is enough for now
- ❌ **80%+ coverage everywhere** — chase critical paths only
- ❌ **Custom design system** — Vuetify ships everything needed
- ❌ **Rewriting Python service to Kotlin** — SudachiPy + speech analysis are well-served by Python
- ❌ **Native mobile apps** — PWA (P1-5) gets 90% of the value at 10% of the cost

---

## Suggested timeline

| Week | Focus |
|---|---|
| 1 | P0-1 backend test backbone (security + service core) |
| 2 | P0-1 frontend tests + P0-2 BE/FE alignment fixes |
| 3 | P0-3 OpenAPI codegen pipeline + P0-4 error codes |
| 4 | P0-5 health UI + cross-cutting: secret scan, registration rate limit, email verify |
| 5–6 | P1-1 Lesson model + frontend |
| 7 | P1-2 audit log view + P1-3 streak/gamification |
| 8 | P1-4 server search + P1-5 PWA offline cache |
| 9+ | P2 selectively |

---

## How to use this plan

1. **One PR per checklist item.** Squash-merge to `main`.
2. **Update memory** when a sub-item is done so future sessions don't redo it.
3. **Don't try to do everything.** Pick 1 P0 item, ship it, review with user, iterate.
4. **Avoid revisiting refactored conventions** — see `REFACTORING_PLAN.md` and skills under `.claude/skills/`.

When invoking Claude Code to execute an item from this plan, prefer:

```
"Pick up IMPROVEMENT_PLAN.md item P0-2 and execute it.
 Make a feature branch, write the migration + service update, add the integration test,
 and verify with build-and-verify skill before reporting."
```

The right skills (`backend-microservice`, `frontend-conventions`, `build-and-verify`) will be invoked automatically.
