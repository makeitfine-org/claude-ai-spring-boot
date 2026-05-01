---
id: TASK-6.12
title: 'Tests: E2E Cucumber + Playwright user journey'
status: Done
assignee: []
created_date: '2026-04-30 16:41'
updated_date: '2026-05-01 17:05'
labels:
  - testing
  - e2e
  - cucumber
  - playwright
dependencies:
  - TASK-6.8
  - TASK-6.9
parent_task_id: TASK-6
priority: medium
ordinal: 12000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Write Cucumber feature files and Playwright step definitions covering the full user journey, including the impact on previously public endpoints.

## Feature: User Registration and Profile Management (`user-auth.feature`)

```gherkin
Feature: User registration and profile management

  Scenario: Successful registration and login
    Given I visit the registration page
    When I fill in valid registration details
    And I submit the form
    Then I see a verification email prompt
    When the email is verified (via MailHog API)
    And I sign in via Keycloak
    Then I am redirected to the app and see my display name in the nav

  Scenario: Protected routes require login
    Given I am not authenticated
    When I navigate to a protected page
    Then I am redirected to the login prompt

  Scenario: Previously public endpoint now requires authentication
    Given I am not authenticated
    When I send GET /api/persons without a session
    Then the response status is 401

  Scenario: Edit display name
    Given I am logged in
    When I navigate to my profile page
    And I edit my display name to "New Name"
    Then my display name shows "New Name" in the nav after reload

  Scenario: Upload and remove avatar
    Given I am logged in and on my profile page
    When I upload a valid PNG avatar
    Then my avatar image is displayed
    When I remove my avatar
    Then the placeholder image is displayed

  Scenario: Account deletion
    Given I am logged in
    When I delete my account
    Then I am redirected to the home page and am not authenticated
    And signing in with the deleted credentials fails
```

## Infrastructure
- MailHog API used to retrieve verification emails and extract the verification link.
- Keycloak direct grant used to seed test users for non-registration scenarios.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 All 6 Gherkin scenarios above have passing step definitions.
- [x] #2 The registration scenario uses MailHog API to click the verification link without manual intervention.
- [x] #3 The '401 on /api/persons' scenario runs against the live stack and confirms the endpoint is now auth-protected.
- [x] #4 The account deletion scenario verifies the user cannot log back in after deletion.
- [x] #5 cd e2e && npm test passes with all new scenarios green.
- [x] #6 Existing e2e scenarios that hit protected endpoints are updated to include a login step.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Cucumber + Playwright suite covering the full registration → login → profile lifecycle is in place under `e2e/features/auth/user-auth.feature` with steps in `e2e/src/steps/user-auth.steps.ts` and helpers in `e2e/src/support/{mailhog-client,keycloak-admin,jwt-helper}.ts`.

15 of 17 scenarios pass against the live docker stack. Two scenarios are tagged `@wip` and excluded from the default cucumber run because they expose product bugs that are out of scope for this task and are tracked separately:

- **Successful registration and login** — OIDC callback redirect chain leaves the browser at `chrome-error://chromewebdata/`. Tracked in TASK-10.
- **Upload and remove avatar** — SPA upload fails at the multipart layer (Spring rejects request before controller). Backend endpoint itself is healthy (verified via `curl -F`). Likely cause: `frontend/src/features/profile/profileApi.ts` forces `Content-Type: 'multipart/form-data'` without a boundary, defeating axios's FormData auto-detection. Tracked in TASK-9.

Once TASK-9 and TASK-10 are resolved, the `@wip` tags should be removed from the corresponding scenarios in `e2e/features/auth/user-auth.feature`.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Closure (2026-05-01)

With TASK-9 (avatar upload) and TASK-10 (OIDC redirect chain) resolved, both `@wip` tags were removed and all 6 Gherkin scenarios now pass against the live docker stack. Full e2e suite: **17/17 scenarios, 86/86 steps green**.
<!-- SECTION:FINAL_SUMMARY:END -->
