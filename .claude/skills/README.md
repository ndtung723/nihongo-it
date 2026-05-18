# Nihongo IT — Claude Code Skills

A modular skill set for the Nihongo IT project.

## Skills

| Skill | When to invoke |
|---|---|
| [backend-microservice](backend-microservice/SKILL.md) | Kotlin services — common module, gateway header auth, BusinessException, Flyway |
| [build-and-verify](build-and-verify/SKILL.md) | Verify before commit — `gradle build -x test` + `npm type-check && npm run build`, **never bootRun** |
| [feature-implementation-workflow](feature-implementation-workflow/SKILL.md) | End-to-end BE + FE feature — ordering across the new Next.js stack |

## Which skill to invoke when

```
User request               →  Primary skill
────────────────────────────────────────────────────
"Add endpoint X"           →  backend-microservice
                              + feature-implementation-workflow if FE is needed
"End-to-end feature"       →  feature-implementation-workflow (orchestrator)
"Verify the build"         →  build-and-verify
"Update CLAUDE.md"         →  invoke claude-md-management:revise-claude-md (built-in)
```

## Frontend conventions (no dedicated skill)

The Vue/Pinia/Vuetify-specific skills (`frontend-conventions`, `frontend-state-management`, `frontend-composables`, `frontend-router-auth`, `frontend-error-handling`) were removed when the frontend was rewritten to Next.js. The new patterns are documented inline in the root `CLAUDE.md` — see sections **"Frontend — must not be violated"** and **"Anti-patterns previously refactored"**.

Create a dedicated `frontend-nextjs` skill only when enough Next.js 16 / React 19 / shadcn quirks accumulate to make CLAUDE.md unwieldy.

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

## Anti-patterns now enforced by skills + CLAUDE.md

Every anti-pattern below existed at some point in the codebase, was caught, and was refactored — these guardrails exist to keep them from reappearing:

**Backend (skill-enforced):**

| Anti-pattern | Where it's prevented |
|---|---|
| `bootRun` for verification | build-and-verify |
| Validating JWT inside downstream services | backend-microservice |
| Ad-hoc `ResponseEntity.badRequest()` | backend-microservice |
| Copying code from `common/` into services | backend-microservice |

**Frontend (CLAUDE.md-enforced):**

| Anti-pattern | Rule in CLAUDE.md |
|---|---|
| `toast()` from sonner directly | "Toasts" rule — use `useAppToast` |
| Repeated `err as { response?: ... }` | "Error handling" rule — use `extractApiError` |
| Service named `categoryService.ts` (camelCase) | "Service files" rule |
| Types inlined inside service files | "Types" rule |
| `window.confirm()` / manual `setTimeout` | "Confirm/debounce" rule |
| Raw `localStorage.getItem("auth_token")` | "Token access" rule |
| Stores calling `useAppToast` | "Stores (Zustand)" rule |
| `middleware.ts` (Next.js 15 era) | "Next.js 16 — file naming" rule |
| Sync `params.id` in dynamic routes | "Async params" rule |
| `z.coerce.number()` in form schemas | "Forms" rule |

## Adding new skills

When a new anti-pattern or convention is discovered:

1. Check whether an existing skill already covers it → extend that skill
2. If it's about Next.js / frontend → add the rule to CLAUDE.md (no skill yet)
3. If it's a new domain (e.g. "python NLP service") → create a new skill
4. Update this README with a corresponding entry
5. Do NOT create empty skills — every skill needs a concrete pattern + anti-pattern + verify command

## References

- Root `CLAUDE.md` — project-wide overrides
- `docs/superpowers/plans/2026-05-17-nextjs-migration.md` — Vue → Next.js migration plan + ~60 discoveries
- Memory: `C:\Users\LENOVO\.claude\projects\D--workspace-nihongo-it\memory\MEMORY.md`
