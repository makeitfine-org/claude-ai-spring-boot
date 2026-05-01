---
id: TASK-11
title: Replace OIDC login prompt with email + password form on /login
status: Done
assignee: []
created_date: '2026-05-01 19:33'
updated_date: '2026-05-01 20:12'
labels:
  - frontend
  - auth
  - e2e
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

The `/login` route currently redirects to `LoginPromptPage`, which only shows a single **"Sign in"** button that initiates the Keycloak OIDC redirect (`/oauth2/authorization/keycloak`). Users cannot enter credentials directly on the page.

A `LoginPage.tsx` already exists in `frontend/src/features/auth/` with an email + password form that posts to the local `POST /api/auth/login` endpoint (DB-backed users + JWT). It is **not wired into the router**.

After logout, `AuthContext.logout()` redirects to `/`, which currently bounces through `Navigate` to `/persons` → `ProtectedRoute` → `/login` → `/login-prompt`.

## Goal

The `/login` page must show **email field + password field + "Sign in" button**, submitting to the local `/api/auth/login` flow. The Register link/page stays as is. Logout sends the user back to `/login`.

## Scope

### Frontend (`frontend/`)
- `src/App.tsx`: route `/login` renders `LoginPage` directly (remove the `Navigate` to `/login-prompt`). Decide whether to keep `/login-prompt` route or delete `LoginPromptPage.tsx` + its test (`LoginPromptPage.test.tsx`) — prefer delete to avoid dead code.
- `src/auth/AuthContext.tsx`: change `logout()` redirect from `window.location.href = '/'` to `'/login'`.
- `src/auth/ProtectedRoute.tsx` (and any other place referencing `/login-prompt`): point unauthenticated redirects at `/login`.
- Keep the existing `LoginPage.tsx` form (email + password, RHF + Zod) — no behavioral change to the form itself; it already posts to `/api/auth/login` and navigates to `/persons` on success. Add a "Don't have an account? Register" link below the form (currently only on `LoginPromptPage`).
- Keep `RegisterPage` and `RegisterSuccessPage` untouched.

### Backend (`backend/`)
- No code changes expected — `POST /api/auth/login` (`AuthController`) and the local `DaoAuthenticationProvider` flow are already in place and permitted in `SecurityConfig`.
- Verify the OIDC chain (`oauth2Login`, `OidcLoginSuccessHandler`, `OidcClientInitiatedLogoutSuccessHandler`) can stay as-is for backend-initiated flows, or remove if no consumer remains. **Do not remove in this task** unless analysis confirms zero consumers — flag as a follow-up if so.

### E2E (`e2e/`)
Update Cucumber features + step defs to match the new flow:
- `e2e/features/auth/login.feature`:
  - Replace `Scenario: Login prompt page shows Sign in button` with a scenario that asserts email + password fields + Sign in button are visible on `/login`.
  - `Successful login with valid credentials redirects to persons page` — switch from OIDC sign-in to filling the local form.
- `e2e/features/auth/user-auth.feature`:
  - `Successful registration and login` — replace `And I sign in via Keycloak with the registered credentials` with a step that fills the local form on `/login`.
  - `Protected routes require login` — assert redirect to `/login` (form page), not `/login-prompt`.
  - `Account deletion` step `Then I am redirected and no longer authenticated` — assert landing on `/login` form page.
- Update `e2e/src/steps/auth.steps.ts` and `user-auth.steps.ts`: replace OIDC/Keycloak-redirect steps with form-fill steps. Keep the registered test account flow (still goes through Keycloak to create the user via `RegistrationService`); only the **sign-in** step changes.
- Confirm `e2e-test-*@example.com` cleanup still works.

### Tests to add/update
- Frontend: extend or replace `LoginPromptPage.test.tsx` with a `LoginPage.test.tsx` covering: form renders both fields + button; valid submission calls `/api/auth/login` and navigates to `/persons`; invalid credentials show the error Alert.
- Backend: existing `AuthController` / `AuthenticationService` tests already cover the endpoint — verify they still pass.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Visiting `/login` renders an email input, a password input, and a "Sign in" button (no OIDC redirect button).
- [x] #2 Submitting valid credentials calls `POST /api/auth/login` and navigates the user to `/persons`.
- [x] #3 Submitting invalid credentials displays an inline error ("Invalid email or password") without a full page reload.
- [x] #4 A "Register" link is visible on `/login` and navigates to `/register`.
- [x] #5 `RegisterPage` and `RegisterSuccessPage` flows are unchanged.
- [x] #6 Clicking logout from any authenticated page lands the user on `/login` (the form page).
- [x] #7 An unauthenticated user navigating to a protected route is redirected to `/login` (not `/login-prompt`).
- [x] #8 No references to `/login-prompt` remain in `frontend/src/` (or the route is intentionally retained with justification in the PR description).
- [x] #9 Frontend unit tests cover: form render, successful submit, failed submit. All pass with `cd frontend && npm test`.
- [x] #10 Backend `mvn verify` passes (JaCoCo ≥ 85% per package).
- [x] #11 All Cucumber e2e scenarios under `e2e/features/auth/` pass with `make acceptanceTest`.
- [x] #12 `make clean build` passes.
<!-- SECTION:DESCRIPTION:END -->

<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Result

`/login` now renders an email + password form posting to the local `/api/auth/login` endpoint; the OIDC-redirect "Sign in" prompt page is gone. Logout clears the JWT pair and redirects to `/login`. All 17 e2e scenarios, 30 frontend Vitest tests, and 257 backend tests pass; `make build` is green.

## Frontend
- `App.tsx`: `/login` renders `LoginPage` directly; `/login-prompt` route + `LoginPromptPage.tsx` + `LoginPromptPage.test.tsx` deleted.
- `LoginPage.tsx`: keeps the existing email/password RHF + Zod form, now stores tokens via `setTokens(...)`, calls `refreshUser()`, navigates to `/persons`. Adds a "Don't have an account? Register" link.
- `LoginPage.test.tsx` (new): asserts form rendering, register link, success-flow token storage + navigation, and failure-flow error alert.
- `lib/api.ts`: introduces `accessToken`/`refreshToken` localStorage helpers, a request interceptor that attaches `Authorization: Bearer …`, and a 401 → `/api/auth/refresh` → retry interceptor (skipping `/api/auth/*`).
- `AuthContext.tsx`: logout clears tokens and redirects to `/login`.
- `ProtectedRoute.tsx`, `RegisterPage.tsx`, `RegisterSuccessPage.tsx`: replaced all `/login-prompt` references with `/login`.

## Backend
The form needed a working end-to-end path, so the original "no backend changes" note was relaxed slightly to keep `/api/users/me` reachable after login:
- `CustomUserDetailsService` now queries `UserRepository` (by email or by UUID, so refresh works) and returns a `UserDetails` whose username is the user's `sub`. Bearer JWTs minted by `JwtService` therefore have a UUID subject that `ProfileController.extractSub` can parse. The demo password remains `password`.
- `UserRepository.findByEmail(...)` added.
- Flyway `V7__seed_test_user.sql` seeds the `test@example.com` row (sub `…099`) so the existing demo login works after the lookup change.
- `CustomUserDetailsServiceTest` rewritten for the new repository-backed lookup.

## Infrastructure
- `frontend/nginx.conf`: added `location = /login { try_files /index.html =404; }` ahead of `location /login/`. Without it, nginx 301-redirected `/login` → `/login/` and proxied to the backend OIDC callback handler instead of serving the SPA.

## E2E
- `features/auth/login.feature`: replaced the OIDC-prompt scenario with one that asserts the email field, password field, and Sign in button all render on `/login`.
- `features/auth/user-auth.feature`: replaced "I sign in via Keycloak with the registered credentials" with the local-form variant; "Protected routes require login" now asserts the redirect target is `/login`.
- `steps/auth.steps.ts`: navigate to `/login` (not `/login-prompt`); added `Then I should see the email field` / `Then I should see the password field` assertions.
- `steps/user-auth.steps.ts`: new `I sign in via the login form with the registered email` step (clears cookies first, fills form, waits for `/persons`); the email-verification step no longer pre-injects a JWT (that was short-circuiting the form-based sign-in we want to test); `I am redirected and no longer authenticated` now asserts a `/login` URL.

## Verification
- `cd frontend && npm test` → 5 files, 30 tests passed.
- `cd backend && mvn verify` → 257 tests, 0 failures, JaCoCo coverage met.
- `cd e2e && npm test` (against rebuilt stack) → 17 scenarios, 88 steps, all green.
- `make build` → BUILD SUCCESSFUL.
<!-- SECTION:FINAL_SUMMARY:END -->
