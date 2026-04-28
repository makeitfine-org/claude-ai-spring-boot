# CLAUDE.md — claude-ai-spring-boot (root)

## Monorepo Layout

```
backend/    → Spring Boot 3.4.1 REST API (see backend/CLAUDE.md)
frontend/   → React 19 + Vite SPA       (see frontend/CLAUDE.md)
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
| Pre-merge quality gate | `code-reviewer` |
| Backend naming review | `backend-naming-reviewer` |
| Frontend naming review | `frontend-naming-reviewer` |

Delegate to subagents liberally — keep the main context window clean.
Load skills from `.claude/skills/` for targeted in-context capabilities
(e.g. `jpa-patterns` for N+1 issues, `api-contract-review` before releasing endpoints).

## Delivery Checklist (cross-cutting)

Before marking any task done:
1. `docker-compose.yml` reflects any new services or env vars
2. Root `README.md` updated if ports, services, or quick-start steps changed
3. GitHub Actions `.github/workflows/ci.yml` updated if pipeline steps changed
