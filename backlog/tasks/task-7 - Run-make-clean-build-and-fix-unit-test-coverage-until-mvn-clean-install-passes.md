---
id: TASK-7
title: Run make clean build and fix unit test coverage until mvn clean install passes
status: To Do
assignee: []
created_date: '2026-04-30 20:57'
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
- [ ] #1 make clean build completes with exit code 0
- [ ] #2 mvn clean install passes with all unit tests green
- [ ] #3 JaCoCo coverage gate is satisfied (no coverage threshold failures)
- [ ] #4 No tests are skipped or suppressed to achieve the passing build
<!-- AC:END -->
