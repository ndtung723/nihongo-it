# Nihongo IT — Claude Code Skills

A modular skill set for the Nihongo IT project, distilled from 6 backend sprints + 5 frontend refactoring phases.

## Skills

| Skill | When to invoke |
|---|---|
| [frontend-conventions](frontend-conventions/SKILL.md) | Create/modify files under `services/` or `types/` in the frontend — naming, axios unwrap, type organization |
| [frontend-state-management](frontend-state-management/SKILL.md) | Create/modify Pinia stores — composition API, error flow, no toast inside stores |
| [frontend-composables](frontend-composables/SKILL.md) | Toast, debounce, confirm, async data, pagination, auth — don't write your own when one exists |
| [frontend-router-auth](frontend-router-auth/SKILL.md) | New routes, guards, auth flows — delegate to `requireAuth/requireAdmin` |
| [frontend-error-handling](frontend-error-handling/SKILL.md) | try/catch, error toasts — `extractApiError` + 3-layer service/store/component |
| [backend-microservice](backend-microservice/SKILL.md) | Kotlin services — common module, gateway header auth, BusinessException, Flyway |
| [build-and-verify](build-and-verify/SKILL.md) | Verify before commit — `gradle build -x test` + `npm type-check`, **never bootRun** |
| [feature-implementation-workflow](feature-implementation-workflow/SKILL.md) | End-to-end BE + FE feature — ordering, orchestrating other skills |

## Which skill to invoke when

```
User request               →  Primary skill
────────────────────────────────────────────────────
"Add endpoint X"           →  backend-microservice
                              + feature-implementation-workflow if FE is needed
"Create FE service"        →  frontend-conventions
"Store for X"              →  frontend-state-management
"Show a toast when..."     →  frontend-composables + frontend-error-handling
"Admin route /xxx"         →  frontend-router-auth
"End-to-end feature"       →  feature-implementation-workflow (orchestrator)
"Verify the build"         →  build-and-verify
"Update CLAUDE.md"         →  invoke claude-md-management:revise-claude-md (built-in)
```

## Invocation rules

1. **Read CLAUDE.md first** — it contains the core overrides
2. **1% rule** — if you think a skill might be ≥ 1% relevant, invoke it
3. **Invoke before acting** — don't reason from training data alone
4. **Skill priority**: process skills (brainstorm, debug) before implementation skills

## Skill layout

```
{skill-name}/
├── SKILL.md              # YAML frontmatter (name, description) + workflow
└── references/           # Templates, checklists, deep-dives when needed
    └── *.md
```

YAML frontmatter format:

```yaml
---
name: skill-name
description: One-line trigger description for routing
---
```

## Anti-patterns now enforced by skills

Every anti-pattern below existed at some point in the codebase, was caught, and was refactored — these skills exist to keep them from reappearing:

| Anti-pattern | Skill that prevents it |
|---|---|
| `useToast()` called directly in views/stores | frontend-composables, frontend-state-management |
| Repeated `err as { response?: ... }` | frontend-error-handling |
| Service named `categoryService.ts` (camelCase) | frontend-conventions |
| Types inlined inside service files | frontend-conventions |
| `window.confirm()` | frontend-composables |
| Manual `setTimeout` debounce | frontend-composables |
| Raw `localStorage.getItem("auth_token")` | frontend-router-auth, frontend-conventions |
| `router.beforeEach` with 20 lines of token checks | frontend-router-auth |
| Stores calling `useToast` | frontend-state-management |
| `bootRun` for verification | build-and-verify |
| Validating JWT inside downstream services | backend-microservice |
| Ad-hoc `ResponseEntity.badRequest()` | backend-microservice |
| Copying code from `common/` into services | backend-microservice |

## Adding new skills

When a new anti-pattern or convention is discovered:

1. Check whether an existing skill already covers it → extend that skill
2. If it's a new domain (e.g. "python NLP service") → create a new skill
3. Update this README with a corresponding entry
4. Do NOT create empty skills — every skill needs a concrete pattern + anti-pattern + verify command

## References

- Root `CLAUDE.md` — project-wide overrides
- `REFACTORING_PLAN.md` — full refactoring history
- Memory: `C:\Users\LENOVO\.claude\projects\D--workspace-nihongo-it\memory\MEMORY.md`
