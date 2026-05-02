---
id: TASK-15
title: >-
  fix: frontend polls GET /api?timeout=32s without bearer token causing repeated
  AuthorizationDeniedException spam
status: To Do
assignee: []
created_date: '2026-05-02 11:25'
labels:
  - bug
  - security
  - frontend
  - backend
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

After login, the backend log fills with repeated `AuthorizationDeniedException: Access Denied` stack traces triggered by the frontend polling `GET /api?timeout=32s`.

The `BearerTokenAuthenticationFilter` reports _"Did not process request since did not find bearer token"_, so the JWT is never attached to this request. The anonymous user hits the authorization check and is denied, producing a full stack trace every ~32 seconds.

### Observed log pattern (repeats continuously)

```
BearerTokenAuthenticationFilter : Did not process request since did not find bearer token
...
AuthorizationDeniedException: Access Denied
  at AuthorizationFilter.doFilter(AuthorizationFilter.java:99)
```

### Root cause hypothesis

The `?timeout=32s` parameter indicates a long-polling or SSE keep-alive call. The frontend either:
1. **Omits the `Authorization: Bearer <token>` header** on this specific request, or
2. **Calls the endpoint before the token is available** (race condition on startup/refresh).

## Steps to Reproduce

1. `docker compose up`
2. Open the frontend and log in
3. Watch `docker compose logs -f backend` — the stack trace repeats every ~32 s

## Expected Behaviour

`GET /api?timeout=32s` is sent with the JWT bearer token (if the endpoint requires auth), OR the endpoint is explicitly permitted as public (if it is a health/SSE probe that should not require auth). Either way, no `AuthorizationDeniedException` in the logs after a successful login.

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GET /api?timeout=32s no longer produces AuthorizationDeniedException stack traces after login
- [ ] #2 If the endpoint requires auth, the frontend attaches the Authorization: Bearer header on this call
- [ ] #3 If the endpoint is a public probe, SecurityConfig permits it without authentication
- [ ] #4 No regression: protected endpoints still reject unauthenticated requests
- [ ] #5 `make clean build` passes
<!-- SECTION:DESCRIPTION:END -->
<!-- AC:END -->
