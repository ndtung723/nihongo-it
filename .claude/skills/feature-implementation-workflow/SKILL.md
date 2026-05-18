---
name: feature-implementation-workflow
description: Use when implementing a new feature that spans backend + frontend (new endpoint + UI). Orchestrates the order — types first, backend endpoint, frontend service, store/component — across the Next.js 16 stack.
---

# Feature Implementation Workflow — Nihongo IT

## When to invoke

- The user wants a new end-to-end feature (BE + FE)
- "Add an X feature for user/admin"
- When multiple layers need coordination (controller → service → store → page)

## Overall workflow

```
1. Understand the requirement  → Brainstorm with the user if fuzzy
2. Backend first               → Endpoint → DTO → controller → service → migration
3. Frontend types              → Define in @/types/{domain}.types.ts (the relevant app)
4. Frontend service            → Add method to {domain}.service.ts
5. Frontend store (optional)   → Zustand if shared state needed
6. UI integration              → Page or component with hook + toast + error
7. Verify                      → type-check + lint + build for both sides
8. Commit                      → 1 commit/PR per complete feature
```

## Step 0: Pick the right app

There are TWO Next.js apps:
- `frontend-user/` — user-facing (vocabulary, flashcards, conversation, etc.)
- `frontend-admin/` — admin (users, content CRUD, statistics)

Many features touch only one. Some (like a new vocabulary endpoint) touch both — the user app consumes the public read endpoint, the admin app consumes the admin CRUD endpoint. Decide which app(s) the feature lives in BEFORE writing code.

Types/services are **duplicated** between the two apps (no shared package). When both apps need a new type, copy it to both `src/types/` directories.

## Step 1: Brainstorm (when the request is fuzzy)

If the user says "add a 'favorite vocabulary' feature" without clarity:
- What's the user flow? (click icon → save to server, or local?)
- Is there a dedicated page listing saved items? (already exists as `/vocabulary/saved`)
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

Add to the relevant app's `src/types/{domain}.types.ts`. Often unnecessary — most features extend an existing type. For our example, `VocabularyItem.isSaved` already exists, so no new type needed.

Remember the import boundary: services import from `@/types/...types`, NEVER define types inside service files.

## Step 4: Frontend service

Add methods to the relevant `{domain}.service.ts`:

```typescript
// frontend-user/src/services/vocabulary.service.ts
saveVocabulary: (id: string): Promise<void> =>
  api.post(`/api/v1/learning/vocabulary/${id}/save`, {}).then(() => undefined),

removeSavedVocabulary: (id: string): Promise<void> =>
  api.delete(`/api/v1/learning/vocabulary/${id}/save`).then(() => undefined),
```

Service methods return unwrapped `Promise<T>`. Use `unwrap<T>()` helper if the backend response is enveloped as `{data: T}`.

## Step 5: Store (only if shared state is needed)

Use Zustand if:
- Multiple components need to know the saved status
- A saved-items list needs caching (e.g. `useVocabularyStore.savedVocabulary`)

If only one component uses it → skip the store and call the service directly.

```typescript
// frontend-user/src/stores/vocabulary.store.ts
async toggleFavorite() {
  const current = get().currentVocabulary
  if (!current) return
  try {
    if (current.isSaved) await vocabularyService.removeSavedVocabulary(current.vocabId)
    else await vocabularyService.saveVocabulary(current.vocabId)
    set({ currentVocabulary: { ...current, isSaved: !current.isSaved } })
  } catch (err) {
    const msg = extractApiError(err, 'Không cập nhật được trạng thái')
    throw new Error(msg)  // ← throw to caller, do NOT toast from inside the store
  }
}
```

## Step 6: UI integration

Use `useAppToast` for feedback. Always wrap store/service calls in try/catch and surface errors via toast — the component, not the store, owns the user-facing message.

```typescript
// frontend-user/src/app/(app)/vocabulary/[id]/VocabularyDetailClient.tsx
'use client'

import { useAppToast } from '@/hooks/useAppToast'
import { useVocabularyStore } from '@/stores/vocabulary.store'

export function VocabularyDetailClient({ vocabId }: { vocabId: string }) {
  const toast = useAppToast()
  const toggleFavorite = useVocabularyStore((s) => s.toggleFavorite)

  async function handleToggle() {
    try {
      await toggleFavorite()
      toast.success('Đã cập nhật')
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Không cập nhật được')
    }
  }
  // ...
}
```

For dynamic routes, remember Next.js 16's async params:
```typescript
// frontend-user/src/app/(app)/vocabulary/[id]/page.tsx
interface Props { params: Promise<{ id: string }> }
export default async function Page({ params }: Props) {
  const { id } = await params
  return <VocabularyDetailClient vocabId={id} />
}
```

## Step 7: Verify both sides

Invoke the `build-and-verify` skill.

```bash
cd services && ./gradlew :learning-service:build -x test
cd frontend-user && npm run type-check && npm run lint && npm run build
# if admin app also changed:
cd frontend-admin && npm run type-check && npm run lint && npm run build
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
Migration → Entity → Repo → Service → Controller → FE Type → FE Service → Store/Component
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

## Frontend rules to follow

All listed in root `CLAUDE.md` — read them before adding files:
- Async `params` and `useSearchParams` Suspense rules
- Service file naming + `Promise<T>` return
- `extractApiError` + `useAppToast` for errors
- Zustand: throw from store, toast from component
- `proxy.ts` (not `middleware.ts`) for auth redirect
- Forms: `valueAsNumber: true` + `z.number().or(z.nan()).transform(...)` not `z.coerce.number()`
- Radix Select: sentinel value (e.g. `__all__`) not empty string

## Reference docs

- `backend-microservice/SKILL.md` — step 2
- `build-and-verify/SKILL.md` — step 7
- Root `CLAUDE.md` — all frontend conventions (sections "Frontend — must not be violated" and "Anti-patterns")
- `docs/superpowers/plans/2026-05-17-nextjs-migration.md` — historical context + Next.js 16 quirks catalog
