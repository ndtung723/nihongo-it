---
name: frontend-state-management
description: Use when creating or modifying Pinia stores in frontend/src/stores/modules/. Enforces composition API style, error-flow separation (no toast in stores), and import boundaries learned during refactoring.
---

# Frontend State Management — Nihongo IT

## When to invoke

- Creating or editing a file under `frontend/src/stores/modules/`
- When the user says "add an action to the store", "store for feature X", "shared state"
- When you see a `useToast` import inside a store file — that's a violation

## Core rules

### 1. Composition API only

```typescript
// ✅ Correct
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useFooStore = defineStore('foo', () => {
  const items = ref<Foo[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const itemCount = computed(() => items.value.length)

  async function fetchItems() { ... }

  return { items, loading, error, itemCount, fetchItems }
})

// ❌ Wrong — previously in flashcards.ts, rewritten during refactoring
export const useFlashcardsStore = defineStore('flashcards', {
  state: () => ({ ... }),
  getters: { ... },
  actions: { ... }
})
```

All 5 current stores use composition API: `auth.ts`, `categories.ts`, `flashcards.ts`, `notifications.ts`, `vocabulary.ts`. Stay consistent.

### 2. NEVER call `useToast` from a store action (SRP)

```typescript
// ❌ Wrong — store mixed with UI concerns, formerly in vocabulary.ts (5 spots)
import { useToast } from 'vue-toast-notification'

async function fetchById(id: string) {
  try { ... }
  catch (err) {
    error.value = extractApiError(err)
    const toast = useToast()
    toast.error(error.value, { position: 'top', duration: 3000 })  // ❌
  }
}

// ✅ Correct — store only sets state + throws; the component does the toast
import { extractApiError } from '@/types/common.types'

async function fetchById(id: string) {
  try { ... }
  catch (err) {
    error.value = extractApiError(err, 'Failed to load')
    throw new Error(error.value)
  }
}

// Component:
const { fetchById } = useFooStore()
const toast = useAppToast()

async function load() {
  try {
    await fetchById(id)
  } catch (e) {
    toast.error((e as Error).message)
  }
}
```

**Exception**: if the failure isn't critical (e.g. a stats fetch failed), the store can silently swallow it — no throw needed:

```typescript
async function fetchStats() {
  try {
    stats.value = await service.getStats()
  } catch {
    // stats aren't critical — silent fail, UI shows empty state
  }
}
```

### 3. Use `extractApiError` instead of the repeated catch pattern

```typescript
// ❌ Wrong — pattern repeated 6+ times in auth.ts before refactoring
catch (err) {
  const e = err as { response?: { data?: { message?: string } } }
  error.value = e?.response?.data?.message || 'Login failed'
}

// ✅ Correct
import { extractApiError } from '@/types/common.types'
catch (err) {
  error.value = extractApiError(err, 'Login failed')
}
```

### 4. Import types from `@/types`, NOT from services

```typescript
// ✅ Correct
import categoryService from '@/services/category.service'
import type { Category } from '@/types/learning.types'

// ❌ Wrong — reverse coupling
import categoryService, { type Category } from '@/services/category.service'
```

## Store template

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import fooService from '@/services/foo.service'
import type { Foo } from '@/types/{domain}.types'
import { extractApiError } from '@/types/common.types'

export const useFooStore = defineStore('foo', () => {
  // State
  const items = ref<Foo[]>([])
  const currentItem = ref<Foo | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const itemCount = computed(() => items.value.length)
  const hasItems = computed(() => items.value.length > 0)

  // Actions
  async function fetchAll() {
    loading.value = true
    error.value = null
    try {
      items.value = await fooService.getAll()
    } catch (err) {
      error.value = extractApiError(err, 'Failed to load items')
      throw new Error(error.value)
    } finally {
      loading.value = false
    }
  }

  async function fetchById(id: string) {
    loading.value = true
    error.value = null
    try {
      currentItem.value = await fooService.getById(id)
      return currentItem.value
    } catch (err) {
      error.value = extractApiError(err, 'Failed to load item')
      throw new Error(error.value)
    } finally {
      loading.value = false
    }
  }

  function reset() {
    items.value = []
    currentItem.value = null
    error.value = null
  }

  return {
    items, currentItem, loading, error,
    itemCount, hasItems,
    fetchAll, fetchById, reset,
  }
})
```

## Registering the store

Pinia auto-discovers stores when you call `defineStore()`. You do NOT need to add anything to `stores/index.ts` (that file just re-exports the hooks):

```typescript
// stores/index.ts
export { useAuthStore } from './modules/auth'
export { useVocabularyStore } from './modules/vocabulary'
// ... add a matching line when you create a new store
```

## Existing stores — don't create duplicates

| Store | Domain |
|---|---|
| `auth.ts` | Login state, current user, token refresh |
| `categories.ts` | Category list cache |
| `flashcards.ts` | Due cards + stats cache |
| `notifications.ts` | In-app notifications |
| `vocabulary.ts` | Vocabulary detail + saved list |

Before creating a new store, ask: **does this state really need to be shared across multiple components?** If only one view uses it, keep it local; don't push it into a store.

## Verify

```bash
cd frontend
npm run type-check
```
