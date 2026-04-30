---
id: TASK-6.5
title: 'Backend: users table migration + profile API (GET/PATCH/DELETE /api/users/me)'
status: Done
assignee: []
created_date: '2026-04-30 16:39'
updated_date: '2026-04-30 21:56'
labels:
  - backend
  - profile
  - jpa
  - flyway
dependencies:
  - TASK-6.3
parent_task_id: TASK-6
priority: high
ordinal: 5000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Create the `users` Postgres table (via Flyway migration), the JPA entity, repository, and the profile management endpoints.

## DB Migration (Flyway)

```sql
CREATE TABLE users (
  sub                  UUID PRIMARY KEY,
  username             VARCHAR(15) NOT NULL UNIQUE,
  display_name         VARCHAR(50),
  email                VARCHAR(255) NOT NULL UNIQUE,
  avatar_bytes         BYTEA,
  avatar_content_type  VARCHAR(20),
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX users_username_lower_idx ON users (lower(username));
```

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | /api/users/me | Return full profile (no avatar bytes, just a flag `hasAvatar`) |
| PATCH | /api/users/me | Update `display_name` only |
| DELETE | /api/users/me | Permanently delete local row + IdP user; log out |

## Profile lazy-creation

On `GET /api/users/me`, if no row exists for the authenticated `sub`, return 404 (profile must be created via `/api/register`).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Flyway migration runs cleanly on a fresh Postgres container.
- [x] #2 GET /api/users/me returns the profile for the authenticated user; returns 404 if no local row exists.
- [x] #3 GET /api/users/me does not include avatar bytes in the response body (only hasAvatar boolean).
- [x] #4 PATCH /api/users/me updates display_name and sets updated_at; rejects control characters and lengths outside 1–50 with 400.
- [x] #5 PATCH /api/users/me cannot change username or email (those fields are ignored or rejected).
- [x] #6 DELETE /api/users/me removes the local users row, calls IdentityProvider.deleteUser, invalidates the session, and returns 204.
- [x] #7 All three endpoints return 401 without a valid session.
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Implemented GET/PATCH/DELETE /api/users/me profile endpoints. Users table migration and entity were already created in TASK-6.4.

**New files:**
- `application/dto/ProfileResponse.java` — includes hasAvatar flag, no avatarBytes
- `application/dto/UpdateProfileRequest.java` — displayName only, nullable, size/control-char validated
- `domain/exception/UserNotFoundException.java`
- `application/service/ProfileService.java` — getProfile/updateProfile/deleteProfile with UserNotFoundException
- `presentation/rest/ProfileController.java` — GET/PATCH/DELETE /api/users/me using @AuthenticationPrincipal Jwt

**Modified:** GlobalExceptionHandler (404 for UserNotFoundException), SecurityConfig (CSRF ignores for PATCH/DELETE /api/users/me)

**Tests:** 218 tests passing, JaCoCo 85% gate satisfied.
<!-- SECTION:FINAL_SUMMARY:END -->
