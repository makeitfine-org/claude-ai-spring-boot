---
id: TASK-10
title: 'Fix: OIDC callback redirect chain leaves browser on chrome-error page'
status: To Do
assignee: []
created_date: '2026-05-01 15:06'
labels:
  - bug
  - auth
  - oidc
  - keycloak
dependencies:
  - TASK-6.12
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

End-to-end scenario "Successful registration and login" (TASK-6.12) fails after the user clicks Sign in. Diagnostic logging captured the final URL as `chrome-error://chromewebdata/`, meaning the browser hit a navigation/connection error somewhere in the OIDC callback chain.

## Expected flow

1. SPA → `http://localhost:3000/oauth2/authorization/keycloak` (proxied to backend)
2. Backend → Keycloak `realms/claude-ai/protocol/openid-connect/auth?...&redirect_uri=http://localhost:3000/login/oauth2/code/keycloak`
3. Keycloak (auto-completes if already authenticated via email-verification step) → redirect to `http://localhost:3000/login/oauth2/code/keycloak?code=...`
4. Backend handles callback, creates session, redirects to `/persons` on the SPA.

## Evidence

- `OIDC_REDIRECT_BASE_URL=http://localhost:3000` is set in docker-compose.
- Keycloak realm export lists both `localhost:3000/*` and `localhost:8080/*` as valid redirect URIs.
- The MailHog email-verification step preceding the failure passes (Keycloak session is established).
- The browser still ends on `chrome-error://chromewebdata/` — suggests a step in the redirect chain hits an unreachable host or the nginx proxy lacks a route.

## Suggested investigation

- Inspect `frontend/nginx.conf` for a `/oauth2/` and `/login/oauth2/code/` proxy to backend.
- Trace the chain manually with `curl -L -v` or Playwright trace to see which hop fails.
- Confirm Spring's success URL is the SPA root, not a backend-only path.

## Acceptance Criteria
<!-- AC:BEGIN -->
- "Successful registration and login" scenario passes end-to-end.
- `@wip` tag removed from that scenario in `e2e/features/auth/user-auth.feature`.
- `make clean build` passes.
<!-- SECTION:DESCRIPTION:END -->

- [ ] #1 After registration + email verification + Sign in, the browser lands on /persons in the SPA with the user's display name in the nav.
- [ ] #2 @wip tag removed from the 'Successful registration and login' scenario in e2e/features/auth/user-auth.feature.
- [ ] #3 `make clean build` passes.
<!-- AC:END -->
