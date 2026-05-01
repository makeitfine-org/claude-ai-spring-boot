---
id: TASK-6.8
title: 'Frontend: registration & login/logout UI'
status: Done
assignee: []
created_date: '2026-04-30 16:40'
updated_date: '2026-05-01 09:01'
labels:
  - frontend
  - auth
  - react
dependencies:
  - TASK-6.3
  - TASK-6.4
parent_task_id: TASK-6
priority: high
ordinal: 8000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Build the React screens and routing for registration, login initiation, and logout. Login and password reset are IdP-hosted (redirect); registration is a custom form posting to `POST /api/register`.

## Screens & Components

### Registration Page (`/register`)
- Form fields: username, email, password, display name.
- Client-side validation matching backend rules (pattern, length, required).
- Show field-level error messages from API 400 responses.
- On 201: redirect to `/register/success` (message: "Check your email to verify your account").
- On 409: show inline "username/email already taken" error.

### Login
- "Sign in" button/link → `GET /oauth2/authorization/keycloak` (full-page redirect; no custom form).
- On return from IdP with session cookie: redirect to `/` or the originally requested page.

### Logout
- "Sign out" button in nav → `POST /api/logout` → redirect to `/`.

### Authenticated guard
- `PrivateRoute` wrapper that redirects unauthenticated users to `/login-prompt` (a page with the "Sign in" button).

## State

- A lightweight `AuthContext` holding `{ isAuthenticated, user }` populated by `GET /api/users/me` on app load.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Visiting /register renders the registration form with all four fields.
- [ ] #2 Submitting the form with invalid data shows field-level errors without a page reload.
- [ ] #3 Successful registration redirects to /register/success with an email-verification prompt.
- [ ] #4 The 'Sign in' button triggers a redirect to the Keycloak login page.
- [ ] #5 After login, the user is redirected back to the app and the nav shows their display name.
- [ ] #6 The 'Sign out' button logs the user out and redirects to /.
- [ ] #7 Navigating to a protected route while unauthenticated redirects to the login prompt page.
- [ ] #8 AuthContext reflects the authenticated state correctly on page refresh (re-fetches /api/users/me).
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented session-based auth UI: LoginPromptPage (Keycloak OIDC redirect), RegisterPage (Zod validation, POST /api/register), RegisterSuccessPage. AuthContext rewritten to use GET /api/users/me for auth state and POST /api/logout. api.ts simplified to withCredentials only. nginx and vite.config proxies added for /oauth2/ and /login/ BFF routes. Keycloak realm export updated with test user and redirect URIs. E2E auth steps fixed with LIFO-ordered Playwright routes: GET /api/users/me is mocked (local JWT sub is email not UUID), all other /api/** calls get Bearer token injected. Build passes with all 11 scenarios green.
<!-- SECTION:FINAL_SUMMARY:END -->
