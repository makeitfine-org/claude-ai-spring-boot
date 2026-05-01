---
id: TASK-6.7
title: 'Backend: global authorization, rate limiting & audit log'
status: In Progress
assignee: []
created_date: '2026-04-30 16:40'
updated_date: '2026-05-01 07:35'
labels:
  - backend
  - security
  - audit
  - rate-limit
dependencies:
  - TASK-6.3
  - TASK-6.5
parent_task_id: TASK-6
priority: high
ordinal: 7000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Lock down all API endpoints, add IP-based rate limiting on registration, and implement an audit log for profile mutations and account deletion.

## Authorization

- All `/api/**` require authentication except `POST /api/register` and `GET /actuator/health`.
- Existing `/api/persons/**` endpoints become authenticated (E2E scenarios will be updated separately).

## Rate Limiting

- Use Spring's `HandlerInterceptor` or Bucket4j to rate-limit `POST /api/register` to **5 requests / 15 min per IP**.
- Return 429 with `Retry-After` header when exceeded.

## Audit Log

Flyway migration adds:

```sql
CREATE TABLE audit_events (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  sub         VARCHAR(255) NOT NULL,   -- anonymised to 'DELETED:<hash>' on account deletion
  event_type  VARCHAR(50)  NOT NULL,   -- DISPLAY_NAME_CHANGED, AVATAR_UPLOADED, AVATAR_REMOVED, ACCOUNT_DELETED
  before_val  TEXT,
  after_val   TEXT,
  occurred_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
```

- Log on: `PATCH /api/users/me` (display name change), `PUT /api/users/me/avatar`, `DELETE /api/users/me/avatar`, `DELETE /api/users/me`.
- On account deletion: anonymise existing audit rows for that sub (`UPDATE audit_events SET sub = 'DELETED:' || md5(sub) WHERE sub = ?`).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 GET /api/persons returns 401 without a session (previously public).
- [ ] #2 POST /api/register and GET /actuator/health remain accessible without authentication.
- [ ] #3 Posting to /api/register 6 times within 15 minutes from the same IP returns 429 with a Retry-After header on the 6th request.
- [ ] #4 Changing display_name writes an AUDIT_EVENT row with event_type=DISPLAY_NAME_CHANGED, before_val, and after_val.
- [ ] #5 Uploading or removing an avatar writes the corresponding AVATAR_UPLOADED or AVATAR_REMOVED audit row.
- [ ] #6 Deleting the account writes an ACCOUNT_DELETED audit row, then anonymises all prior audit rows for that sub.
- [ ] #7 audit_events rows for a deleted account have sub starting with 'DELETED:'.
<!-- AC:END -->
