# Controller Checklist

Checklist for adding a new endpoint to a backend Kotlin service.

## Before writing code

- [ ] Which service does this endpoint live in? (user-service / learning-service / ai-service / notification)
- [ ] Is there a matching domain controller already? (e.g. `VocabularyController.kt`) → append, don't create a new file
- [ ] Path convention: `/api/v1/{service-prefix}/...` (user / learning / ai / notification)
- [ ] Is an admin endpoint needed → `/api/v1/{prefix}/admin/...`
- [ ] Does the matching frontend service have the method? → coordinate naming

## Inside the controller

- [ ] `@RestController` + `@RequestMapping` at the class level
- [ ] Constructor-inject the service (do NOT use `@Autowired` on fields)
- [ ] Method returns DTO/Page directly; do NOT wrap in `ResponseEntity` (unless a specific status is required)
- [ ] Actions that return no body → `@ResponseStatus(HttpStatus.NO_CONTENT)` + return Unit
- [ ] Get the userId via the `authentication: Authentication` parameter (gateway-injected)
- [ ] Bean validation: `@Valid @RequestBody` for POST/PUT
- [ ] Path variable: `@PathVariable id: UUID` — Spring auto-converts string UUIDs
- [ ] Default query param: `@RequestParam(defaultValue = "20") size: Int`

## In the service layer

- [ ] Throw `BusinessException(code, message, status)` instead of returning null/empty
- [ ] Code is UPPER_SNAKE_CASE: `VOCAB_NOT_FOUND`, `EMAIL_DUPLICATE`
- [ ] Vietnamese message is fine for user-facing strings
- [ ] Logging: `log.info`/`log.error` with `{}` placeholders, NOT string interpolation
- [ ] Transactions: `@Transactional` for writes, `@Transactional(readOnly = true)` for reads

## Security

- [ ] Does the endpoint need auth? → already configured in `SecurityConfig` (`/api/v1/{prefix}/**` requires auth)
- [ ] Admin endpoint? → path contains `/admin/` + config uses `.hasAuthority("ROLE_ADMIN")`
- [ ] User-scoped query? → filter by `authentication.name` (X-User-Id)
- [ ] Do NOT validate JWTs in controllers — the gateway already did

## Database

- [ ] Schema change needed? → create a Flyway migration `V{n}__{snake_case}.sql`
- [ ] Version increments without gaps
- [ ] Do NOT edit migrations that have already been merged — create a new one
- [ ] Index columns used in frequent queries (especially LIKE search)

## DTOs

- [ ] Request DTOs live in `dto/request/` or `dto/`
- [ ] Response DTOs live in `dto/response/` or `dto/`
- [ ] Entity → DTO conversion happens in the service layer, NOT in the controller
- [ ] Field naming matches the frontend type (camelCase, not snake_case)
- [ ] Date fields: `Instant` or `LocalDateTime`; Jackson serializes ISO 8601

## Testing (if applicable)

- [ ] MockMvc test for the controller
- [ ] Mock out the service layer
- [ ] Test both happy path + error case (BusinessException → expected HTTP status)
- [ ] Authentication mock: `@WithMockUser` or set the header manually

## Verify

```bash
cd services
./gradlew :{service-name}:build -x test    # compile check
./gradlew :{service-name}:test             # if tests exist
```

## Reference pattern

```kotlin
@RestController
@RequestMapping("/api/v1/learning/vocabulary")
class VocabularyController(
  private val vocabularyService: VocabularyService,
) {
  // Public list
  @GetMapping
  fun list(
    @RequestParam(required = false) keyword: String?,
    @RequestParam(required = false) jlptLevel: String?,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int,
    authentication: Authentication,
  ): Page<VocabularyDto> = vocabularyService.list(keyword, jlptLevel, page, size, authentication.name)

  // Get single
  @GetMapping("/{id}")
  fun get(@PathVariable id: UUID): VocabularyDto = vocabularyService.getById(id)

  // Action without a body
  @PostMapping("/{id}/save")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun save(@PathVariable id: UUID, authentication: Authentication) {
    vocabularyService.save(id, authentication.name)
  }

  // Search with validation
  @GetMapping("/search")
  fun search(
    @RequestParam @NotBlank(message = "Keyword không được trống") keyword: String,
  ): List<VocabularyDto> = vocabularyService.search(keyword)
}
```
