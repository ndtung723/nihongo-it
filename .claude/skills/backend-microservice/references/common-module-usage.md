# Common Module — Detailed Breakdown

`services/common/` holds code shared by every backend service. Avoids duplication.

## Layout

```
common/src/main/kotlin/com/example/common/
├── dto/
│   ├── ErrorResponseDto.kt
│   └── FieldErrorDto.kt
├── exception/
│   ├── BusinessException.kt
│   ├── UnauthorizedException.kt
│   └── GlobalExceptionHandler.kt
├── ext/                          # Kotlin extension functions
├── logging/                      # JSON encoder, correlation ID filter
├── metrics/                      # Custom Micrometer counters/gauges
├── result/                       # Result<T> sealed class
└── security/
    ├── GatewayHeaderAuthFilter.kt
    └── JwtAuthenticationEntryPoint.kt
```

## dto/

### ErrorResponseDto

Unified shape for every error response:

```kotlin
data class ErrorResponseDto(
  val timestamp: Instant,
  val status: Int,
  val code: String,            // Machine-readable
  val message: String,         // User-facing (Vietnamese)
  val path: String,
  val correlationId: String?,
  val fieldErrors: List<FieldErrorDto>? = null,
)
```

The frontend `extractApiError()` parses the `message` field. Do NOT change this shape — it will break frontend error handling.

### FieldErrorDto

For validation errors:

```kotlin
data class FieldErrorDto(
  val field: String,
  val rejectedValue: Any?,
  val message: String,
)
```

## exception/

### BusinessException

```kotlin
open class BusinessException(
  val code: String,
  override val message: String,
  val status: HttpStatus = HttpStatus.BAD_REQUEST,
) : RuntimeException(message)
```

Use cases:
- `throw BusinessException("VOCAB_NOT_FOUND", "Không tìm thấy từ vựng", NOT_FOUND)`
- `throw BusinessException("EMAIL_DUPLICATE", "Email đã được sử dụng", CONFLICT)`
- `throw BusinessException("FSRS_INVALID_RATING", "Rating phải từ 1 đến 4")`

Rules for `code`:
- `{DOMAIN}_{CONDITION}` — `VOCAB_NOT_FOUND`, `USER_INACTIVE`, `OAUTH_PROVIDER_FAILED`
- UPPER_SNAKE_CASE
- Specific enough that the frontend can i18n if needed

### UnauthorizedException

```kotlin
class UnauthorizedException(message: String = "Unauthorized") :
  BusinessException("UNAUTHORIZED", message, HttpStatus.UNAUTHORIZED)
```

Throw when an action isn't permitted (auth is fine but the role/owner isn't). Used for 401, not 403 — 403 is normally handled at the gateway.

### GlobalExceptionHandler

`@RestControllerAdvice` handling:

| Exception | HTTP | Response |
|---|---|---|
| `BusinessException` | from `.status` | `ErrorResponseDto` with `code`, `message` |
| `MethodArgumentNotValidException` | 400 | `ErrorResponseDto` + `fieldErrors[]` |
| `AccessDeniedException` | 403 | `ErrorResponseDto` code=`FORBIDDEN` |
| Generic `Exception` | 500 | `ErrorResponseDto` code=`INTERNAL_ERROR`, generic message (no stack leak) |

**Already provided by common; do NOT override in individual services.** If you need a service-specific exception handler → add an `@ExceptionHandler` in a controller-specific advice; do NOT edit `GlobalExceptionHandler`.

## logging/

JSON structured logs with fields:
- `@timestamp`, `level`, `logger`, `thread`
- `correlationId` (from MDC)
- `userId` if an authentication context exists
- `message`, `exception` when applicable

Loki + Promtail aggregate the logs. The Grafana dashboard can query by `correlationId` to trace a request across services.

Do NOT use `println` — it bypasses the JSON encoder and correlation ID.

## security/

### GatewayHeaderAuthFilter

Reads the 3 headers `X-User-Id/Role/Email` (gateway-injected) and sets `SecurityContextHolder`:

```kotlin
// Already exists — services just register it
@Configuration
class SecurityConfig {
  @Bean
  fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
    http
      .csrf { it.disable() }
      .sessionManagement { it.sessionCreationPolicy(STATELESS) }
      .authorizeHttpRequests { auth ->
        auth
          .requestMatchers("/api/v1/{domain}/admin/**").hasAuthority("ROLE_ADMIN")
          .requestMatchers("/api/v1/{domain}/**").authenticated()
          .anyRequest().permitAll()
      }
      .addFilterBefore(GatewayHeaderAuthFilter(), UsernamePasswordAuthenticationFilter::class.java)
      .exceptionHandling { it.authenticationEntryPoint(JwtAuthenticationEntryPoint()) }
      .build()
}
```

### JwtAuthenticationEntryPoint

When a request is missing/has wrong auth headers → return 401 with `ErrorResponseDto` (NOT a redirect).

## metrics/

Helpers for custom metrics:

```kotlin
@Component
class VocabularyMetrics(meterRegistry: MeterRegistry) {
  private val saveCounter = meterRegistry.counter("vocabulary.save.count")
  private val searchTimer = meterRegistry.timer("vocabulary.search.duration")

  fun recordSave() { saveCounter.increment() }
  fun recordSearch(durationMs: Long) { searchTimer.record(Duration.ofMillis(durationMs)) }
}
```

Prometheus scrapes `/actuator/prometheus` on each service. The Grafana dashboard already covers standard metrics.

## When to add to common/ vs. keep in a service

| Add to common/ | Keep in service |
|---|---|
| Cross-cutting (logging, security, metrics) | Domain-specific entity/service |
| Shared DTO used by > 1 service | Service-only DTO |
| Abstract exception used in many places | Concrete exception for a single domain |
| Generic utility extension | Business-logic service |
