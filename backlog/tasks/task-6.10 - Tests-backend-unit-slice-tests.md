---
id: TASK-6.10
title: 'Tests: backend unit & slice tests'
status: Done
assignee: []
created_date: '2026-04-30 16:41'
updated_date: '2026-05-01 09:42'
labels:
  - testing
  - backend
  - unit
dependencies:
  - TASK-6.4
  - TASK-6.5
  - TASK-6.6
  - TASK-6.7
parent_task_id: TASK-6
priority: medium
ordinal: 10000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Write JUnit 5 unit tests and `@WebMvcTest` slice tests for all new backend components introduced in TASK-6.

## Coverage Targets

### Validators (pure unit tests)
- `UsernameValidator`: valid patterns, too short, too long, illegal characters.
- `PasswordValidator`: all passing rules, each failing rule individually, equals-username case, equals-email-local-part case.
- `DisplayNameValidator`: valid Unicode, control characters, too long, trim behaviour.
- `AvatarValidator`: correct magic bytes, mismatched magic bytes, oversized file.

### `RegistrationService` (unit, mock `IdentityProvider` + `UserRepository`)
- Happy path: returns created user.
- Duplicate username: throws `DuplicateUsernameException`.
- IdP failure on step 3: compensating delete is called.

### Controllers (`@WebMvcTest`)
- `RegistrationController`: 201 on valid request, 400 on each invalid field, 409 on duplicate.
- `UserProfileController`: 200/404 on GET, 200/400 on PATCH, 204 on DELETE.
- `AvatarController`: 200 on PUT valid PNG, 413/415/400 on invalid uploads, 404 on GET when absent.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 All validator unit tests pass with no Spring context loaded.
- [x] #2 RegistrationService compensating-delete test verifies IdentityProvider.deleteUser is called when DB insert throws.
- [x] #3 WebMvcTest for RegistrationController covers at least: valid 201, invalid username 400, duplicate 409, weak password 400.
- [x] #4 WebMvcTest for AvatarController covers: valid upload, size exceeded, wrong content-type, magic-byte mismatch, missing avatar GET.
- [x] #5 No test mocks the database (slice tests use H2 or Testcontainers; unit tests mock only the direct collaborators).
- [x] #6 mvn test passes with all new tests green.
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added two missing @WebMvcTest tests to AvatarControllerTest: `uploadAvatar_FileTooLarge_Returns413` (verifies AvatarTooLargeException → HTTP 413) and `uploadAvatar_MagicBytesMismatch_Returns400` (verifies InvalidAvatarException → HTTP 400). All other acceptance criteria were already met by existing tests: PasswordValidatorTest (AC#1), RegistrationServiceTest with compensation-delete (AC#2), RegistrationControllerTest (AC#3), AvatarControllerTest now complete (AC#4), unit tests mock only direct collaborators (AC#5). All 246 tests pass with `mvn verify` including JaCoCo 85% coverage gate.
<!-- SECTION:FINAL_SUMMARY:END -->
