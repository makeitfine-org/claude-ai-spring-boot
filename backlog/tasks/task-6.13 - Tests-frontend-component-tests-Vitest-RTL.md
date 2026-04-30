---
id: TASK-6.13
title: 'Tests: frontend component tests (Vitest + RTL)'
status: To Do
assignee: []
created_date: '2026-04-30 16:41'
labels:
  - testing
  - frontend
  - vitest
  - rtl
dependencies:
  - TASK-6.8
  - TASK-6.9
parent_task_id: TASK-6
priority: medium
ordinal: 13000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Write Vitest + React Testing Library component tests for the new frontend components introduced in TASK-6.

## Components to Test

### `RegistrationForm`
- Renders all four fields (username, email, password, displayName).
- Shows field-level validation errors on submit with invalid data.
- Calls the registration API with correct payload on valid submit.
- Renders API 409 error as an inline message (no redirect).

### `AvatarUploader`
- File input accepts only `.png`, `.jpg`, `.jpeg`.
- Selecting a file > 1 MB shows an error message before making any API call.
- Selecting a valid file triggers `PUT /api/users/me/avatar` with the file as form data.
- After successful upload, the avatar image src is updated (cache-busted).
- "Remove avatar" button calls `DELETE /api/users/me/avatar` and hides the button.

### `DisplayNameEditor`
- Clicking "Edit" shows the text input pre-populated with the current display name.
- Clicking "Cancel" restores the original value without an API call.
- Saving with a valid value calls `PATCH /api/users/me` and updates the displayed name.
- Saving with an empty or too-long value shows an inline error.

### `AuthContext`
- `isAuthenticated=true` when `/api/users/me` returns 200.
- `isAuthenticated=false` when `/api/users/me` returns 401.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 RegistrationForm tests cover: render, client-side validation errors, valid submit, and API 409 error display.
- [ ] #2 AvatarUploader tests cover: file-type filter, >1 MB rejection, successful upload with src refresh, and remove.
- [ ] #3 DisplayNameEditor tests cover: edit/cancel, valid save with API call, and invalid-length error.
- [ ] #4 AuthContext tests cover: authenticated and unauthenticated states based on /api/users/me response.
- [ ] #5 All mocked API calls use MSW (Mock Service Worker) or vi.fn() — no real network calls.
- [ ] #6 npm test (Vitest) passes with all new component tests green.
<!-- AC:END -->
