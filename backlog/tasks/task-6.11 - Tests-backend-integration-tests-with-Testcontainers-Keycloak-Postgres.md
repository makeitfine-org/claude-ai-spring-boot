---
id: TASK-6.11
title: 'Tests: backend integration tests with Testcontainers (Keycloak + Postgres)'
status: done
assignee: []
created_date: '2026-04-30 16:41'
labels:
  - testing
  - backend
  - integration
  - testcontainers
dependencies:
  - TASK-6.10
parent_task_id: TASK-6
priority: medium
ordinal: 11000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Write Spring Boot integration tests using Testcontainers to spin up real Keycloak and Postgres instances and verify end-to-end backend flows.

## Test Scenarios

### Registration flow
1. POST /api/register with valid data → 201; Keycloak user exists; local users row exists.
2. POST /api/register with duplicate username → 409.
3. POST /api/register with weak password → 400.
4. POST /api/register 6 times from same IP within window → 6th returns 429.

### Login & session
5. Authenticated session (obtained via Keycloak direct grant for testing) → GET /api/users/me returns 200.
6. No session → GET /api/users/me returns 401.

### Profile mutation
7. PATCH /api/users/me → display_name updated; audit_events row created.
8. PUT /api/users/me/avatar (valid PNG) → stored; GET /api/users/me/avatar streams it back.
9. DELETE /api/users/me/avatar → hasAvatar=false on subsequent GET /api/users/me.

### Account deletion
10. DELETE /api/users/me → 204; GET /api/users/me → 404; Keycloak user no longer exists; audit rows anonymised.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Testcontainers starts Keycloak (with realm import) and Postgres for the test suite without manual setup.
- [x] #2 All 10 scenarios listed above pass.
- [x] #3 The compensating-delete scenario (DB unavailable after IdP create) is tested: Keycloak user is removed.
- [x] #4 Rate-limit test confirms 429 + Retry-After on the 6th registration attempt.
- [x] #5 Audit anonymisation test confirms sub of deleted account rows starts with 'DELETED:'.
- [x] #6 mvn verify passes including integration tests.
<!-- AC:END -->
