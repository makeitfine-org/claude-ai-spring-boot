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

<!-- git-snapshot 2026-05-01T09:21:15Z -->
- .claude/docs/blackbox/audit.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T09:22:27Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T09:34:41Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
- backlog/tasks/task-6.9 - Frontend-profile-edit-avatar-upload-UI.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T09:43:04Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
- notes.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T09:59:56Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T10:14:05Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
- backlog/tasks/task-6.11 - Tests-backend-integration-tests-with-Testcontainers-Keycloak-Postgres.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T10:14:16Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
- backlog/tasks/task-6.11 - Tests-backend-integration-tests-with-Testcontainers-Keycloak-Postgres.md
<!-- end-snapshot -->

## 2026-05-01T10:45:00Z
### Decisions
- TASK-6.11: RegistrationService.save() changed to saveAndFlush() so DB constraint violation fires inside try-catch and compensating deleteUser() executes correctly
- TASK-6.11: JpaAuditingConfig extended with OffsetDateTime-aware DateTimeProvider bean to fix User entity audit fields
- TASK-6.11: UserRegistrationIntegrationTest uses unique X-Forwarded-For IPs per test to avoid rate-limiter cross-test interference
- TASK-6.11: Keycloak container uses GenericContainer with --import-realm and realm JSON mounted via MountableFile
### Constraints Stated by User
- No Lombok, no MapStruct, no Java records in backend
- Semantic commit messages ≤80 chars, no Co-Authored-By trailer
### Files Modified
- backend/src/main/java/pl/piomin/services/application/service/RegistrationService.java — save→saveAndFlush for compensating delete
- backend/src/main/java/pl/piomin/services/config/JpaAuditingConfig.java — OffsetDateTime DateTimeProvider
- backend/src/test/java/pl/piomin/services/integration/UserRegistrationIntegrationTest.java — new; 11 integration tests
- backend/src/test/resources/keycloak/test-realm.json — new; Keycloak test realm with backend-admin service account
### Deferred
- Nothing
---

<!-- git-snapshot 2026-05-01T12:15:14Z -->
- .claude/docs/blackbox/audit.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:15:16Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:16:22Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:16:25Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:16:36Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:16:42Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:17:46Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:17:46Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:17:46Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:18:08Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:18:17Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:18:18Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:18:20Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:18:27Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:19:18Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:28:09Z -->
- .claude/docs/blackbox/audit.md
- .claude/settings.local.json
<!-- end-snapshot -->

<!-- git-snapshot 2026-05-01T12:32:56Z -->
- .claude/docs/blackbox/audit.md
- .claude/docs/blackbox/session-log.md
- .claude/settings.local.json
<!-- end-snapshot -->
