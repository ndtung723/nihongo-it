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

ONLY when the user explicitly asks:
```bash
./gradlew :learning-service:test
```

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
cd frontend
npm run test:unit       # watch mode
npm run test:coverage   # one-shot + coverage report
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
