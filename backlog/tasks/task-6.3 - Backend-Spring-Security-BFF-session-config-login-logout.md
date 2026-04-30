---
id: TASK-6.3
title: 'Backend: Spring Security BFF session config (login / logout)'
status: To Do
assignee: []
created_date: '2026-04-30 16:39'
labels:
  - backend
  - security
  - oidc
  - session
dependencies: []
parent_task_id: TASK-6
priority: high
ordinal: 3000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Configure Spring Security as a BFF (Backend-for-Frontend): backend completes the OIDC authorization code + PKCE flow, stores tokens server-side, and issues an HttpOnly session cookie to the SPA. The React app never handles tokens directly.

## Scope

- `SecurityFilterChain` with OAuth2 login (code flow), session management, and resource-server JWT validation for direct API calls.
- Session cookie: HttpOnly, Secure (in prod), SameSite=Lax.
- Login initiator: `GET /oauth2/authorization/keycloak` — redirects to IdP.
- Callback handler: standard Spring `/login/oauth2/code/keycloak`.
- Logout endpoint: `POST /api/logout` — invalidates server session + triggers IdP RP-initiated logout.
- Public paths: `POST /api/register`, `GET /actuator/health`. All other `/api/**` require authentication.
- Store `sub` claim from the ID token into the session on first login (used to look up local profile).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GET /oauth2/authorization/keycloak redirects to the Keycloak login page.
- [ ] #2 After successful login, backend sets an HttpOnly session cookie; no token appears in the response body or a JS-accessible header.
- [ ] #3 POST /api/logout invalidates the server session and redirects to IdP end_session_endpoint.
- [ ] #4 GET /api/persons (any protected endpoint) returns 401 without a valid session cookie.
- [ ] #5 POST /api/register and GET /actuator/health return expected responses without a session cookie.
- [ ] #6 The sub claim is available in the security context after login.
<!-- AC:END -->
