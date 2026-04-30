---
id: TASK-7
title: Run make clean build and fix unit test coverage until mvn clean install passes
status: Done
assignee: []
created_date: '2026-04-30 20:57'
updated_date: '2026-04-30 21:14'
labels:
  - build
  - testing
  - coverage
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Run `make clean build` from the project root and resolve any unit test coverage failures so that `mvn clean install` (backend build with JaCoCo coverage gate) passes successfully without errors or skipped tests.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 make clean build completes with exit code 0
- [x] #2 mvn clean install passes with all unit tests green
- [x] #3 JaCoCo coverage gate is satisfied (no coverage threshold failures)
- [x] #4 No tests are skipped or suppressed to achieve the passing build
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added 6 test files to bring 4 packages above the 85% JaCoCo line-coverage gate:
- DtoTest.java — covers all 4 DTO classes (AuthRequest, AuthResponse, PersonRequest, PersonResponse)
- AuthenticationServiceTest.java — covers login and refreshToken (happy + error paths)
- JwtServiceTest.java — covers token generation, extraction, and validation
- JwtAuthenticationFilterTest.java — covers all doFilterInternal branches
- CustomUserDetailsServiceTest.java — covers known user and unknown user cases
- AuthControllerTest.java — covers /api/auth/login and /api/auth/refresh endpoints

make clean build now exits 0 with all 4 coverage violations resolved.
<!-- SECTION:FINAL_SUMMARY:END -->
