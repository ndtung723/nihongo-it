---
name: build-and-verify
description: Use when verifying code changes compile (Kotlin or TypeScript), running linter, or before committing. Establishes compile-only workflow without running services — user prefers gradle build -x test for backend, npm type-check + build for frontend, NOT bootRun.
---

# Build & Verify — Nihongo IT

## When to invoke

- Before commit/PR
- After editing Kotlin or Vue/TS code
- When the user asks for "build", "verify", "test compile", "type check"
- Do NOT invoke when the user actually wants to run the server

## Important user preference

**The user only wants compile verification, NOT bootRun.** This is recorded in memory: "compile only with `./gradlew build -x test`, no bootRun needed".

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
./gradlew :notification:build -x test
./gradlew :api-gateway:build -x test
./gradlew :common:build -x test
```

### When to run tests

ONLY when the user explicitly asks. Run multiple modules together to save time:

```bash
# Run tests for the three modules that currently have test suites:
cd services
./gradlew :common:test :api-gateway:test :learning-service:test

# Single module
./gradlew :learning-service:test
```

**Auto-fix style before running tests** (test compile also goes through ktlint):
```bash
./gradlew :common:ktlintFormat :api-gateway:ktlintFormat :learning-service:ktlintFormat
```

**Read test results after running (PowerShell):**
```powershell
$modules = @('common','api-gateway','learning-service')
foreach ($m in $modules) {
    $dir = "D:/workspace/nihongo-it/services/$m/build/test-results/test"
    if (Test-Path $dir) {
        $xmlFiles = Get-ChildItem $dir -Filter '*.xml'
        $total = 0; $fail = 0; $err = 0
        foreach ($f in $xmlFiles) {
            [xml]$x = Get-Content $f.FullName
            $total += [int]$x.testsuite.tests
            $fail  += [int]$x.testsuite.failures
            $err   += [int]$x.testsuite.errors
        }
        Write-Host "${m}: $total tests, $fail failures, $err errors"
    } else {
        Write-Host "${m}: no test results found"
    }
}
```

Expected clean baseline: `common: 12 tests, 0 failures, 0 errors` / `api-gateway: 14 tests, 0 failures, 0 errors` / `learning-service: 43 tests, 0 failures, 0 errors`

Do NOT run tests as part of the regular verify workflow — slow and unnecessary for a compile check.

### Ktlint/Detekt failures

If `build` fails on style:
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

## Frontend — npm/Vite/vue-tsc

### Quick type-check (fastest)

```bash
cd frontend
npm run type-check
```

- Runs `vue-tsc --build`
- Must be 0 errors before commit
- The 3 pre-existing ESLint errors about test files outside `tsconfig` are unrelated — ignore them

### Full build verification

```bash
cd frontend
npm run build
```

- Runs `run-p type-check "build-only"` in parallel
- Vite production build
- Output to `dist/`, takes about 12 seconds
- Bundle-size warnings are acceptable

### Lint

```bash
cd frontend
npm run lint            # eslint --fix
```

- Auto-fixes many issues
- ~136 floating-Promise warnings are pre-existing and **do not block commits**
- Errors MUST be 0 (except for the 3 pre-existing ones mentioned above)

### Dev server (when the user wants to test the UI)

```bash
cd frontend
npm run dev             # http://localhost:5173
```

Only run when the user explicitly wants to test in the browser. Do NOT start it on your own.

### Tests (vitest)

```bash
# One-shot run (non-interactive) — use this in CI or when verifying
cd frontend
npx vitest run

# With verbose output showing each test name (useful for debugging failures)
npx vitest run --reporter=verbose

# Filter output to pass/fail summary only
npx vitest run --reporter=verbose 2>&1 | grep -E "FAIL|PASS|Tests |Test Files|✓|×" | tail -20

# Coverage report
npm run test:coverage

# Watch mode (interactive, for development)
npm run test:unit
```

**DO NOT use `npm run test`** — that script does not exist; the correct scripts are `test:unit` and `test:coverage`.

Expected clean baseline: **55 tests, 6 test files, 0 failures** across:
- `src/__tests__/auth.store.test.ts` — auth store (isAuthenticated, login, initializeAuth, logout)
- `src/__tests__/common.types.test.ts` — `extractApiError()` utility
- `src/__tests__/guards.test.ts` — router guards (requireAuth, requireAdmin, redirectIfAuthenticated)
- `src/__tests__/jwt.test.ts` — JWT utilities (decodeToken, isTokenExpired, getStoredPayload, isAdmin)
- `src/__tests__/tokenStore.test.ts` — in-memory token store
- `src/__tests__/useDebounce.test.ts` — `useDebounce` composable

**Key mocking patterns for frontend tests:**

```typescript
// tokenStore (in-memory — NOT localStorage)
vi.mock("@/utils/tokenStore", () => ({ getAccessToken: vi.fn(), clearAccessToken: vi.fn() }))
import { getAccessToken } from "@/utils/tokenStore"
vi.mocked(getAccessToken).mockReturnValue("token")

// jwt utils
vi.mock("@/utils/jwt", () => ({ isTokenExpired: vi.fn(), decodeToken: vi.fn() }))

// axios (for initializeAuth refresh-token call)
vi.mock("axios", async (importOriginal) => {
  const actual = await importOriginal<typeof import("axios")>()
  return { ...actual, default: { ...actual.default, post: vi.fn() } }
})
import axios from "axios"
vi.mocked(axios.post).mockResolvedValue({ data: { token: "valid_token" } })
```

**Use JUnit 5 assertions in Kotlin tests, NOT `kotlin.test`:**
```kotlin
// ✅ Always import from JUnit 5 — kotlin.test is not on classpath in these modules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
// ❌ Never import kotlin.test.assertEquals — will cause "Unresolved reference" at compile time
```

Only run when the user asks or when you edit a file with a corresponding test.

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
docker compose up -d                       # start everything
docker compose ps                          # status
docker compose logs -f learning-service    # tail logs
docker compose down                        # stop
```

When the user says "end-to-end test" or "run the full stack" → use docker compose.

## Standard pre-commit workflow

```bash
# 1. Backend if Kotlin changed
cd services && ./gradlew build -x test

# 2. Frontend if Vue/TS changed
cd frontend && npm run type-check && npm run build

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
- ❌ Claiming "build passes" without actually running the command — verify-before-completion principle
- ❌ Ignoring lint warnings beyond the 136 pre-existing — new warnings might be real problems
