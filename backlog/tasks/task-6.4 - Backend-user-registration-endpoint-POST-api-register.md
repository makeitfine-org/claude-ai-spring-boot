---
id: TASK-6.4
title: 'Backend: user registration endpoint (POST /api/register)'
status: To Do
assignee: []
created_date: '2026-04-30 16:39'
labels:
  - backend
  - registration
  - validation
dependencies:
  - TASK-6.2
parent_task_id: TASK-6
priority: high
ordinal: 4000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement `POST /api/register` — the only public registration endpoint. Validates input, atomically provisions the IdP user and the local profile row, then triggers email verification.

## Request Body

```json
{ "username": "john_doe", "email": "john@example.com", "password": "S3cur3!Pass", "displayName": "John Doe" }
```

## Validation Rules

- **username**: `^[a-zA-Z0-9_]+$`, 5–15 chars, case-insensitively unique in local `users` table.
- **email**: RFC 5322 valid, unique in IdP realm.
- **password**: ≥8 chars, upper + lower + digit + special, not equal to username (case-insensitive) or email local-part. Backend pre-validates before calling IdP.
- **displayName**: 1–50 printable Unicode chars, no control characters, trimmed.

## Atomicity

1. Validate all fields (return 400 with field-level errors on failure).
2. Call `IdentityProvider.createUser(...)` → get `sub`.
3. Insert row into `users` table.
4. Call `IdentityProvider.triggerEmailVerification(sub)`.
5. On any step 3–4 failure: call `IdentityProvider.deleteUser(sub)` as compensation, then return 500.

## Response

- 201 Created with `{ "sub": "...", "username": "...", "email": "..." }` on success.
- 400 with structured error body on validation failure.
- 409 on duplicate username or email.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 POST /api/register with valid data returns 201 and creates both the IdP user and a local users row.
- [ ] #2 Invalid username pattern or length returns 400 with a field-level error message.
- [ ] #3 Duplicate username returns 409.
- [ ] #4 Duplicate email returns 409.
- [ ] #5 Password that fails policy (too short, missing character class, equals username) returns 400.
- [ ] #6 displayName with control characters is rejected with 400.
- [ ] #7 If local DB insert fails after IdP user creation, the IdP user is deleted (compensating rollback) and 500 is returned.
- [ ] #8 A verification email is dispatched to MailHog after successful registration.
<!-- AC:END -->
