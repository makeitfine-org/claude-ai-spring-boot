---
id: TASK-9
title: 'Fix: avatar upload from SPA fails (multipart Content-Type)'
status: To Do
assignee: []
created_date: '2026-05-01 15:06'
labels:
  - bug
  - frontend
  - avatar
dependencies:
  - TASK-6.12
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

End-to-end scenario "Upload and remove avatar" (TASK-6.12) fails: the SPA shows "Failed to upload avatar. Please try again." after a PNG is selected.

## Evidence

- Direct `curl -F file=@valid.png` against `PUT /api/users/me/avatar` with the same JWT returns 200 — the backend endpoint is healthy.
- From the SPA, backend logs show `Secured PUT /api/users/me/avatar` then no controller invocation — Spring rejects the request before the handler.

## Likely root cause

`frontend/src/features/profile/profileApi.ts` calls `api.put` with `headers: { 'Content-Type': 'multipart/form-data' }`. The axios instance also has a default `Content-Type: 'application/json'`. With FormData as the body, the explicit `multipart/form-data` (no boundary) overrides the browser's auto-set `multipart/form-data; boundary=...`, so Spring cannot parse the multipart body.

## Suggested fix

Either:
- Omit the explicit `Content-Type` and let the axios FormData detection / browser set the proper boundary, or
- Use a request-level header override (`'Content-Type': undefined`) that defeats the instance default.

Verify with the e2e scenario currently tagged `@wip` in `e2e/features/auth/user-auth.feature` (Upload and remove avatar) — once green, remove the `@wip` tag.

## Acceptance Criteria
<!-- AC:BEGIN -->
- The two `@wip`-tagged steps pass against the live stack.
- `make clean build` passes with the `@wip` tag removed from the avatar scenario.
<!-- SECTION:DESCRIPTION:END -->

- [ ] #1 SPA avatar upload via PUT /api/users/me/avatar succeeds end-to-end (controller invoked, 200 response, image visible).
- [ ] #2 @wip tag removed from the 'Upload and remove avatar' scenario in e2e/features/auth/user-auth.feature.
- [ ] #3 `make clean build` passes.
<!-- AC:END -->
