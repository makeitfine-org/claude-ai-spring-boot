---
id: TASK-6
title: 'User Registration, Login & Profile Management'
status: To Do
assignee: []
created_date: '2026-04-30 16:36'
updated_date: '2026-04-30 16:48'
labels:
  - auth
  - keycloak
  - oidc
  - profile
  - security
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Summary

Add self-service user registration, login, and profile management, backed by Keycloak as the initial Identity Provider. Auth integration is done via standards-based OIDC so the IdP can be swapped (Auth0, Authentik, Cognito, …) with configuration changes only — no Keycloak-specific APIs in application code.

## User Stories

- As a visitor, I can register with a unique username, email, and strong password, then verify my email to activate my account.
- As a registered user, I can log in and log out.
- As a logged-in user, I can edit my display name and upload/replace/remove my avatar.
- As a user who forgot my password, I can reset it via an email link.
- As a user, I can permanently delete my account (GDPR "right to be forgotten").

## Subtask Breakdown

This task is decomposed into 13 subtasks. Execute in dependency order:

### Layer 1 — Foundation (no dependencies, can run in parallel)
| ID | Title |
|---|---|
| TASK-6.1 | Keycloak & MailHog infrastructure setup |
| TASK-6.2 | Backend: IdentityProvider port + Keycloak adapter |
| TASK-6.3 | Backend: Spring Security BFF session config (login/logout) |

### Layer 2 — Core backend (depends on Layer 1)
| ID | Title | Depends On |
|---|---|---|
| TASK-6.4 | Backend: user registration endpoint (POST /api/register) | 6.2 |
| TASK-6.5 | Backend: users table migration + profile API | 6.3 |

### Layer 3 — Features (depends on Layer 2)
| ID | Title | Depends On |
|---|---|---|
| TASK-6.6 | Backend: avatar upload, serve & delete | 6.5 |
| TASK-6.7 | Backend: global authorization, rate limiting & audit log | 6.3, 6.5 |
| TASK-6.8 | Frontend: registration & login/logout UI | 6.3, 6.4 |

### Layer 4 — Profile UI (depends on Layer 3)
| ID | Title | Depends On |
|---|---|---|
| TASK-6.9 | Frontend: profile edit & avatar upload UI | 6.5, 6.6, 6.8 |

### Layer 5 — Tests (can run in parallel after their deps)
| ID | Title | Depends On |
|---|---|---|
| TASK-6.10 | Tests: backend unit & slice tests | 6.4–6.7 |
| TASK-6.11 | Tests: backend integration tests (Testcontainers) | 6.10 |
| TASK-6.12 | Tests: E2E Cucumber + Playwright | 6.8, 6.9 |
| TASK-6.13 | Tests: frontend component tests (Vitest + RTL) | 6.8, 6.9 |

## Architecture

### Identity & Auth

- **IdP-portable design**: backend integrates only via standard OIDC (authorization code + PKCE, OIDC discovery, JWKS). No vendor-specific Admin API calls in application code.
- **Token handling**: BFF pattern. Backend completes the OIDC code flow, stores tokens server-side, issues an HttpOnly, Secure, SameSite session cookie to the SPA. The React app never sees access/refresh tokens.
- **Login & password reset**: hosted by the IdP (redirect flow). React app initiates via `/oauth2/authorization/{provider}` and receives the session cookie on callback.
- **Logout**: backend endpoint that invalidates the session and triggers IdP RP-initiated logout.

### Registration

- Custom backend endpoint `POST /api/register` validates input, provisions the user in the IdP, creates the local profile row, and triggers email verification — atomically (compensating delete on failure).
- The IdP integration is encapsulated behind an `IdentityProvider` port (Spring interface). A `KeycloakIdentityProvider` adapter is the only implementation today; future providers add new adapters without touching callers.

### Profile Storage

- New backend table `users` keyed by IdP `sub` claim (UUID). Columns: `sub`, `username`, `display_name`, `email`, `avatar_bytes BYTEA`, `avatar_content_type`, `created_at`, `updated_at`.
- `Person` entity is unrelated and out of scope.

### Endpoints (new)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /api/register | public | Create account |
| GET | /api/users/me | required | Return profile |
| PATCH | /api/users/me | required | Update display_name |
| PUT | /api/users/me/avatar | required | Upload avatar (multipart) |
| DELETE | /api/users/me/avatar | required | Remove avatar |
| GET | /api/users/me/avatar | required | Serve avatar bytes |
| DELETE | /api/users/me | required | Permanent account deletion |

### Authorization

- All `/api/**` endpoints require an authenticated session, EXCEPT `POST /api/register` and `/actuator/health`.
- Existing endpoints (e.g. `/api/persons/**`) become authenticated. E2E scenarios will be updated to log in first.

## Validation Rules

### Username (immutable after registration)
- Pattern: `^[a-zA-Z0-9_]+$`
- Length: 5–15 characters
- Globally unique (case-insensitive)

### Display Name (editable)
- Length: 1–50 characters
- Any printable Unicode allowed (letters, digits, spaces, punctuation, emoji)
- Reject control characters; trim leading/trailing whitespace
- Not required to be unique

### Email
- Required at registration; RFC 5322 valid; unique per IdP realm
- Verification mandatory — login blocked until verified

### Password
- Minimum length: 8
- Must contain: at least one uppercase, one lowercase, one digit, one special character
- Must not equal the username (case-insensitive) or email local-part
- Rejected if found in a known-breached-password list (Keycloak built-in or HIBP integration)

### Avatar
- Formats: PNG or JPG only (validated by content type AND magic bytes)
- Max file size: 1 MB; max dimensions: 512×512 (backend downscales larger images)
- Stored as `BYTEA` in Postgres

## Cross-Cutting Concerns

- **Rate limiting**: Keycloak brute-force detection for login; backend IP rate limit on `POST /api/register`.
- **Audit log**: every change to `display_name`, `avatar` (including removal), and account deletion appended to an `audit_events` table with `sub`, `event_type`, `timestamp`, `before/after` summary.
- **GDPR**: account deletion removes the local profile row and the IdP user; audit entries are retained but anonymised (sub replaced with a tombstone).

## Infrastructure Changes

- Add `keycloak` service to `docker-compose.yml` with a pre-imported realm (`claude-ai`) containing:
  - Public SPA client (PKCE, redirect URIs for local + docker)
  - Confidential backend client
  - Realm-level password policy matching the rules above
  - Email settings (add `mailhog` dev SMTP container)
  - Brute-force detection enabled
- Backend `application.yaml`: Spring Security OAuth2 client + resource server config driven by `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET` env vars (no `keycloak.*` properties).
- Update root `README.md` quick-start with new ports/services.
- Update `.github/workflows/ci.yml` if pipeline steps change.

## Test Coverage

- **Backend unit + slice tests**: validators, `IdentityProvider` adapter, controllers (`@WebMvcTest`).
- **Backend integration tests** (Testcontainers): real Keycloak + Postgres; cover register → verify → login → profile update → avatar upload → delete account.
- **E2E** (`e2e/`, Cucumber + Playwright): new `.feature` file covering the full user journey, including login gating on previously public endpoints.
- **Frontend component tests** (Vitest + RTL): registration form, profile edit form, avatar uploader (happy path + validation errors).

## Out of Scope (deferred)

- Social login (Google/GitHub) — enabled later as IdP config only
- Multi-factor authentication
- Roles / RBAC beyond "authenticated user"
- Admin UI for managing other users
- Migrating `Person` ownership to users
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A new visitor can register, receive a verification email, verify, log in, and reach an authenticated page in the SPA.
- [ ] #2 Attempting to log in before email verification is rejected with a clear message.
- [ ] #3 A logged-in user can change display name and upload/replace/remove a PNG or JPG avatar within the documented limits; oversized or wrong-format uploads are rejected with 400.
- [ ] #4 Username cannot be changed after registration (no endpoint exposes this).
- [ ] #5 Forgot-password flow sends an email and lets the user set a new password meeting policy.
- [ ] #6 Deleting the account removes both the IdP user and the local profile; the user is logged out and cannot log in again with the same credentials.
- [ ] #7 All previously public /api/** endpoints (except /api/register and /actuator/health) return 401 without a valid session.
- [ ] #8 Swapping the IdP requires only changing OIDC_* env vars and (if needed) writing a new IdentityProvider adapter — no changes to controllers, services, or the SPA.
- [ ] #9 make build passes; backend, integration, e2e, and frontend tests all green.
<!-- AC:END -->
