---
name: frontend-composables
description: Use when adding toast notifications, debounced inputs, confirm dialogs, async data fetching, pagination, or auth checks in Vue components. Covers useAppToast, useDebounce, useConfirm, useAsyncData, usePagination, useAuth — replaces direct vue-toast-notification, window.confirm, manual setTimeout patterns.
---

# Frontend Composables — Nihongo IT

When you need one of the patterns below, **do not roll your own** — a composable already exists.

## Available composables

| Composable | Purpose | When to use |
|---|---|---|
| `useAppToast` | Toast with sensible defaults (top-right, dismissible) | Anywhere you need a toast |
| `useDebounce` | Debounce a ref value | Search inputs, filters |
| `useConfirm` | Modal confirm dialog | Delete, destructive actions |
| `useAsyncData` | Loading/error wrapper for fetch | Simple single-source views |
| `usePagination` | Pagination state | List views with pagination |
| `useAuth` | Current user + role check | Header, conditional rendering |

## 1. useAppToast — NEVER use raw useToast

```typescript
// ✅ Correct
import { useAppToast } from '@/composables/useAppToast'

const toast = useAppToast()

toast.success('Lưu thành công')
toast.error('Không thể tải dữ liệu')   // default duration is 5000 ms for errors
toast.warning('Phiên sắp hết hạn')
toast.info('Đang đồng bộ...')
```

```typescript
// ❌ Wrong — 16 files were refactored away from this pattern
import { useToast } from 'vue-toast-notification'
const toast = useToast()
toast.success('...', { position: 'top', duration: 3000 })  // ❌ defaults already cover this
```

**Notes**:
- Do NOT pass `{ position, duration }` — defaults are already set
- Do NOT import the CSS `vue-toast-notification/dist/theme-sugar.css` in views — it's already imported once in `main.ts`
- Override defaults when truly necessary: `toast.success('...', { duration: 10000 })`

## 2. useDebounce — NEVER hand-roll setTimeout

```typescript
// ✅ Correct — pattern used in admin views (Category, Topic, Vocabulary management)
import { ref, watch } from 'vue'
import { useDebounce } from '@/composables/useDebounce'

const searchQuery = ref('')
const debouncedSearch = useDebounce(searchQuery, 400)

watch(debouncedSearch, () => {
  fetchItems(0, pagination.value.itemsPerPage)
})
```

```typescript
// ❌ Wrong — refactored away from VocabularyManagementView, CategoryManagementView, TopicManagementView
let debounceTimeout: number | null = null
function debouncedSearch() {
  if (debounceTimeout) clearTimeout(debounceTimeout)
  debounceTimeout = setTimeout(() => fetchItems(), 500) as unknown as number
}
// <v-text-field @input="debouncedSearch">
```

Templates only need `v-model="searchQuery"` — no `@input` handler bound.

## 3. useConfirm — NEVER window.confirm

```typescript
// ✅ Correct
import { useConfirm } from '@/composables/useConfirm'

const { confirm } = useConfirm()

async function handleDelete() {
  const ok = await confirm({
    message: 'Bạn có chắc chắn muốn xóa?',
    type: 'warning',
    confirmText: 'Xóa',
  })
  if (!ok) return
  await service.delete(id)
}
```

```typescript
// ❌ Wrong — refactored away from TranslationView
if (window.confirm('Bạn có muốn xóa?')) { ... }
```

## 4. useAsyncData — loading/error wrapper

```typescript
// ✅ Correct for simple single-source views
import { useAsyncData } from '@/composables/useAsyncData'
import flashcardService from '@/services/flashcard.service'

const { data: stats, loading, error, refresh: fetchStats } = useAsyncData(
  () => flashcardService.getStudyStatistics() as Promise<FlashcardStats>
)

onMounted(fetchStats)
```

Template:
```vue
<v-progress-circular v-if="loading" indeterminate />
<v-alert v-else-if="error" type="error">
  Không tải được. <v-btn @click="fetchStats">Thử lại</v-btn>
</v-alert>
<template v-else-if="stats">...</template>
```

**When NOT to use useAsyncData**:
- Views with multiple independent data sources (each needing its own loading state)
- Fetches with complex post-completion side effects (e.g. chart init)
- Optimistic updates — use a store + a different composable

## 5. usePagination

```typescript
import { usePagination } from '@/composables/usePagination'

const { page, itemsPerPage, totalItems, totalPages } = usePagination({
  initialPage: 1,
  initialItemsPerPage: 20,
})
```

Pass `page - 1` to the backend (0-based) when calling the API:
```typescript
const r = await service.getPaged(page.value - 1, itemsPerPage.value)
totalItems.value = r.totalElements
```

## 6. useAuth

```typescript
import { useAuth } from '@/composables/useAuth'

const { isAuthenticated, isAdmin, currentUser } = useAuth()
```

Use this for conditional rendering in templates, NOT for route protection — routes use guards.

## Composite pattern: admin form with search + confirm delete

```vue
<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useAppToast } from '@/composables/useAppToast'
import { useDebounce } from '@/composables/useDebounce'
import { useConfirm } from '@/composables/useConfirm'
import categoryService from '@/services/category.service'
import { extractApiError } from '@/types/common.types'
import type { Category } from '@/types/learning.types'

const toast = useAppToast()
const { confirm } = useConfirm()

const items = ref<Category[]>([])
const loading = ref(false)
const searchQuery = ref('')
const debouncedSearch = useDebounce(searchQuery, 400)

async function fetchItems() {
  loading.value = true
  try {
    items.value = searchQuery.value
      ? await categoryService.adminSearchCategories(searchQuery.value)
      : await categoryService.adminGetAllCategories()
  } catch (err) {
    toast.error(extractApiError(err, 'Không tải được danh sách'))
  } finally {
    loading.value = false
  }
}

async function deleteItem(id: string, name: string) {
  const ok = await confirm({
    message: `Xóa "${name}"?`,
    type: 'warning',
    confirmText: 'Xóa',
  })
  if (!ok) return
  try {
    await categoryService.adminDeleteCategory(id)
    toast.success('Đã xóa')
    await fetchItems()
  } catch (err) {
    toast.error(extractApiError(err, 'Xóa thất bại'))
  }
}

watch(debouncedSearch, fetchItems)
onMounted(fetchItems)
</script>
```

## Verification after edits

```bash
cd frontend
grep -rn "vue-toast-notification" src/ --include="*.vue" --include="*.ts"
# Should only appear in: main.ts, composables/useAppToast.ts
```

```bash
grep -rn "window\.confirm" src/ --include="*.vue"
# MUST be empty
```
