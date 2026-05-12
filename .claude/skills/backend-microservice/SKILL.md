---
name: backend-microservice
description: Use when modifying backend Kotlin/Spring Boot services (user-service, learning-service, ai-service, notification, api-gateway). Covers common module reuse, gateway-based auth header injection, BusinessException pattern, Flyway migrations, structured logging.
---

# Backend Microservice — Nihongo IT

## When to invoke

- Editing Kotlin code under `services/*/src/main/kotlin/`
- Adding a controller, exception, or security filter
- Creating a Flyway migration
- When the user says "add a backend endpoint", "new service", "fix exception", "JWT", "rate limit"

## Auth architecture — gateway-centric

JWTs are validated ONLY at `api-gateway`. After successful validation, the gateway injects 3 headers downstream:

| Header | Source | Use case |
|---|---|---|
| `X-User-Id` | JWT `sub` (UUID) | Service queries user-scoped data |
| `X-Role` | JWT `role` claim | Authorization in controllers |
| `X-Email` | JWT `email` claim | Logging / audit |

### Services read those headers via `GatewayHeaderAuthFilter`

```kotlin
// common/security/GatewayHeaderAuthFilter.kt — already exists
// This filter reads X-User-Id/Role/Email from HttpServletRequest and
// sets SecurityContextHolder.authentication = UsernamePasswordAuthenticationToken

// In a controller:
@GetMapping("/profile")
fun getProfile(authentication: Authentication): ProfileDto {
  val userId = authentication.name           // X-User-Id
  val role = authentication.authorities.first().authority  // ROLE_USER or ROLE_ADMIN
  return profileService.getById(userId)
}
```

Do NOT parse JWTs inside services — the gateway already did. Do NOT call `userService.findByToken()` — the `X-User-Id` header is enough.

## Common module — mandatory reuse

`services/common/` provides:

```
common/src/main/kotlin/com/example/common/
├── dto/
│   ├── ErrorResponseDto.kt       # Unified shape for error responses
│   └── FieldErrorDto.kt          # Per-field validation error
├── exception/
│   ├── BusinessException.kt      # Throw instead of returning ResponseEntity.badRequest()
│   ├── UnauthorizedException.kt
│   └── GlobalExceptionHandler.kt # @ControllerAdvice handling everything
├── ext/                          # Kotlin extensions
├── logging/                      # Structured JSON logging + correlation ID
├── metrics/                      # Micrometer/Prometheus helpers
├── result/                       # Result<T> wrapper
└── security/
    ├── GatewayHeaderAuthFilter.kt
    └── JwtAuthenticationEntryPoint.kt
```

Each service's `build.gradle.kts`:
```kotlin
dependencies {
  implementation(project(":common"))
  // ...
}
```

Do NOT copy code from `common/` into individual services. If a helper is missing → add it to `common/`, do not inline it.

## Error handling pattern

### Throw BusinessException, not ResponseEntity.badRequest()

```kotlin
// ❌ Wrong
@PostMapping("/vocabulary")
fun create(@RequestBody req: CreateVocabularyRequest): ResponseEntity<*> {
  if (vocabularyRepository.existsByTerm(req.term)) {
    return ResponseEntity.badRequest().body(mapOf("message" to "Term exists"))
  }
  // ...
}

// ✅ Correct
@PostMapping("/vocabulary")
fun create(@RequestBody req: CreateVocabularyRequest): VocabularyDto {
  if (vocabularyRepository.existsByTerm(req.term)) {
    throw BusinessException("VOCAB_DUPLICATE", "Term đã tồn tại", HttpStatus.BAD_REQUEST)
  }
  return vocabularyService.create(req)
}
```

`GlobalExceptionHandler` (already in `common/`) will:
- Catch `BusinessException` → return `ErrorResponseDto` with the right HTTP status
- Catch `MethodArgumentNotValidException` (bean validation) → return `FieldErrorDto[]`
- Catch generic `Exception` → log + return 500 with a generic message

### BusinessException constructor

```kotlin
class BusinessException(
  val code: String,                    // Machine-readable, e.g. "VOCAB_DUPLICATE"
  override val message: String,        // User-facing, Vietnamese is OK
  val status: HttpStatus = BAD_REQUEST,
) : RuntimeException(message)
```

`code` is used for frontend i18n or to track specific errors. `message` is shown as a toast to the user.

## Controller pattern

```kotlin
@RestController
@RequestMapping("/api/v1/learning/vocabulary")
class VocabularyController(
  private val vocabularyService: VocabularyService,
) {
  @GetMapping
  fun list(
    @RequestParam(required = false) keyword: String?,
    @RequestParam(required = false) jlptLevel: String?,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
    authentication: Authentication,
  ): Page<VocabularyDto> {
    return vocabularyService.list(keyword, jlptLevel, page, size, userId = authentication.name)
  }

  @PostMapping("/{id}/save")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun save(@PathVariable id: UUID, authentication: Authentication) {
    vocabularyService.save(id, authentication.name)
  }
}
```

- Return DTO/Page directly — do NOT wrap in `ResponseEntity` unless you need a specific status
- `@ResponseStatus(NO_CONTENT)` for actions that don't return a body
- Admin endpoints: `@RequestMapping("/api/v1/learning/admin/...")` + the gateway route checks `X-Role`

## Flyway migrations

### Location

```
services/{service-name}/src/main/resources/db/migration/V{version}__{snake_case_description}.sql
```

Examples:
- `V1__init_schema.sql`
- `V2__add_user_preferences.sql`
- `V3__add_flashcard_state_index.sql`

### Rules

- Do NOT edit migrations that have been merged to main — create a new one instead
- Versions increment without gaps
- Each service owns its schema (user-service does not edit learning-service tables)
- Naming: `V{n}__{snake_case}.sql` — two underscores between version and description

### Cross-service data sharing

If you need a cross-service FK: do NOT create real FK constraints. Just store the UUID and validate via a Feign call or event. Each service has its own database (logical separation).

## Logging

```kotlin
private val log = LoggerFactory.getLogger(VocabularyService::class.java)

log.info("Created vocabulary: id={} term={}", vocabId, term)
log.error("Failed to fetch from OpenAI", exception)
```

- Structured JSON (Logstash encoder, already configured in `common/logging`)
- Correlation ID is auto-injected via the MDC filter
- Do NOT use `println` or `System.err.println`
- Do NOT log sensitive data (passwords, tokens, full credit-card numbers)

## Rate limiting

Already configured at `api-gateway` with Bucket4j:
- Login: 5/min
- AI endpoints: 30/min
- Speech endpoints: 10/min

Services do NOT need to rate-limit again. If new rules are needed → edit `api-gateway` config.

## Python service protection

`python` (FastAPI :8000) only accepts requests with an `X-Internal-Key` header matching an env var. Kotlin services calling Python must inject that header in Feign config — do NOT expose the Python service publicly.

## Verify build

```bash
cd services
./gradlew build -x test           # compile only, skip tests
./gradlew :learning-service:build -x test   # build a single service
```

You do NOT need `bootRun` to verify compilation. See the `build-and-verify` skill.

## References

- `references/common-module-usage.md` — detailed per-class breakdown of common/
- `references/controller-checklist.md` — checklist when adding endpoints
