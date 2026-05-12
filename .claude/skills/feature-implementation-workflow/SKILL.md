---
name: feature-implementation-workflow
description: Use when implementing a new feature that spans backend + frontend (new endpoint + UI). Orchestrates the order — types first, backend endpoint, frontend service, store/component — and references domain-specific skills along the way.
---

# Feature Implementation Workflow — Nihongo IT

## When to invoke

- The user wants a new end-to-end feature (BE + FE)
- "Add an X feature for user/admin"
- When multiple layers need coordination (controller → service → store → view)

## Overall workflow

```
1. Understand the requirement  → Brainstorm with the user if fuzzy
2. Backend first               → Endpoint → DTO → controller → service → migration
3. Frontend types              → Define types in @/types/{domain}.types.ts
4. Frontend service            → Add method to {domain}.service.ts
5. Frontend store/view         → State management or inline in the view
6. UI integration              → Toast, confirm, loading state
7. Verify                      → type-check + build for both sides
8. Commit                      → 1 commit/PR per complete feature
```

## Step 1: Brainstorm (when the request is fuzzy)

If the user says "add a 'favorite vocabulary' feature" without clarity:
- What's the user flow? (click icon → save to server, or local?)
- Is there a dedicated page listing saved items? (already exists as `VocabularyStorageView`)
- Does admin see stats?

→ Invoke `superpowers:brainstorming` to clarify.

If the user is already specific ("POST /vocabulary/{id}/save returns 204; frontend calls it after the click") → jump straight to implementation.

## Step 2: Backend

Invoke the `backend-microservice` skill. Workflow:

### 2a. Migration (if schema changes)

```
services/{service}/src/main/resources/db/migration/V{n}__add_xxx.sql
```

### 2b. Entity + Repository

```kotlin
@Entity
@Table(name = "saved_vocabulary")
data class SavedVocabulary(
  @Id val id: UUID = UUID.randomUUID(),
  val userId: UUID,
  val vocabId: UUID,
  val savedAt: Instant = Instant.now(),
)

interface SavedVocabularyRepository : JpaRepository<SavedVocabulary, UUID> {
  fun existsByUserIdAndVocabId(userId: UUID, vocabId: UUID): Boolean
  fun deleteByUserIdAndVocabId(userId: UUID, vocabId: UUID)
}
```

### 2c. Service

```kotlin
@Service
class VocabularyService(
  private val savedRepo: SavedVocabularyRepository,
  private val vocabRepo: VocabularyRepository,
) {
  @Transactional
  fun save(vocabId: UUID, userId: String) {
    val userIdUuid = UUID.fromString(userId)
    if (!vocabRepo.existsById(vocabId)) {
      throw BusinessException("VOCAB_NOT_FOUND", "Không tìm thấy từ vựng", HttpStatus.NOT_FOUND)
    }
    if (savedRepo.existsByUserIdAndVocabId(userIdUuid, vocabId)) return  // idempotent
    savedRepo.save(SavedVocabulary(userId = userIdUuid, vocabId = vocabId))
  }
}
```

### 2d. Controller

```kotlin
@PostMapping("/{id}/save")
@ResponseStatus(HttpStatus.NO_CONTENT)
fun save(@PathVariable id: UUID, auth: Authentication) {
  vocabularyService.save(id, auth.name)
}
```

### 2e. Verify backend

```bash
cd services
./gradlew :learning-service:build -x test
```

## Step 3: Frontend types

Invoke the `frontend-conventions` skill. Add to `@/types/learning.types.ts`:

```typescript
// Often unnecessary — save actions usually don't need new types.
// VocabularyItem.isSaved already exists
```

In most cases the type is already sufficient — just verify the response shape.

## Step 4: Frontend service

Add methods to `vocabulary.service.ts`:

```typescript
saveVocabulary: (id: string): Promise<void> =>
  api.post(`/api/v1/learning/vocabulary/${id}/save`, {}).then(() => undefined),

removeSavedVocabulary: (id: string): Promise<void> =>
  api.delete(`/api/v1/learning/vocabulary/${id}/save`).then(() => undefined),
```

## Step 5: Store (only if shared state is needed)

Invoke the `frontend-state-management` skill if:
- Multiple views need to know the saved status
- A saved-items list needs caching

If only one view uses it → skip the store and call the service directly from the view.

## Step 6: UI integration

Invoke the `frontend-composables` + `frontend-error-handling` skills.

```vue
<script setup lang="ts">
import { useAppToast } from '@/composables/useAppToast'
import { extractApiError } from '@/types/common.types'
import vocabularyService from '@/services/vocabulary.service'

const toast = useAppToast()

async function toggleSave(item: VocabularyItem) {
  try {
    if (item.isSaved) {
      await vocabularyService.removeSavedVocabulary(item.vocabId)
      item.isSaved = false                       // optimistic
      toast.success('Đã bỏ lưu')
    } else {
      await vocabularyService.saveVocabulary(item.vocabId)
      item.isSaved = true
      toast.success('Đã lưu')
    }
  } catch (err) {
    toast.error(extractApiError(err, 'Không thực hiện được'))
  }
}
</script>
```

## Step 7: Verify both sides

Invoke the `build-and-verify` skill.

```bash
cd services && ./gradlew :learning-service:build -x test
cd frontend && npm run type-check && npm run build
```

## Step 8: Commit

One feature = one commit/PR. Branch `feature/<scope>-<desc>`.

```bash
git checkout -b feature/vocab-save-toggle
git add -A
git commit -m "feat(vocab): add save/remove vocabulary endpoints"
```

## Dependency order

Don't flip the order:

```
Migration → Entity → Repo → Service → Controller → FE Type → FE Service → Store/View
```

Why:
- Finishing the backend **first** lets you nail down the real response shape — no guessing
- FE types follow the actual shape, not "what we think it'll be"
- Test the endpoint with curl/Postman before writing FE code — isolates bugs

## When to parallelize BE + FE

If the user wants high velocity AND the response shape is clearly agreed via spec:
- FE codes against mock data first
- BE codes separately
- Integrate once both are done

For most small features — sequential is faster because you avoid back-and-forth fixing shapes.

## Cross-service note

If a feature touches 2 services (e.g. learning-service needs to check user-service):
- Do NOT create real cross-database FK constraints between the two service DBs
- Use the Feign client (already configured) or event-driven flows
- Service A stores service B's entity UUID and validates via API when needed

## Reference docs

- `frontend-conventions/SKILL.md` — steps 3, 4
- `frontend-state-management/SKILL.md` — step 5
- `frontend-composables/SKILL.md` — step 6
- `frontend-error-handling/SKILL.md` — step 6
- `backend-microservice/SKILL.md` — step 2
- `build-and-verify/SKILL.md` — step 7
