# Plan: Create backlog task for registration/login/logout fix + e2e

## Context

User reported a set of auth-flow problems they want tracked as a single backlog work item:

- Registration must work end-to-end without requiring email confirmation.
- A user who just registered must be able to log in immediately.
- Logout must redirect the browser to `/login`.
- E2E coverage must exist for all of the above.

Current state (from exploration):

- `RegistrationService.java:73` calls `identityProvider.triggerEmailVerification(sub)` after user creation in Keycloak — so a fresh user is unverified and cannot log in until they click the email link. This is the root cause of the "registration works without email confirmation" requirement.
- Backend logout (`SecurityConfig.java:136-142`) uses `OidcClientInitiatedLogoutSuccessHandler` redirecting to `{baseUrl}` (root `/`), while the frontend `AuthContext.tsx:64` does `window.location.href = '/login'` after clearing tokens. The two paths are inconsistent — needs to land on `/login` in both cases.
- E2E features already exist under `e2e/features/auth/` (`user-auth.feature`, `login.feature`, `jwt-refresh.feature`); `user-auth.feature` currently includes an email-verification step that will need to be revisited once verification is removed.

## Recommended approach

Create **one backlog task** (single focused PR; backend + frontend + e2e all touch the same auth flow and should ship together). No subtasks — scope is small and tightly coupled.

### Task fields

- **Title**: `Registration without email verification + logout redirect to /login + e2e coverage`
- **Priority**: high
- **Status**: To Do
- **Labels**: `auth`, `backend`, `frontend`, `e2e`

### Description (the WHY)

Self-registered users currently cannot log in because Keycloak requires email verification, but the project has no SMTP/email pipeline configured. Additionally, logout lands on `/` from the backend OIDC handler but `/login` from the SPA, producing inconsistent UX. Goal: a registered user can log in immediately, and any logout (manual or session-expiry) lands on `/login`. E2E tests must enforce all three properties.

### Acceptance Criteria

- [ ] `POST /api/register` creates a Keycloak user that is immediately able to authenticate (no email verification step required).
- [ ] `RegistrationService` no longer triggers email verification; user is created as already verified (or the equivalent Keycloak flag is set).
- [ ] After successful registration, the user can log in via `POST /api/auth/login` using the same email + password and receive valid tokens.
- [ ] Logout (frontend logout action AND backend `/api/logout`) leaves the browser on `/login`.
- [ ] E2E scenario: register → log in → assert authenticated state — passes without any email/verification interaction.
- [ ] E2E scenario: authenticated user clicks logout → browser ends on `/login`.
- [ ] Existing `e2e/features/auth/user-auth.feature` is updated to remove obsolete email-verification steps.
- [ ] `make clean build` passes.

### Critical files (for the implementer)

- `backend/src/main/java/pl/piomin/services/application/RegistrationService.java` (remove `triggerEmailVerification` call; ensure created user has `emailVerified=true`)
- `backend/src/main/java/pl/piomin/services/infrastructure/keycloak/KeycloakIdentityProvider.java` (verify user-creation payload sets `emailVerified`)
- `backend/src/main/java/pl/piomin/services/config/SecurityConfig.java` (logout success handler → `/login`)
- `frontend/src/contexts/AuthContext.tsx` (already redirects to `/login` — confirm both manual logout and 401 paths)
- `e2e/features/auth/user-auth.feature` and matching steps in `e2e/src/steps/user-auth.steps.ts`
- Possibly `e2e/features/auth/login.feature` for the logout-redirect scenario

### Verification

1. `cd backend && mvn clean verify`
2. `make dockerAll` then in another shell `cd e2e && npm test`
3. Manual: register a new account, confirm immediate login works, click logout, confirm URL is `/login`.
4. `make clean build` — required by project Delivery Checklist.