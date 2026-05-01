# Blackbox — Session Log
# Append-only. See .claude/rules/blackbox-policy.md

<!-- git-snapshot 2026-05-01T07:17:07Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T07:23:43Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

## 2026-05-01T09:03:00Z
### Decisions
- TASK-6.8: AuthContext uses session-based auth (GET /api/users/me) instead of localStorage JWT
- E2E auth fix: Playwright routes registered in LIFO order — mock fulfills GET /api/users/me (local JWT sub is email, not UUID), Bearer token injected for all other /api/** calls via route.continue
- Route pattern must use API_BASE_URL (http://localhost:8080) not FRONTEND_URL — axios baseURL is http://localhost:8080 baked at Vite build time
### Constraints Stated by User
- Complete tasks sequentially; validate with `flock /tmp/make-build.lock make clean build` before committing each
### Files Modified
- e2e/src/steps/auth.steps.ts — LIFO route interception: mock /api/users/me, inject Bearer on all others
- frontend/src/auth/AuthContext.tsx — session-based; GET /api/users/me on mount, POST /api/logout
- frontend/src/lib/api.ts — withCredentials only, no JWT interceptors
- frontend/src/auth/ProtectedRoute.tsx — return null while isLoading, redirect to /login-prompt
- frontend/src/features/auth/LoginPromptPage.tsx — Sign in via /oauth2/authorization/keycloak
- frontend/src/features/auth/RegisterPage.tsx — Zod validation, POST /api/register
- frontend/src/features/auth/RegisterSuccessPage.tsx — email verification prompt
- frontend/nginx.conf — added /oauth2/ and /login/ proxy locations
- frontend/vite.config.ts — dev proxy for /api, /oauth2, /login, /actuator
- keycloak/realm-export.json — added test user and redirect URIs
- frontend/src/App.tsx — routing updates
- frontend/src/features/persons/PersonsPage.tsx — shows displayName/username from AuthContext
- backend/src/main/resources/application.yml — forward-headers-strategy: native
### Deferred
- Fix local JWT to use UUID subject (currently uses email); ProfileController UUID.fromString fails for local tokens — workaround is mocking /api/users/me in e2e
---

<!-- git-snapshot 2026-05-01T09:02:19Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
- .claude/settings.local.json
- backlog/tasks/task-6.6 - Backend-avatar-upload-serve-delete-PUT-GET-DELETE-api-users-me-avatar.md
- backlog/tasks/task-6.7 - Backend-global-authorization-rate-limiting-audit-log.md
- backlog/tasks/task-6.8 - Frontend-registration-login-logout-UI.md
- notes.md
<!-- end-snapshot -->
