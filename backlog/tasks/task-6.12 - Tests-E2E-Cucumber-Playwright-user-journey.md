---
id: TASK-6.12
title: 'Tests: E2E Cucumber + Playwright user journey'
status: To Do
assignee: []
created_date: '2026-04-30 16:41'
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
- [ ] #1 All 6 Gherkin scenarios above have passing step definitions.
- [ ] #2 The registration scenario uses MailHog API to click the verification link without manual intervention.
- [ ] #3 The '401 on /api/persons' scenario runs against the live stack and confirms the endpoint is now auth-protected.
- [ ] #4 The account deletion scenario verifies the user cannot log back in after deletion.
- [ ] #5 cd e2e && npm test passes with all new scenarios green.
- [ ] #6 Existing e2e scenarios that hit protected endpoints are updated to include a login step.
<!-- AC:END -->
