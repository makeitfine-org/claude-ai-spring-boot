---
id: TASK-6.6
title: 'Backend: avatar upload, serve & delete (PUT/GET/DELETE /api/users/me/avatar)'
status: In Progress
assignee: []
created_date: '2026-04-30 16:40'
updated_date: '2026-05-01 07:27'
labels:
  - backend
  - avatar
  - upload
dependencies:
  - TASK-6.5
parent_task_id: TASK-6
priority: medium
ordinal: 6000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Implement avatar upload, retrieval, and deletion. Images are stored as BYTEA in the `users` table.

## Endpoints

| Method | Path | Description |
|---|---|---|
| PUT | /api/users/me/avatar | Upload or replace avatar (multipart/form-data, field `file`) |
| GET | /api/users/me/avatar | Stream avatar bytes with correct Content-Type |
| DELETE | /api/users/me/avatar | Remove avatar; set avatar_bytes and avatar_content_type to NULL |

## Upload Validation

1. Accept only `image/jpeg` and `image/png` Content-Type.
2. Verify magic bytes: JPEG starts with `FF D8 FF`; PNG starts with `89 50 4E 47`.
3. Reject if file size > 1 MB (before reading full body).
4. Decode image dimensions; if either dimension > 512px, downscale proportionally using Java ImageIO (maintain aspect ratio, max 512×512).
5. Re-encode to original format after resize; store result in `avatar_bytes`, set `avatar_content_type`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 PUT /api/users/me/avatar with a valid PNG under 1 MB stores the image and returns 200.
- [ ] #2 PUT /api/users/me/avatar with a valid JPEG under 1 MB stores the image and returns 200.
- [ ] #3 A file with wrong Content-Type (e.g. image/gif) is rejected with 415.
- [ ] #4 A file whose magic bytes don't match the declared Content-Type is rejected with 400.
- [ ] #5 A file larger than 1 MB is rejected with 413.
- [ ] #6 An image with dimensions > 512×512 is downscaled to fit within 512×512 before storage.
- [ ] #7 GET /api/users/me/avatar streams the bytes with the correct Content-Type header.
- [ ] #8 GET /api/users/me/avatar returns 404 when no avatar is set.
- [ ] #9 DELETE /api/users/me/avatar sets avatar_bytes to NULL and returns 204.
- [ ] #10 All three endpoints return 401 without a valid session.
<!-- AC:END -->
