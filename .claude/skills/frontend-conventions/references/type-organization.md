# Type Organization

## Grouping rules

Types are grouped by **business domain**, not by technical category.

| File | Contains |
|---|---|
| `common.types.ts` | Primitive aliases (`JlptLevel`, `DateString`, `UUID`), `PagedResponse<T>`, helper `extractApiError()` |
| `user.types.ts` | `UserInfo`, `UserDetailInfo`, `UserPreferences`, auth req/resp, admin user CRUD types, `DashboardStats` |
| `learning.types.ts` | `Category`, `Topic`, `VocabularyItem`, `VocabularyFilter`, `FlashcardDTO`, `FlashcardStats`, `ReviewResponse`, `ExampleSentence`, `ChatMessage` |
| `conversation.types.ts` | `Conversation`, `ConversationLine` |
| `notification.types.ts` | `NotificationType`, `NotificationItem` |
| `ai.types.ts` | `ContentType`, `TTSCheckResponse`, `SpeechAnalysisResult`, `WordAnalysis`, `VocabularyExplanation`, `ChatResponse`, `FeedbackSummary` |
| `progress.types.ts` | User progress tracking |
| `roles.ts` | `ROLES` constant + `Role` type (NOT named `roles.types.ts` — this file has a constant export) |

## Process for adding a new type

1. **Domain check**: Which domain does this type belong to?
   - User-facing data (profile, auth, admin user list) → `user.types.ts`
   - Learning content (vocab, flashcard, category, topic) → `learning.types.ts`
   - Cross-cutting (paged, dates, IDs) → `common.types.ts`
2. **Find duplicates**: Grep to confirm the type doesn't already exist:
   ```bash
   grep -rn "interface UserInfo" frontend/src/
   ```
3. **Append to the domain file**; do NOT create a new file unless you're opening a new domain
4. **Use named exports** — no default exports in type files

## Lessons from refactoring

### Past bug: duplicate types with diverging fields

`UserInfo` previously had two definitions:
- `auth.service.ts`: minimal — `userId, email, fullName, roleId`
- `admin.service.ts`: extended — also `streakCount, points, lastLogin, ...`

Components imported different shapes from different places → TypeScript passed but runtime crashed when accessing missing fields.

**Resolution applied**: a single minimal `UserInfo` plus `UserDetailInfo extends UserInfo` for admin.

```typescript
export interface UserInfo {
  userId: UUID
  email: string
  fullName: string
  roleId: number
  // optional fields, may or may not be populated
  profilePicture?: string
  currentLevel?: JlptLevel
  jlptGoal?: JlptLevel
  isActive?: boolean
  isEmailVerified?: boolean
  createdAt?: DateString
  updatedAt?: DateString
}

export interface UserDetailInfo extends UserInfo {
  streakCount?: number
  points?: number
  flashcardCount?: number
  newCards?: number
  learningCards?: number
  masteredCards?: number
  // admin-only
  reminderEnabled?: boolean
  reminderTime?: string
  notificationPreferences?: string
  minCardThreshold?: number
}
```

### `FlashcardDTO.state` is a `string`, NOT a `number`

The backend FSRS uses a numeric enum (0–3), but the frontend compares string values `"new"`, `"learning"`, `"review"`, `"relearning"` in multiple places (`StatisticsView.vue`, `FlashcardStudyView.vue`). Don't "normalize" it to `number` — it will break those views.

### Form bindings need `string | undefined`, not the enum

```typescript
// Vuetify select needs string | undefined
export interface SignupRequest {
  currentLevel?: string  // NOT `JlptLevel` — Vuetify breaks when the user hasn't selected
  jlptGoal?: string
}
```

`JlptLevel` is a literal union; assigning `undefined` to it works, but a select component may emit empty `""` — use `string | undefined` only for form input types.

## Re-export pattern (only for backward compatibility)

If you remove an inline type from a service but legacy code still imports from that path:

```typescript
// at the end of auth.service.ts
export type { UserInfo, LoginRequest } from '@/types/user.types'
```

This is **temporary**. The goal state: every import comes directly from `@/types/...`. Once no callers remain, delete the re-export.
