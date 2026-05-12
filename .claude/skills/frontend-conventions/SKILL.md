---
name: frontend-conventions
description: Use when creating or modifying frontend service files, type definitions, or anything in src/services/, src/types/. Enforces naming conventions, type organization, axios unwrap patterns, and import boundaries discovered during the refactoring phases.
---

# Frontend Conventions — Nihongo IT

## When to invoke

- Creating or editing a file under `frontend/src/services/`
- Creating or editing a file under `frontend/src/types/`
- When the user says "add an endpoint", "create a new service", "declare a type", "API wrapper"
- When a component contains a raw `AxiosResponse<T>` — the service should unwrap it

## Naming conventions (must not be violated)

### Service files

| Correct | Wrong (already removed during refactoring) |
|---|---|
| `category.service.ts` | `categoryService.ts` |
| `topic.service.ts` | `topicService.ts` |
| `vocabulary.service.ts` | `vocabularyService.ts` |
| `auth.service.ts` | `authService.ts` |

**Rule:** lowercase with the `.service.ts` separator. When renaming, use `git mv` to preserve history.

### Type files

Organize by **domain**, not by category:

```
src/types/
├── common.types.ts       # JlptLevel, DateString, UUID, PagedResponse<T>, extractApiError
├── user.types.ts         # UserInfo, UserDetailInfo, auth req/resp, DashboardStats
├── learning.types.ts     # Category, Topic, VocabularyItem, FlashcardDTO, FlashcardStats
├── conversation.types.ts # Conversation, ConversationLine
├── notification.types.ts # NotificationType, NotificationItem
├── ai.types.ts           # ContentType, SpeechAnalysisResult, ChatResponse, FeedbackSummary
└── progress.types.ts     # progress tracking
```

Do NOT create a separate `vocabulary.types.ts` — fold it into `learning.types.ts`. Do NOT create a separate `category.types.ts` either.

## Axios unwrap pattern

### Public methods → return `Promise<T>` (already unwrapped)

```typescript
// ✅ Correct
import api from '@/utils/api'
import type { Category } from '@/types/learning.types'

const categoryService = {
  getAllCategories: (): Promise<Category[]> =>
    api.get('/api/v1/learning/categories').then(r => r.data),

  getCategoryById: (id: string): Promise<Category> =>
    api.get(`/api/v1/learning/categories/${id}`).then(r => r.data),
}
```

### Admin methods (sometimes `AxiosResponse` is required)

If a component genuinely needs `response.headers`, `response.status`, or a complex wrapper like `response.data.content` for a paged response, you may keep `AxiosResponse<T>`:

```typescript
// Only acceptable when a component truly needs this shape
adminGetAllVocabulary: (page = 0, size = 20): Promise<AxiosResponse<PagedResponse<VocabularyItem>>> =>
  api.get('/api/v1/learning/admin/vocabulary', { params: { page, size } }),
```

In most cases, **unwrap**. If a component only needs `data`, don't leak `AxiosResponse` out of the service.

### Auth token — go through tokenStore

```typescript
// ✅ Correct
import { getAccessToken } from '@/utils/tokenStore'
const token = getAccessToken()

// ❌ Wrong — this was the bug at ai.service.ts line 336
const token = localStorage.getItem("auth_token")
```

## Import types from `@/types`, NOT from services

```typescript
// ✅ Correct
import type { VocabularyItem } from '@/types/learning.types'
import vocabularyService from '@/services/vocabulary.service'

// ❌ Wrong — re-exporting from a service creates a reverse coupling
import type { VocabularyItem } from '@/services/vocabulary.service'
```

Service files **import** types from `@/types`; they do NOT **export** them. If you see `export type X` inside a service, that's a signal the type should be moved into `types/`.

## PagedResponse — use the common type

```typescript
// ✅ Correct
import type { PagedResponse } from '@/types/common.types'
type VocabPage = PagedResponse<VocabularyItem>

// ❌ Wrong — this was previously defined 3 times across service files
interface PagedVocabularyResponse {
  content: VocabularyItem[]
  totalElements: number
  // ...
}
```

## Workflow for creating a new service

1. **Domain check**: Which domain does the endpoint belong to? (learning/user/notification/ai/conversation) → decide which file in `types/`
2. **Type first**: Declare request/response/entity types in `src/types/{domain}.types.ts`
3. **Service file**: Create `src/services/{name}.service.ts` lowercase and import the types
4. **Method signature**: Public → unwrapped `Promise<T>`. Admin → also `Promise<T>` unless there's a real reason for `AxiosResponse`
5. **Errors**: Do NOT try/catch in services — let the axios interceptor + caller handle it
6. **Verify**: `npm run type-check` from `/frontend` — must be 0 errors

## Verify before committing

```bash
cd frontend
npm run type-check    # vue-tsc --build, MUST be 0 errors
```

The 3 pre-existing ESLint errors about test files outside `tsconfig` are unrelated to service work — ignore them.

## Quick reference: existing files

Before creating a new file, check whether a service already exists:

```
src/services/
├── admin.service.ts          # Admin user CRUD, dashboard stats
├── ai.service.ts             # OpenAI chat, TTS, vocabulary explanation
├── auth.service.ts           # Login, register, OAuth Google, refresh
├── category.service.ts       # Category public + admin
├── conversation.service.ts   # Conversation public + admin
├── feedback.service.ts       # User feedback
├── flashcard.service.ts      # FSRS flashcard study
├── notification.service.ts   # In-app notifications
├── statistics.service.ts     # User progress stats
├── topic.service.ts          # Topic public + admin
├── user.service.ts           # User profile self-service
├── vocabulary.service.ts     # Vocabulary public + admin (merged)
└── SpeechRecognitionService.ts  # Browser SpeechRecognition wrapper
```

When extending a feature: **add a method to the existing service** rather than creating a new file.

## References

- `references/service-template.md` — scaffold template for a new service
- `references/type-organization.md` — rules for grouping types by domain
