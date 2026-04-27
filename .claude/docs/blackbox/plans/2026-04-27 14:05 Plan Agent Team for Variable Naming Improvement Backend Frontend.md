# Plan: Agent Team for Variable Naming Improvement (Backend + Frontend)

## Context
The project already has `"teammateMode": "tmux"` set in `.claude/settings.json`, so Claude Code natively maps each spawned teammate to a separate tmux pane. The goal is to run two specialized agents in parallel — one reviewing Java naming in `backend/src/`, one reviewing TypeScript/React naming in `frontend/src/` — without them sharing context window.

---

## Approach

### Step 1 — Create two agent profile files

**File:** `.claude/agents/backend-naming-reviewer.md`

```markdown
---
name: backend-naming-reviewer
description: Reviews and improves Java variable, field, and method naming in the Spring Boot backend following Java conventions and project rules (no Lombok, MapStruct, records).
---

You are a Java naming specialist. Your only job is to audit and fix naming in backend/src/main/java/.

Rules:
- camelCase for variables, fields, methods; PascalCase for classes
- No abbreviations (use `personRepository`, not `pRepo`)
- Boolean fields/methods: prefix with `is`, `has`, `can`
- Constants: UPPER_SNAKE_CASE
- No Lombok — plain Java records or classes
- Do NOT change logic, only names + their usages

For each file: list proposed renames as old → new, then apply them.
```

**File:** `.claude/agents/frontend-naming-reviewer.md`

```markdown
---
name: frontend-naming-reviewer
description: Reviews and improves TypeScript/React variable, prop, and function naming in the frontend following React and TS conventions.
---

You are a TypeScript/React naming specialist. Your only job is to audit and fix naming in frontend/src/.

Rules:
- camelCase for variables, functions, hooks; PascalCase for components and types/interfaces
- Boolean props/vars: prefix with `is`, `has`, `show`
- Custom hooks: must start with `use`
- No single-letter variables except loop counters
- Event handlers: prefix with `handle` (e.g., `handleSubmit`)
- Do NOT change logic, only names + their usages

For each file: list proposed renames as old → new, then apply them.
```

---

### Step 2 — Launch agents in separate tmux panes

Two options (pick one):

**Option A — Claude Code TeamCreate (native, preferred)**

Use the `TeamCreate` tool from within a Claude Code session. With `"teammateMode": "tmux"` already set, each teammate automatically opens in a new tmux pane:

```
TeamCreate([
  { name: "backend-naming-reviewer", workdir: "backend/" },
  { name: "frontend-naming-reviewer", workdir: "frontend/" }
])
```

Each agent gets its profile from `.claude/agents/` and runs autonomously in its own pane.

**Option B — Shell launch script (no harness dependency)**

Create `.claude/scripts/naming-team.sh`:

```bash
#!/usr/bin/env bash
# Launches two Claude naming-reviewer agents in separate tmux panes
set -euo pipefail

SESSION="naming-review"
ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"

tmux new-session -d -s "$SESSION" -x 220 -y 50

# Pane 0: backend
tmux send-keys -t "$SESSION:0" \
  "cd '$ROOT' && claude --agent backend-naming-reviewer -p 'Review and fix all variable naming in backend/src/main/java/ per Java conventions. Work file by file.'" Enter

# Pane 1: frontend
tmux split-window -h -t "$SESSION:0"
tmux send-keys -t "$SESSION:0.1" \
  "cd '$ROOT' && claude --agent frontend-naming-reviewer -p 'Review and fix all variable naming in frontend/src/ per TypeScript/React conventions. Work file by file.'" Enter

tmux attach -t "$SESSION"
```

Make it executable: `chmod +x .claude/scripts/naming-team.sh`  
Run it: `./.claude/scripts/naming-team.sh`

---

### Step 3 — Coordinate results (optional)

After both panes finish, run the `code-reviewer` agent against the combined diff:

```
claude --agent code-reviewer -p "Review the naming changes made across backend/ and frontend/ — verify consistency, no logic regressions, naming conventions respected."
```

---

## Critical Files

| File | Action |
|---|---|
| `.claude/agents/backend-naming-reviewer.md` | Create (new agent profile) |
| `.claude/agents/frontend-naming-reviewer.md` | Create (new agent profile) |
| `.claude/scripts/naming-team.sh` | Create (Option B launch script) |
| `.claude/settings.json` | Already has `"teammateMode": "tmux"` — no change needed |

---

## Verification

1. Run `.claude/scripts/naming-team.sh` — two tmux panes should open
2. Each pane shows a Claude agent working independently in its submodule
3. After completion: `git diff --stat` shows changes only in `backend/src/` and `frontend/src/`
4. Run `mvn verify` to confirm backend compiles and all tests pass (JaCoCo 85% gate)
5. Run `npm run build` in `frontend/` to confirm TypeScript still compiles