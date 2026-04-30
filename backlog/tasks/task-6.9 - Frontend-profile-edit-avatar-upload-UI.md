---
id: TASK-6.9
title: 'Frontend: profile edit & avatar upload UI'
status: To Do
assignee: []
created_date: '2026-04-30 16:40'
labels:
  - frontend
  - profile
  - avatar
  - react
dependencies:
  - TASK-6.5
  - TASK-6.6
  - TASK-6.8
parent_task_id: TASK-6
priority: medium
ordinal: 9000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Build the authenticated profile page where the user can view and edit their display name and manage their avatar.

## Profile Page (`/profile`)

### Display Name Section
- Shows current display name (or username if unset).
- Inline edit: click → text input → Save / Cancel.
- Client-side validation: 1–50 chars, no leading/trailing whitespace shown after trim.
- On success: refresh display name in `AuthContext` and nav.
- On 400: show error message below the field.

### Avatar Section
- Shows current avatar using `GET /api/users/me/avatar`, or a default placeholder if `hasAvatar=false`.
- "Upload avatar" button → file picker filtered to `.png, .jpg, .jpeg`.
- Client-side pre-check: reject files > 1 MB and wrong extension before upload.
- On upload success: refresh avatar image (cache-bust with timestamp query param).
- "Remove avatar" button (visible only when `hasAvatar=true`) → calls `DELETE /api/users/me/avatar`.
- Show upload errors from API (415, 400, 413) as user-friendly messages.

### Account Deletion
- "Delete account" link at the bottom.
- Confirmation modal: "This will permanently delete your account. This cannot be undone." with Cancel / Delete buttons.
- On confirm: calls `DELETE /api/users/me` → redirects to `/` with a session-cleared state.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Profile page is accessible only to authenticated users.
- [ ] #2 Display name can be edited inline; changes persist after page reload.
- [ ] #3 Attempting to save a display name with control characters shows an error.
- [ ] #4 File picker accepts only .png/.jpg/.jpeg files.
- [ ] #5 Uploading a file > 1 MB client-side shows a friendly error before the request is sent.
- [ ] #6 A successful avatar upload displays the new image without a full page reload.
- [ ] #7 'Remove avatar' removes the image and shows the placeholder.
- [ ] #8 Account deletion modal appears on click and requires explicit confirmation.
- [ ] #9 After confirming account deletion, the user is logged out and redirected to /.
<!-- AC:END -->
