---
name: build-and-verify
description: Use when verifying code changes compile (Kotlin or TypeScript), running linter, or before committing. Establishes compile-only workflow without running services — user prefers gradle build -x test for backend, npm type-check + build for the two Next.js frontends, NOT bootRun.
---

# Build & Verify — Nihongo IT

## When to invoke

- Before commit/PR
- After editing Kotlin or TypeScript code
- When the user asks for "build", "verify", "test compile", "type check"
- Do NOT invoke when the user actually wants to run the server

## Important user preference

**The user only wants compile verification, NOT bootRun / dev server.** This is recorded in memory: "compile only with `./gradlew build -x test`, no bootRun needed".

Reason: services run via Docker Compose or on a dedicated test environment. Locally we only need to know the code compiles + type-checks.

## Backend — Kotlin/Gradle

### Compile + lint all services

```bash
cd services
./gradlew build -x test
```

- The `build` task runs `compileKotlin`, `compileTestKotlin`, `ktlintCheck`, `detekt`
- `-x test` skips test execution
- Each service has an independent Gradle classpath (the root `build.gradle.kts` is intentionally empty)

### Build a single service

```bash
cd services
./gradlew :learning-service:build -x test
./gradlew :user-service:build -x test
./gradlew :ai-service:build -x test
./gradlew :notification-service:build -x test
./gradlew :api-gateway:build -x test
./gradlew :common:build -x test
```

### When to run tests

ONLY when the user explicitly asks. Run multiple modules together to save time:

```bash
cd services
./gradlew :common:test :api-gateway:test :learning-service:test
./gradlew :learning-service:test
```

**Auto-fix style before running tests** (test compile also goes through ktlint):
```bash
./gradlew :common:ktlintFormat :api-gateway:ktlintFormat :learning-service:ktlintFormat
```

Do NOT run tests as part of the regular verify workflow — slow and unnecessary for a compile check.

### Ktlint/Detekt failures

```bash
./gradlew ktlintFormat       # auto-fixes many cases
./gradlew :{service}:ktlintFormat
```

Detekt findings usually need manual fixes — read the output and address them.

### DO NOT use

```bash
./gradlew bootRun            # ❌ the user doesn't want this
./gradlew :{service}:bootRun # ❌
```

If you need to run the stack → `cd docker && docker compose up -d`.

## Frontend — Next.js 16 (TWO apps)

There are two independent apps with identical scripts. Verify each separately.

### Quick type-check (fastest, per app)

```bash
cd frontend-user && npm run type-check
cd frontend-admin && npm run type-check
```

- Runs `tsc --noEmit`
- Must be 0 errors before commit

### Lint (per app)

```bash
cd frontend-user && npm run lint
cd frontend-admin && npm run lint
```

- Runs `eslint .` directly (Next.js 16 removed `next lint`)
- Errors MUST be 0
- **React Compiler lint rules** (new in Next 16): `react-hooks/use-memo`, `react-hooks/set-state-in-effect`, `react-hooks/incompatible-library` — see CLAUDE.md rules 10, 11, 18 for handling

### Full build verification (per app)

```bash
cd frontend-user && npm run build
cd frontend-admin && npm run build
```

- Runs `next build` (Turbopack by default in Next.js 16)
- For Docker: `next.config.ts` has `output: 'standalone'` → produces `.next/standalone/server.js`
- Must succeed before commit

### Tests (vitest)

```bash
cd frontend-user && npm test
cd frontend-admin && npm test
```

- `npm test` runs `vitest run` (non-interactive)
- Each app has its own vitest config + setup file
- Watch mode: `npm run test:watch`
- Coverage: `npm run test:coverage`

Expected baseline: **11 tests per app** (4 tokenStore + 7 auth store). Both apps should be green.

**Key mocking patterns:**

```typescript
// tokenStore (in-memory)
vi.mock('@/lib/tokenStore', () => ({
  getAccessToken: vi.fn(),
  setAccessToken: vi.fn(),
  clearAccessToken: vi.fn(),
}))

// axios instance (single mock, then mockResolvedValueOnce per test)
vi.mock('@/lib/api', () => ({
  default: { post: vi.fn(), get: vi.fn(), put: vi.fn(), delete: vi.fn() },
}))
import api from '@/lib/api'
const mockedApi = api as unknown as { post: ReturnType<typeof vi.fn> }
mockedApi.post.mockResolvedValueOnce({ data: { token: 'jwt-abc' } })

// Zustand: reset state between tests
beforeEach(() => {
  useAuthStore.setState({ user: null, loading: false, error: null, initialized: false })
})
```

### Dev server (when the user wants to test the UI)

```bash
cd frontend-user && npm run dev      # http://localhost:3000
cd frontend-admin && npm run dev     # http://localhost:3001 (in dev; docker maps to host 3002)
```

Only run when the user explicitly wants to test in the browser. Do NOT start it on your own.

## Python service

```bash
cd python
pip install -r requirements.txt   # if not yet installed
pytest                            # run tests
```

Compilation = "Python runs" — there's no separate build step.

## Docker stack

```bash
cd docker
docker compose up -d                       # start everything (incl. both frontends)
docker compose ps                          # status
docker compose logs -f learning-service    # tail logs
docker compose down                        # stop
```

Frontend services:
- `frontend-user` → http://localhost:3000
- `frontend-admin` → http://localhost:3002 (host) / 3001 (container)

When the user says "end-to-end test" or "run the full stack" → use docker compose.

## Standard pre-commit workflow

```bash
# 1. Backend if Kotlin changed
cd services && ./gradlew build -x test

# 2. Frontend if TypeScript changed — check the affected app(s)
cd frontend-user && npm run type-check && npm run lint && npm run build
cd frontend-admin && npm run type-check && npm run lint && npm run build

# 3. Git status check
git status
git diff --stat
```

If any step fails → fix before committing.

## Reporting results

After building, **show the user**:
- ✅/❌ status for each step
- The verbatim error message on failure
- The build time (helps the user track regressions)

Do NOT be silent — the user needs to know the build passed.

## Anti-patterns

- ❌ Running `bootRun` "just to test" — the user doesn't want it
- ❌ Running `gradle test` on every commit — slow, unnecessary
- ❌ Skipping `type-check` on the frontend — TypeScript errors will break the runtime
- ❌ Running `next lint` (Next.js 16 removed it) — use `npm run lint` which runs eslint directly
- ❌ Verifying only one of the two frontend apps when both changed — check both
- ❌ Claiming "build passes" without actually running the command — verify-before-completion principle
