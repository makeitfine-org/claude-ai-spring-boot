---
id: TASK-12
title: >-
  Registration without email verification + logout redirect to /login + e2e
  coverage
status: To Do
assignee: []
created_date: '2026-05-01 20:50'
labels:
  - auth
  - backend
  - frontend
  - e2e
dependencies: []
references:
  - >-
    backend/src/main/java/pl/piomin/services/application/RegistrationService.java
  - >-
    backend/src/main/java/pl/piomin/services/infrastructure/keycloak/KeycloakIdentityProvider.java
  - backend/src/main/java/pl/piomin/services/config/SecurityConfig.java
  - frontend/src/contexts/AuthContext.tsx
  - e2e/features/auth/user-auth.feature
  - e2e/features/auth/login.feature
  - e2e/src/steps/user-auth.steps.ts
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Why

Self-registered users currently cannot log in: `RegistrationService` triggers Keycloak email verification after creating the user, but the project has no SMTP/email pipeline configured, so the verification link is never received and the account stays unverified. Additionally, logout UX is inconsistent — the backend OIDC logout success handler redirects to `{baseUrl}` (i.e. `/`), while the SPA's `AuthContext` redirects to `/login` after clearing tokens. We want a consistent, friction-free flow: register → immediately log in → on logout always land on `/login`. E2E tests must enforce all three properties so the regression cannot reappear.

## Current state (for the implementer)

- `backend/src/main/java/pl/piomin/services/application/RegistrationService.java` — calls `identityProvider.triggerEmailVerification(sub)` after user creation. This is the root cause of the registration → login break.
- `backend/src/main/java/pl/piomin/services/infrastructure/keycloak/KeycloakIdentityProvider.java` — Keycloak user-creation payload; verify `emailVerified=true` is set.
- `backend/src/main/java/pl/piomin/services/config/SecurityConfig.java` (~lines 136–142) — logout success handler currently redirects to `{baseUrl}`; should land on `/login`.
- `frontend/src/contexts/AuthContext.tsx` (~line 64) — already does `window.location.href = '/login'` after manual logout; confirm the same applies on 401/session expiry paths.
- `e2e/features/auth/user-auth.feature` — currently includes an email-verification step that becomes obsolete and must be updated.
- `e2e/features/auth/login.feature` — natural place to add the "logout → /login" assertion.

## Out of scope

- Re-enabling email verification behind a feature flag (not needed; project has no SMTP).
- Password reset / forgot-password flows.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 POST /api/register creates a Keycloak user that is immediately able to authenticate — no email verification step required
- [ ] #2 RegistrationService no longer triggers email verification; the created Keycloak user has emailVerified=true (or equivalent flag) so login works on first attempt
- [ ] #3 After successful registration, the user can log in via POST /api/auth/login with the same email + password and receive valid tokens
- [ ] #4 Frontend logout (manual click) leaves the browser on /login
- [ ] #5 Backend /api/logout (OIDC RP-initiated logout) leaves the browser on /login
- [ ] #6 E2E scenario added/updated: register → log in → assert authenticated state, with no email/verification interaction
- [ ] #7 E2E scenario added/updated: authenticated user logs out → browser URL is /login
- [ ] #8 Obsolete email-verification steps removed from e2e/features/auth/user-auth.feature and matching step definitions
- [ ] #9 `make clean build` passes
<!-- AC:END -->
