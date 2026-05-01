---
id: TASK-8
title: Fix failing RegistrationServiceTest causing make build failure
status: Done
assignee: []
created_date: '2026-05-01 12:28'
updated_date: '2026-05-01 12:41'
labels:
  - bug
  - backend
  - tests
dependencies: []
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

`make build` fails at the backend stage. `mvn clean install` reports test failures in `pl.piomin.services.application.service.RegistrationServiceTest`:

```
Tests run: 257, Failures: 3, Errors: 2, Skipped: 0
```

All 5 failures are in `RegistrationServiceTest` (7 tests, 5 failing).

## Failure Details

**Failures (3) — Mockito `Wanted but not invoked`:**
- `register_ValidRequest_TrimsDisplayName` (line 94) — expects `userRepository.save(...)` but `RegistrationService.register` calls `userRepository.saveAndFlush(...)` (RegistrationService.java:68).
- Two other `register_*` tests with the same pattern: tests stub/verify `userRepository.save(...)` while production code uses `saveAndFlush(...)`.

**Errors (2) — Mockito `UnnecessaryStubbingException`:**
- `register_CreatesIdpUserWithCorrectData` (line 169)
- `register_EmailVerificationFailure_CompensatesByDeletingIdpUser` (line 151)

## Root Cause (likely)

`RegistrationService.register` was changed to use `userRepository.saveAndFlush(...)` instead of `save(...)`, but `RegistrationServiceTest` was not updated to match. Some stubs of `save(...)` are now unreachable, triggering Mockito's strict-stubbing `UnnecessaryStubbingException`.

## Files Involved

- `backend/src/main/java/pl/piomin/services/application/service/RegistrationService.java` (line 68 — `saveAndFlush`)
- `backend/src/test/java/pl/piomin/services/application/service/RegistrationServiceTest.java` (lines 94, 151, 169 + the other two failing methods)

## Fix Direction

Update the test to verify `saveAndFlush(...)` instead of `save(...)`, and remove the now-unnecessary stubbings of `save(...)`. Confirm production semantics — if `saveAndFlush` is intentional (e.g. needed for the IdP compensation flow), keep it and align the tests; otherwise revert to `save`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 RegistrationServiceTest passes (7/7 green)
- [x] #2 No Mockito UnnecessaryStubbingException raised in the suite
- [x] #3 Backend `mvn clean install` succeeds
- [x] #4 `make clean build` succeeds end-to-end
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Replaced all `userRepository.save(` and `userRepository).save(` calls in `RegistrationServiceTest` with `saveAndFlush(` to match the production code change in `RegistrationService.register` (line 68). 257 tests pass, `mvn clean install` succeeds with BUILD SUCCESS.
<!-- SECTION:FINAL_SUMMARY:END -->
