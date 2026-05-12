---
name: frontend-error-handling
description: Use when handling API errors, displaying user-facing error messages, or wrapping try/catch in stores and components. Enforces extractApiError + useAppToast pattern, separates store error state from UI display, avoids repeated cast patterns.
---

# Frontend Error Handling — Nihongo IT

## When to invoke

- Writing try/catch around an API call
- When the user says "handle the error", "show a toast on error", "API error"
- When you see the `err as { response?: ... }` pattern — that violates the convention
- When you see `useToast` inside a store — that violates SRP

## Three error-handling layers

```
┌────────────────────────────────────────────┐
│ Service: lets axios errors propagate       │  ← NO try/catch
└────────────────────┬───────────────────────┘
                     │
┌────────────────────▼───────────────────────┐
│ Store: catch → extractApiError → throw     │  ← Set state, NO toast
│ error.value = extractApiError(err, ...)    │
└────────────────────┬───────────────────────┘
                     │
┌────────────────────▼───────────────────────┐
│ Component: catch → toast.error(...)        │  ← UI concern
└────────────────────────────────────────────┘
```

## Layer 1: Service — NO try/catch

```typescript
// ✅ Correct — service is transparent to errors
getById: (id: string): Promise<Foo> =>
  api.get(`/api/v1/foos/${id}`).then(r => r.data)
```

Errors bubble up to the store/component. The service doesn't swallow, retry, or log them.

Exception: if a service has special retry/fallback logic → wrap it, but this is VERY rare. 99% of methods are just `.then(r => r.data)`.

## Layer 2: Store — set state + throw

```typescript
// ✅ Correct
import { extractApiError } from '@/types/common.types'

async function fetchById(id: string) {
  loading.value = true
  error.value = null
  try {
    currentItem.value = await fooService.getById(id)
    return currentItem.value
  } catch (err) {
    error.value = extractApiError(err, 'Không tải được mục')
    throw new Error(error.value)
  } finally {
    loading.value = false
  }
}
```

### `extractApiError(err, fallback)`

The single helper for parsing error messages. Definition:

```typescript
// src/types/common.types.ts
export function extractApiError(err: unknown, fallback = 'Đã xảy ra lỗi'): string {
  const e = err as { response?: { data?: { message?: string } } }
  return e?.response?.data?.message ?? fallback
}
```

The backend (`GlobalExceptionHandler`) returns `ErrorResponseDto.message` → a user-friendly Vietnamese string. `extractApiError` reads that string out.

### When to silent-fail (no throw)

The failure isn't critical and the view can still work without the data:

```typescript
async function fetchStats() {
  try {
    stats.value = await service.getStats()
  } catch {
    // Stats aren't critical — UI showing 0/empty is fine
    // Do NOT set error.value, so the view doesn't enter the error state
  }
}
```

Signs you should silent-fail: optional widget, secondary data, analytics-like fetch.

## Layer 3: Component — toast

```vue
<script setup lang="ts">
import { useAppToast } from '@/composables/useAppToast'
import { useFooStore } from '@/stores'

const toast = useAppToast()
const fooStore = useFooStore()

async function loadFoo(id: string) {
  try {
    await fooStore.fetchById(id)
  } catch (e) {
    toast.error((e as Error).message)
  }
}
</script>
```

### When a component calls a service directly (no store)

```typescript
import { useAppToast } from '@/composables/useAppToast'
import { extractApiError } from '@/types/common.types'

const toast = useAppToast()

async function handleSubmit() {
  try {
    await fooService.create(data)
    toast.success('Đã tạo')
  } catch (err) {
    toast.error(extractApiError(err, 'Tạo thất bại'))
  }
}
```

## Anti-patterns — already refactored

### ❌ Repeated cast pattern

```typescript
// Wrong — appeared 9+ times across stores/views before the refactoring
catch (err) {
  const e = err as { response?: { data?: { message?: string } } }
  error.value = e?.response?.data?.message || 'Login failed'
}

// Right
catch (err) {
  error.value = extractApiError(err, 'Login failed')
}
```

### ❌ Toast inside a store

```typescript
// Wrong — refactored away from vocabulary.ts (5 spots)
async function fetchById(id: string) {
  try { ... }
  catch (err) {
    error.value = extractApiError(err)
    const toast = useToast()              // ❌ store coupled to UI
    toast.error(error.value, { ... })
  }
}
```

### ❌ Swallowing errors inside a service

```typescript
// Wrong
async function getAll(): Promise<Foo[]> {
  try {
    const r = await api.get('/...')
    return r.data
  } catch (e) {
    console.error(e)
    return []                              // ❌ caller can't tell anything went wrong
  }
}
```

### ❌ Repeated toast options

```typescript
// Wrong — refactored across 16 files
toast.success('Đã lưu', { position: 'top', duration: 3000 })

// Right — defaults live in useAppToast
toast.success('Đã lưu')
```

## Complete pattern: admin form with validation

```vue
<script setup lang="ts">
import { ref } from 'vue'
import { useAppToast } from '@/composables/useAppToast'
import { extractApiError } from '@/types/common.types'
import categoryService from '@/services/category.service'
import type { CreateCategoryRequest } from '@/types/learning.types'

const toast = useAppToast()
const submitting = ref(false)
const form = ref<CreateCategoryRequest>({ name: '', meaning: '' })

async function submit() {
  if (!form.value.name || !form.value.meaning) {
    toast.warning('Vui lòng điền đủ thông tin')
    return
  }
  submitting.value = true
  try {
    await categoryService.adminCreateCategory(form.value)
    toast.success('Đã tạo danh mục')
    form.value = { name: '', meaning: '' }
  } catch (err) {
    toast.error(extractApiError(err, 'Không tạo được danh mục'))
  } finally {
    submitting.value = false
  }
}
</script>
```

## Validation errors from the backend (FieldErrorDto)

The backend returns `ErrorResponseDto` with `fieldErrors[]` when `@Valid` fails. The frontend currently doesn't parse the field-level info — it just shows the `message` (summary). If you need to highlight specific fields in a form, parse `err.response.data.fieldErrors` and set vee-validate errors per field.

This is an **enhancement that hasn't been done yet**, not the default.

## Verify

```bash
# Find any remaining cast patterns
grep -rn "err as { response" frontend/src/ --include="*.ts" --include="*.vue"
# Should be empty — everything has moved to extractApiError

# Find useToast imports inside stores
grep -rn "vue-toast-notification" frontend/src/stores/
# Should be empty

# Find repeated toast options
grep -rn "position: \"top\"" frontend/src/
# Should be empty — defaults handle this
```
