---
id: TASK-9
title: 'Fix: avatar upload from SPA fails (multipart Content-Type)'
status: Done
assignee:
  - claude
created_date: '2026-05-01 15:06'
updated_date: '2026-05-01 15:47'
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

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Fix `frontend/src/features/profile/profileApi.ts`: drop the hard-coded `Content-Type: multipart/form-data` header on `uploadAvatar` so the browser sets the correct `multipart/form-data; boundary=...`. Override the axios instance default by passing `headers: { 'Content-Type': undefined }` (axios then lets the browser/XHR auto-detect from FormData).
2. Remove `@wip` tag from the "Upload and remove avatar" scenario in `e2e/features/auth/user-auth.feature`.
3. Run `make clean build` from repo root and verify both the avatar e2e steps pass and full suite is green.
4. Check off acceptance criteria, append final summary, mark task Done.
<!-- SECTION:PLAN:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Two bugs combined to make the avatar scenario fail; both fixed:

1. **Frontend axios config (real root cause for the SPA upload)** — `frontend/src/lib/api.ts` declared `headers: { 'Content-Type': 'application/json' }` on the shared axios instance. That instance default defeated axios's per-request FormData detection, so `uploadAvatar` sent the body without a `boundary=...` and the browser/Spring could not parse it. Removed the default `Content-Type` from the axios instance and dropped the explicit `multipart/form-data` header from `profileApi.uploadAvatar`. Axios now auto-sets `application/json` for plain object bodies and lets the browser set `multipart/form-data; boundary=...` for FormData.

2. **E2E test race exposed once the upload reached the wire** — `e2e/src/steps/user-auth.steps.ts` deleted the temp PNG in a `finally` block immediately after `fileChooser.setFiles(tmpFile)`. Chromium reads the file lazily when serialising the FormData; in the full suite the unlink raced ahead of the XHR and the request body went out empty (in single-scenario runs the timing happened to win). Reordered the step to `waitForResponse` on `PUT /api/users/me/avatar` (and `networkidle`) before unlinking, with a defensive `existsSync` check.

Removed the `@wip` tag on the "Upload and remove avatar" scenario. `make clean build` is green: backend tests pass and 16/16 e2e scenarios pass.
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Avatar upload from the SPA was failing for two reasons, both now fixed:

- **Frontend axios config** (`src/lib/api.ts`, `src/features/profile/profileApi.ts`): the shared instance defaulted `Content-Type: application/json`, which silently overrode axios's FormData auto-detection on `uploadAvatar`. The browser never saw `multipart/form-data; boundary=...`, so the request was malformed. Removed the instance default and the explicit per-call header — axios now picks the right Content-Type per body type.
- **E2E test race** (`e2e/src/steps/user-auth.steps.ts`): the upload step deleted the temp PNG in `finally` before Chromium had read it for the XHR. Now the step waits for `PUT /api/users/me/avatar` to complete (and `networkidle`) before unlinking.

`@wip` tag removed from the "Upload and remove avatar" scenario; `make clean build` passes (backend tests + 16/16 e2e scenarios).
<!-- SECTION:FINAL_SUMMARY:END -->

- [ ] #1 SPA avatar upload via PUT /api/users/me/avatar succeeds end-to-end (controller invoked, 200 response, image visible).
- [ ] #2 @wip tag removed from the 'Upload and remove avatar' scenario in e2e/features/auth/user-auth.feature.
- [ ] #3 `make clean build` passes.
<!-- AC:END -->

- [ ] #1 SPA avatar upload via PUT /api/users/me/avatar succeeds end-to-end (controller invoked, 200 response, image visible).
- [ ] #2 @wip tag removed from the 'Upload and remove avatar' scenario in e2e/features/auth/user-auth.feature.
- [ ] #3 `make clean build` passes.
<!-- AC:END -->

- [ ] #1 SPA avatar upload via PUT /api/users/me/avatar succeeds end-to-end (controller invoked, 200 response, image visible).
- [ ] #2 @wip tag removed from the 'Upload and remove avatar' scenario in e2e/features/auth/user-auth.feature.
- [ ] #3 `make clean build` passes.
<!-- AC:END -->
