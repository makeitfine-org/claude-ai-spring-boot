# Plan: Root-level `Makefile` for claude-ai-spring-boot

## Context

The project currently has no root-level build orchestration script — every workflow (clean, build backend, build frontend, run docker compose, run e2e tests, check GitHub Actions) requires the developer to `cd` into a subdirectory and run a different tool (`mvn`, `npm`, `docker compose`, `gh`). The user wants a single `Makefile` at repo root that mirrors the style of the `RENOVATION` project's Makefile they provided as a template, with Telegram success/failure notifications wrapping long-running targets.

Inputs gathered:
- Monorepo: `backend/` (Maven, no wrapper), `frontend/` (Vite npm), `e2e/` (Cucumber+Playwright, `npm test`).
- Backend artifact: `claude-ai-spring-boot:1.0.1`. Plain `mvn` (no `./mvnw`).
- Frontend npm scripts: `dev`, `build` (`tsc -b && vite build`), `lint`, `preview`.
- E2E npm script: `test` → `cucumber-js`.
- `docker-compose.yml` builds two local images. With compose project name = repo dir (`claude-ai-spring-boot`) and services `app` + `frontend`, the resulting image tags are `claude-ai-spring-boot-app:latest` and `claude-ai-spring-boot-frontend:latest` — matches the names the user listed for `clean`.
- No existing `Makefile`.

User decisions (from clarification):
- `build` runs full e2e: `buildBackend` → `buildFrontend` → `docker compose up -d` → `npm --prefix e2e test` → `docker compose down`.
- Telegram-wrap targets: `clean`, `build`, `acceptanceTest`, `docker_all`.

## Files to create

- `/home/eug/dev/projects/my/claude-ai-spring-boot/Makefile` — new file (no existing one to merge with).

No other files modified.

## Makefile design

### Targets (all `.PHONY`)

| Target | Action |
|---|---|
| `clean` | `docker compose down` (ignore failure); `docker rmi -f claude-ai-spring-boot-app:latest claude-ai-spring-boot-frontend:latest` (ignore "not found"); `cd backend && mvn clean`; `cd frontend && rm -rf dist node_modules package-lock.json && npm install`. Wrapped in Telegram ✅/❌. |
| `buildBackend` | `cd backend && mvn install` |
| `buildFrontend` | `cd frontend && npm install && npm run build` (note: `npm run build`, not `npm build` — the user's spec had a typo). |
| `acceptanceTest` | `cd e2e && npm install && npm test`. Telegram-wrapped. Assumes stack is up (separate target). |
| `build` | `clean` → `buildBackend` → `buildFrontend` → `docker compose up -d` → `cd e2e && npm install && npm test` → `docker compose down`. Telegram-wrapped. |
| `docker_all` | `build` (sans final `docker compose down`) → leaves stack running in foreground via `docker compose up`. Mirrors RENOVATION template. Telegram-wrapped. To avoid duplicating build-then-e2e-then-up, implement as: `buildBackend` → `buildFrontend` → `docker compose down` → `docker compose up`. |
| `docker_down` | `docker compose down` |
| `ghList` | `gh run list --limit 5` |
| `ghView` | `gh run view --web` |
| `default_message` | `make message "🔔 Execution finished (claude-ai-spring-boot) 🔔"` |
| `message` | Same Telegram bash function as RENOVATION template — uses `NOTIFICATION_TELEGRAM_BOT_TOKEN` + `NOTIFICATION_TELEGRAM_CHAT_ID` env vars. |
| `help` | Print formatted usage banner listing all targets grouped by category (Build / Docker / GitHub / Notification). |

### Reuse from RENOVATION template (verbatim, adapted)
- `define execute_commands` block — reused unchanged for ✅/❌ wrapping.
- `message` target body (Telegram bash function `tn`) — reused unchanged; only the project name in default text changes from "renovation" to "claude-ai-spring-boot".
- `%:` / `@:` catch-all to allow `make message "free text"` arg passing.
- `help` ASCII banner — reused with this project's targets.

### Key adjustments vs. user spec
1. **`docker down` → `docker compose down`** — `docker down` isn't a real command.
2. **`npm build` → `npm run build`** — `build` is a script, requires `run`.
3. **Image rmi tolerated** — use `docker rmi -f ... 2>/dev/null || true` so a missing image doesn't fail `clean`.
4. **`mvn` not `./mvnw`** — backend has no Maven wrapper.

## Verification

After approval and implementation, verify end-to-end:

1. `make help` — banner renders, lists every target.
2. `make ghList` — invokes `gh run list --limit 5` (read-only, safe).
3. `make message "test ping"` — sends to Telegram if env vars set; prints clear "Usage" if no arg.
4. `make clean` — confirm: compose down, both images removed, `backend/target` gone, `frontend/{dist,node_modules,package-lock.json}` regenerated, ✅ Telegram message arrives.
5. `make buildBackend` — produces `backend/target/claude-ai-spring-boot-1.0.1.jar`.
6. `make buildFrontend` — produces `frontend/dist/`.
7. `make build` — full pipeline ends with ✅ Telegram message; `e2e` cucumber report passes; `docker compose ps` shows nothing left running.
8. `make docker_all` — stack up; ✅ Telegram on graceful exit; `make docker_down` cleans up.

## Out of scope

- No CI workflow changes.
- No `.gitignore` changes (Makefile is committed).
- No symlink handling (RENOVATION template uses an external symlink; we commit the Makefile directly into this repo).