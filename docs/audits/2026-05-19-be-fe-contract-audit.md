# Backend ↔ Frontend Contract Audit

**Date:** 2026-05-19
**Scope:** All Kotlin services (`services/*`) vs `frontend-user/` + `frontend-admin/`
**Policy:** Backend is the source of truth; FE must adapt.

## Severity legend

- 🔴 **HIGH** — runtime breakage or silent data loss (field always undefined, wrong path → 404).
- 🟡 **MEDIUM** — works today but FE types misrepresent BE shape; future maintenance hazard or dead defensive code.
- 🟢 **LOW** — type-only mismatch (BE returns more than FE surfaces, optional vs required, dead types). No user-visible impact.

---

## Cross-cutting issues

### C-1 🔴 Backend has two paged-response shapes
- `PagedVocabularyResponseDto` uses `{ content, page, size, totalElements, totalPages, lastPage }` ✅ matches `frontend/.../common.types::PagedResponse<T>`.
- `PagedResponse<T>` in `learning-service` (used by ConversationController, AdminConversationController) uses `{ content, totalPages, totalElements, currentPage, size }` ❌ — exposes `currentPage` instead of `page`.
- `NotificationController` inline map uses `{ content, totalElements, totalPages, page, size }` ✅.
- `AdminStatisticsController` user-list uses `{ users, totalItems, totalPages, currentPage }` (different again).

**Effect:** Conversation listing/paging in either FE returns `undefined` when code reads `.page`. Fix BE: rename `currentPage` → `page` in `services/learning-service/.../dto/ConversationDTOs.kt::PagedResponse<T>` and align statistics user-list with `PagedResponse<T>`.

### C-2 🟡 Inconsistent datetime serialization
Two formats coexist on BE:
- `LocalDateTime` with `@JsonFormat(pattern = "yyyy-MM-dd HH:mm")` — used in `UserDto.lastLogin`, `VocabularyResponseDto.createdAt`, `FlashcardDTO.due`, `ConversationDTO.{createdAt,updatedAt}`, `TopicDTO.{createdAt,updatedAt}`, `CategoryDTO.{createdAt,updatedAt}`.
- `Instant` (ISO-8601 default) — used in `VocabularyDto.createdAt`, `FlashcardDTO.{createdAt,updatedAt}`.

FE `DateString` is just `string`, so both work for display, but `new Date(...)` parsing differs. **Recommend:** standardize on `Instant` (ISO-8601) everywhere, drop the custom pattern.

### C-3 🟡 `@JsonInclude(NON_NULL)` is inconsistent
Some DTOs strip null fields (`VocabularyDto`, `ConversationDTO`), others always emit them (`UserDto`, `FlashcardDTO` does, but several admin endpoints' map-built responses do not). FE handles either, but documenting the convention prevents drift.

### C-4 🔴 Wrapped vs unwrapped response envelopes
BE wraps some responses in `{ data: ... }` and some not, with no consistent rule:
- Wrapped: `GetVocabularyResponseDto`, `GetFlashcardResponseDto`, `GetFlashcardsResponseDto`, `GetDueCardsResponseDto`, `GetStatisticsResponseDto`, `CreateVocabularyResponseDto`, `UpdateVocabularyResponseDto`, `CreateFlashcardResponseDto`, `ReviewFlashcardResponseDto`, `DeleteFlashcardResponseDto`, admin dashboard `/stats`, admin statistics endpoints.
- Unwrapped (raw DTO): `ConversationDTO` (get/create/update), `TopicDTO`, `CategoryDTO`, `UserDto` (admin user CRUD), notification endpoints, vocabulary save/remove notebook (returns raw `VocabularyDto`).

**Effect:** `frontend-admin/src/services/vocabulary.service.ts::getById/create/update` calls `r.data` directly, but BE returns `{ data: VocabularyDto }` → consumer receives the wrapper object, not the DTO. See V-1 below.

**Recommend:** pick one. Either (a) wrap everything in `ApiResponseEnvelope<T> { data: T, message?: String }`, or (b) drop the wrappers entirely. Mixed is the source of every other mismatch in this report.

---

## Domain audit

### A. Auth + User (`user-service/AuthController` + `UserController`)

#### A-1 🟡 `UserInfo` (FE) has fields BE doesn't send
FE `UserInfo` (in `user.types.ts`) declares `isEmailVerified`, `createdAt`, `updatedAt` as optional, but BE `UserDto.kt` does NOT include them. The fields are silently `undefined`.
**Fix:** either add these to `UserDto` (preferred — they exist on the entity) or drop them from FE `UserInfo`.

#### A-2 🟢 `GetCurrentUserResponse.status` field doesn't exist on BE
FE expects `{ status: 'OK'|'NG', userInfo? }`, BE only sends `{ userInfo? }`. Store reads `res.data.userInfo ?? null` directly, so the typed `status` is dead code.
**Fix:** drop `status` from `GetCurrentUserResponse` in both FE apps.

#### A-3 🟢 `LoginResponse.message` and `UpdateProfileResponse.{status,message}` are dead
BE login returns `LoginResponseDto { token }`. BE update-profile returns `Map<message>`. FE types declare additional fields never sent.

#### A-4 🟡 `UserPreferences.notificationPreferences` type mismatch
BE stores/returns this as `String?` (probably JSON-encoded), FE types it as `Record<string, boolean>`. If anything serializes/deserializes this client-side, casts will misbehave.
**Fix:** add a backend Jackson-side transform to materialize as object, or change FE type to `string | undefined`.

#### A-5 🟢 `GET /api/v1/user/user/profile` and `GET /api/v1/user/user/preferences` are untyped
`UserController` returns `ResponseEntity<Any>` with raw entity. Hard to audit without dedicated DTOs.
**Fix:** introduce `UserProfileDto` / `UserPreferencesDto`.

#### A-6 🟢 BE endpoints with no FE caller
- `POST /api/v1/user/auth/logout-all`
- `GET /api/v1/user/auth/verify-email?token`
Not necessarily wrong (verify-email is reached via email link landing page, not an in-app call) but the logout-all UI affordance does not exist.

---

### B. Admin User Management (`user-service/admin/AdminController`)

#### B-1 🔴 `getUserById` typed `UserDetailInfo` but BE returns `UserDto`
FE `frontend-admin/src/services/admin.service.ts::getUserById` returns `Promise<UserDetailInfo>` (which has `streakCount, points, flashcardCount, newCards, learningCards, masteredCards, …`). BE `AdminController.getUserById` returns plain `UserDto` (no extras). All extra fields will be `undefined` at runtime.
**Fix:** add a dedicated `AdminUserDetailDto` on BE that includes the extra fields, or change FE return type to `UserInfo`.

#### B-2 🔴 `deactivate/activate/changeUserRole` typed `Promise<SimpleResult>` but BE returns 204 No Content
BE returns `ResponseEntity.noContent().build()` — body is empty. `res.data` is `""`/`undefined`. FE `{ status, message }` is wrong.
**Fix:** change FE return type to `Promise<void>` and drop the `SimpleResult` interface.

#### B-3 🟢 `createUser` returns `UserInfo` (FE) but BE returns `UserDto`
Same shape, but FE `UserInfo` has fields not present on BE (`isEmailVerified, createdAt, updatedAt`). Same root cause as A-1.

---

### C. AI domain (`user-service/AiController` + `ai-service/{ChatController, SpeechController, TTSController}`)

#### C-1 🔴 Duplicate controllers expose the same routes
- `user-service/AiController` (`@RequestMapping("/api/v1/ai")`) exposes `ask-ai`, `ask-ai-options`, `translate`, `translate/economy`, `vocabulary/list`, `vocabulary/explain`, `vocabulary/chat`, `list-output`, `advisor`, `map-output`, `analyze-speech`, `speech/*`, `tts/*` — proxies (via Feign?) the ai-service.
- `ai-service/ChatController` (`@RequestMapping("/api/v1/ai/chat")`) exposes the same chat endpoints under a `/chat/` prefix.
- FE mixes both. `frontend-user/src/services/ai.service.ts`:
  - `explainVocabulary` → `/api/v1/ai/chat/vocabulary/explain` (ChatController) ✅
  - `chatAboutVocabulary` → `/api/v1/ai/vocabulary/chat` (AiController proxy) ✅
  - `streamVocabularyChat` → `/api/v1/ai/chat/vocabulary/chat/stream` (ChatController) ✅

**Effect:** confusing, but works. **Recommend:** pick one and delete the other. Routing through `ai-service` directly (with the gateway) avoids the duplicate proxy.

#### C-2 🟡 `getVocabularyExplanation` sends params BE ignores
FE sends `reading, partOfSpeech, explanation, language, exampleSentences`. BE `explainVocabulary` accepts only `term, pronunciation, meaning, topicName, example`. Extra params are silently dropped.
**Fix:** either expand BE to accept (rename `pronunciation` → `reading`?) or simplify FE to pass only what BE consumes. Note `explainVocabulary` and `getVocabularyExplanation` in FE are redundant — collapse into one.

#### C-3 🟡 `SpeechAnalysisResult`, `FeedbackSummary` not verifiable
BE `analyzeAudioEnhanced` and `summarizeFeedback` return `Any`. FE types claim specific fields. Verify with `speechAnalysisService` source or add DTOs.

#### C-4 🟢 `frontend-admin/src/types/ai.types.ts` is dead code
Admin doesn't call AI endpoints; the file is a copy of user app's. Either delete or document why kept.

#### C-5 🟢 Unused BE endpoints (no FE caller):
- `/api/v1/ai/analyze-speech` (deprecated path? `/api/v1/ai/speech/analyze-audio-enhanced` is used instead)
- `/api/v1/ai/speech/analyze-sample`, `/api/v1/ai/speech/sample-audio/{sampleId}`, `/api/v1/ai/speech/health`
- `/api/v1/ai/ask-ai`, `/api/v1/ai/ask-ai-options`
- `/api/v1/ai/translate`, `/api/v1/ai/translate/economy`
- `/api/v1/ai/vocabulary/list`, `/api/v1/ai/list-output`, `/api/v1/ai/advisor`, `/api/v1/ai/map-output`

---

### D. Vocabulary (`learning-service/VocabularyController` + admin)

#### D-1 🔴 `frontend-admin/src/services/vocabulary.service.ts::getById/create/update` ignores wrapper
- `getById` typed `Promise<VocabularyItem>` reads `r.data` — but BE returns `GetVocabularyResponseDto { data: VocabularyDto }`. Consumer gets the wrapper.
- `create` reads `r.data` — BE returns `CreateVocabularyResponseDto { message?, data? }`.
- `update` reads `r.data` — BE returns `UpdateVocabularyResponseDto { data: VocabularyDto }`.

**Fix:** apply the same `unwrap()` helper used in `frontend-user/src/services/flashcard.service.ts`, OR drop the BE wrapper. See C-4.

#### D-2 🟡 `saveVocabulary` returns void but BE returns the saved `VocabularyDto`
`frontend-user/src/services/vocabulary.service.ts::saveVocabulary` discards the response. Same for `removeSavedVocabulary`. Not breaking, but wastes the data BE computes.

#### D-3 🟢 `VocabularyResponseDto` is dead BE code
`services/learning-service/.../dto/VocabularyResponseDto.kt` has fields (`hiragana, kanji, katakana, exampleSentence, exampleSentenceTranslation, createdByEmail, …`) that don't match the current schema. No controller references it. **Delete it.**

#### D-4 🟢 `VocabularyItem` (FE) has client-only fields `aiExplanation, aiExamples, chatHistory`
Comment in file says "client-side only". Fine. Consider moving to a separate `VocabularyView` type to keep the wire DTO clean.

---

### E. Flashcard (`learning-service/FlashcardController`)

#### E-1 🔴 `ReviewResponse` (FE) expects `result` field BE doesn't send
FE `frontend-user/src/types/learning.types.ts::ReviewResponse = { result: {status, message}, data: FlashcardDTO }`. BE `ReviewFlashcardResponseDto` is `{ data: FlashcardDTO }` only.
**Fix:** drop `result` from FE type.

#### E-2 🟡 Rating range docs contradict validation
- Swagger description: `"0-4 where 0 is hardest, 4 is easiest"`.
- `ReviewRequest` validation: `@Min(1) @Max(4)` — 0 is rejected.
**Fix:** either accept 0 (FSRS standard "Again") on BE or correct the description.

#### E-3 🟡 Statistics endpoint shape vs FE access
BE `getStudyStatistics` → `{ data: StudyStatisticsDto { summary, cardsDueByDay, dailyReviews, retentionRateByDay, memoryStrengthDistribution, cardsByState, cardsByJlptLevel, reviewTrend, averageRating } }`. FE `flashcardService.getStudySummary` reaches into `.summary.totalCards` etc. BE `StudySummaryDto` has `{ totalCards, dueCardsNow, reviewsLast30Days, currentStreak, overallRetentionRate }` — keys `dueToday, newCards, learningCards, masteredCards, averageRetention` that FE reads do NOT exist on BE → all `undefined`.
**Fix:** Either align FE `FlashcardStats` to BE `StudySummaryDto` field names, or expand BE to emit FE-expected fields.

#### E-4 🟢 BE `FlashcardDTO` uses `Instant` for `createdAt/updatedAt`, `LocalDateTime` (custom format) for `due`
Inconsistent within the same DTO. Pick one. (See C-2 cross-cutting.)

#### E-5 🟢 FE doesn't call `GET /flashcards`, `GET /flashcards/paged`, `GET /flashcards/{id}`, `POST /flashcards`, `PUT /flashcards/{id}`, `DELETE /flashcards/{id}`
Only the vocabulary-linked variants and `/review`, `/due`, `/statistics` are used.

---

### F. Conversation (`learning-service/ConversationController` + admin)

#### F-1 🔴 `PagedResponse<T>::currentPage` vs FE `page`
See C-1 cross-cutting. **Causes conversation paging bugs** on both FE apps unless BE is fixed or FE unwraps `currentPage` manually.

#### F-2 🔴 Endpoints called by FE that don't exist on BE
`frontend-user/src/services/conversation.service.ts` calls:
- `POST /api/v1/user/user/saved-conversations/{id}` (saveConversation)
- `DELETE /api/v1/user/user/saved-conversations/{id}` (unsaveConversation)
- `GET /api/v1/user/user/saved-conversations/check/{id}` (checkSavedConversation)
- `GET /api/v1/user/user/saved-conversations` (getSavedConversations)

Grep of `services/` for `saved-conversations` → 0 matches. **No backend implements these.**
**Fix:** either add the BE endpoints (saved-conversations feature) or remove the FE methods.

#### F-3 🟡 Admin FE sends full `Conversation` to create/update
`frontend-admin/src/services/conversation.service.ts::create/update` posts a `Conversation` object (including `conversationId, createdAt, updatedAt, lineId, tempId`). BE `CreateConversationRequest`/`UpdateConversationRequest` don't accept those fields. Jackson will ignore unknowns, but the contract isn't honest. Build a dedicated form DTO on FE or relax the BE DTO.

#### F-4 🟢 FE `Conversation.jlptLevel: JlptLevel`, BE `ConversationDTO.jlptLevel: String?`
Loose vs strict — fine in practice if values match `N5..N1`.

---

### G. Topic + Category

#### G-1 🔴 `frontend-user/src/services/topic.service.ts::getTopicsByCategoryId` hits non-existent path
FE calls `GET /api/v1/learning/topics/category/{categoryId}`. User-facing `TopicController` does NOT expose that path (only admin `AdminTopicController` does). User-side path is `GET /api/v1/learning/categories/{categoryId}/topics`.
**Fix:** change FE to call the categories-nested path, OR add `/topics/category/{categoryId}` to `TopicController`.

#### G-2 🟢 `TopicDTO.jlptLevel` exists on BE but FE `Topic` doesn't expose it
Minor — add if topics gain JLPT filtering UI.

#### G-3 🟢 `CreateTopicRequest/UpdateTopicRequest` BE accept `description` field, FE includes it
But there's no `description` column in current entities — likely dead. Verify and drop.

---

### H. Furigana + Feedback

#### H-1 🟢 `POST /api/v1/learning/feedback` has zero FE callers
`FeedbackController` is wired but no UI path uses it. Either remove or document.

#### H-2 🟢 Furigana endpoint contract OK
`GET /api/v1/learning/furigana` → `[{text, reading?, isKanji}]` matches FE inline `Token` interface in `FuriganaTool.tsx`. (Aside: `Token` is inlined; consider moving to `types/learning.types.ts`.)

---

### I. Notification (`notification/NotificationController`)

#### I-1 🔴 Entire notification feature has no FE service
- FE has `notification.types.ts` in both apps with `NotificationItem` typed.
- FE has NO call to `/api/v1/notify/notifications/*` from either app.
- UI does not surface notifications anywhere.

Backend endpoints exist (list/unread-count/mark-read/mark-all/delete). Feature gap; either build the FE side or document as intentionally deferred.

#### I-2 🟡 Backend response shape inline (not a DTO)
`NotificationController` builds maps inline (`mapOf("id" to notificationId, …)`). Move to `NotificationDto` for type safety + Swagger schema.

---

### J. Admin Dashboard + Statistics

#### J-1 🟡 `statistics/users` list uses `{users, totalItems, totalPages, currentPage}` — different from `PagedResponse<T>` and from notification pagination
See C-1 cross-cutting. Pick one paged shape.

#### J-2 🟢 `UserStatistics.retentionRate, currentStreak` (FE) absent from BE response top-level
They live inside `summary`. Defensive fallback in FE — harmless but dead.

#### J-3 🟢 `UserStatisticsDetail.profileInfo` (FE) only declares 4 fields; BE returns 11
BE adds `isActive, isEmailVerified, streakCount, points, reminderEnabled, reminderTime, minCardThreshold`. Not surfaced in admin UI but available. Either expand FE type or accept loose coupling.

#### J-4 🟢 `memoryStrengthDistribution` BE returns `Map<String,Int>` (free-form keys), FE locks to `{weak?, medium?, strong?}`
Will display empty if BE keys differ. Verify BE actually emits those three labels in `FlashcardStatisticsService`.

---

## Suggested fix order

1. **Cross-cutting first**:
   - C-4 (pick wrap or no-wrap, apply globally) and C-1 (one paged shape) — these eliminate most domain-specific fixes.
2. **🔴 path/shape bugs**:
   - F-2 (saved-conversations endpoints missing)
   - G-1 (`topics/category/...` wrong path)
   - D-1 (admin vocabulary unwrap)
   - B-1, B-2 (admin user typings)
   - E-1 (ReviewResponse wrong shape), E-3 (study summary field names)
   - I-1 (decide: build or drop notification feature)
3. **🟡 medium**:
   - A-1 (add missing fields to `UserDto`)
   - A-4 (preferences string vs object)
   - C-2 (collapse vocabulary explain duplicates)
   - C-3 (add proper DTOs for speech analysis)
   - E-2 (rating range docs vs validation)
   - F-3 (admin conversation form DTO)
   - I-2 (NotificationDto)
4. **🟢 cleanup**:
   - Delete dead DTOs/types (VocabularyResponseDto, dead admin ai.types, dead LoginResponse/UpdateProfileResponse fields).
   - Delete unused BE endpoints OR document why they exist.
   - Standardize datetime to ISO-8601 Instant everywhere.

## Files referenced in this audit (top-level)

### Backend
- `services/user-service/.../controller/{AuthController, UserController, AiController}.kt`
- `services/user-service/.../controller/admin/AdminController.kt`
- `services/user-service/.../dto/{LoginRequest, LoginResponseDto, SignupRequest, SignupResponseDto, GetCurrentUserResponseDto, UserDto, UserCreateRequest, UserUpdateRequest, UserListResponse, RefreshTokenRequest, OAuthRequest, PasswordResetDtos, ChangePasswordRequestDto, UpdateProfileRequestDto}.kt`
- `services/learning-service/.../controller/{VocabularyController, FlashcardController, ConversationController, TopicController, CategoryController, FuriganaController, FeedbackController}.kt`
- `services/learning-service/.../controller/admin/{AdminVocabularyController, AdminTopicController, AdminCategoryController, AdminConversationController, AdminDashboardController, AdminStatisticsController}.kt`
- `services/learning-service/.../dto/{VocabularyDto, GetVocabularyResponseDto, CreateVocabularyRequestDto, CreateVocabularyResponseDto, UpdateVocabularyRequestDto, UpdateVocabularyResponseDto, PagedVocabularyResponseDto, VocabularyResponseDto, FlashcardDTO, GetFlashcardResponseDto, GetFlashcardsResponseDto, GetDueCardsResponseDto, CreateFlashcardRequestDto, CreateFlashcardResponseDto, ReviewRequest, ReviewFlashcardResponseDto, StudyStatisticsDto, GetStatisticsResponseDto, ConversationDTOs, TopicDTO, CategoryDTO, FeedbackDTO}.kt`
- `services/ai-service/.../controller/{ChatController, SpeechController, TTSController}.kt`
- `services/notification/.../controller/NotificationController.kt`

### Frontend (both apps unless noted)
- `src/types/{user.types, learning.types, conversation.types, ai.types, notification.types, common.types}.ts`
- `src/services/{vocabulary, topic, category, conversation}.service.ts`
- `frontend-user/src/services/{ai, flashcard}.service.ts`
- `frontend-admin/src/services/{admin, statistics}.service.ts`
- `frontend-admin/src/types/statistics.types.ts`
- `frontend-user/src/stores/auth.store.ts`, `frontend-admin/src/stores/auth.store.ts`
