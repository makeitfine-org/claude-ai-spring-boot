# CLAUDE.md — claude-ai-spring-boot (root)

## Monorepo Layout

```
backend/    → Spring Boot 3.4.1 REST API (see backend/CLAUDE.md)
frontend/   → React 19 + Vite SPA       (see frontend/CLAUDE.md)
e2e/        → Cucumber + Playwright acceptance tests (see e2e/CLAUDE.md)
k8s/        → Kubernetes manifests (inside backend/)
docker-compose.yml → postgres:5432, app:8080, frontend:3000
```

Module-specific coding rules live in each module's own `CLAUDE.md`.

## Workflow Defaults

- Plan mode for any task with 3+ steps or an architectural decision
- Session logging: see `.claude/rules/blackbox-policy.md`
- Use **Context7 MCP** proactively for library/API docs — don't wait to be asked
- Commits: semantic message ≤ 80 chars, no `Co-Authored-By` trailer
- Lessons from corrections → `tasks/lessons.md`; review at session start
- When compacting, always preserve the full list of modified files and any test commands

## Agent Selection Guide

| Task | Agent |
|---|---|
| New REST endpoint / JPA entity / Spring service | `spring-boot-engineer` |
| Architectural decision / package restructure | `java-architect` |
| React component / frontend feature | `general-purpose` |
| Backend test gaps / JaCoCo failures | `test-automator` |
| Security config / JWT / auth flows | `security-engineer` |
| Dockerfile / docker-compose changes | `docker-expert` |
| k8s manifests / Skaffold / Helm | `kubernetes-specialist` |
| GitHub Actions pipeline changes | `devops-engineer` |
| New/fix Cucumber+Playwright e2e scenario | `test-automator` |
| Pre-merge quality gate | `code-reviewer` |
| Backend naming review | `backend-naming-reviewer` |
| Frontend naming review | `frontend-naming-reviewer` |

Delegate to subagents liberally — keep the main context window clean.
Load skills from `.claude/skills/` for targeted in-context capabilities
(e.g. `jpa-patterns` for N+1 issues, `api-contract-review` before releasing endpoints).

## Makefile

A root `Makefile` provides convenience targets for all common developer workflows. Run `make help` to see the full list.

| Target | What it does |
|---|---|
| `make build` | Full build: backend (Maven) + frontend (npm) + e2e tests (docker up/down) |
| `make buildBackend` | `cd backend && mvn clean install` |
| `make buildFrontend` | `cd frontend && npm install && npm run build` |
| `make acceptanceTest` | `cd e2e && npm install && npm test` (stack must be running) |
| `make dockerAll` | Full build then `docker compose up` (foreground) — use for first-run |
| `make dockerDown` | `docker compose down` |
| `make clean` | Docker down + remove images + `mvn clean` + reinstall frontend deps |
| `make updateFrontend` | Upgrade frontend deps with `npm-check-updates -u && npm install` |

Prefer `make <target>` over raw commands — targets chain steps correctly and emit pass/fail Telegram notifications.

## Delivery Checklist (cross-cutting)

Before marking any task done:
1. `docker-compose.yml` reflects any new services or env vars
2. Root `README.md` updated if ports, services, or quick-start steps changed
3. GitHub Actions `.github/workflows/ci.yml` updated if pipeline steps changed
4. If Dockerfile, docker-compose, or env vars changed: `cd e2e && npm test` to confirm all scenarios still pass
5. If any files in `backend`, `frontend`, `e2e` modules, except `CLAUDE.md` and `README.md`, were added/modified/deleted: run `make build` and confirm it passes before closing the task

<!-- BACKLOG.MD MCP GUIDELINES START -->

<CRITICAL_INSTRUCTION>

## BACKLOG WORKFLOW INSTRUCTIONS

This project uses Backlog.md MCP for all task and project management activities.

**CRITICAL GUIDANCE**

- **Task execution**: When asked to work on or complete a backlog task, move it to `in progress` status IMMEDIATELY — before writing any code or taking any implementation steps.

- If your client supports MCP resources, read `backlog://workflow/overview` to understand when and how to use Backlog for this project.
- If your client only supports tools or the above request fails, call `backlog.get_backlog_instructions()` to load the tool-oriented overview. Use the `instruction` selector when you need `task-creation`, `task-execution`, or `task-finalization`.

- **First time working here?** Read the overview resource IMMEDIATELY to learn the workflow
- **Already familiar?** You should have the overview cached ("## Backlog.md Overview (MCP)")
- **When to read it**: BEFORE creating tasks, or when you're unsure whether to track work

These guides cover:
- Decision framework for when to create tasks
- Search-first workflow to avoid duplicates
- Links to detailed guides for task creation, execution, and finalization
- MCP tools reference

You MUST read the overview resource to understand the complete workflow. The information is NOT summarized here.

</CRITICAL_INSTRUCTION>

<!-- BACKLOG.MD MCP GUIDELINES END -->
