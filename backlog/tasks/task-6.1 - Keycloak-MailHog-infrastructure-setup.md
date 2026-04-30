---
id: TASK-6.1
title: Keycloak & MailHog infrastructure setup
status: Done
assignee: []
created_date: '2026-04-30 16:39'
labels:
  - infra
  - keycloak
  - docker
dependencies: []
parent_task_id: TASK-6
priority: high
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Add Keycloak and MailHog to docker-compose and provide a pre-imported realm so the rest of the feature can be developed against a working IdP.

## Scope

- Add `keycloak` service to `docker-compose.yml` (image `quay.io/keycloak/keycloak`, health-check included).
- Add `mailhog` service for dev SMTP (port 1025 SMTP, 8025 web UI).
- Create a realm export file (`keycloak/realm-export.json`) for realm `claude-ai` containing:
  - Public SPA client (`claude-ai-spa`) with PKCE, redirect URIs for `http://localhost:3000` and `http://localhost:8080`.
  - Confidential backend client (`claude-ai-backend`) with client-credentials grant.
  - Password policy: length ≥ 8, upper + lower + digit + special, not-username.
  - Brute-force detection enabled (5 failures → 15 min lockout).
  - SMTP settings pointing to MailHog container.
  - Email verification required before login.
- Mount realm export via `--import-realm` so Keycloak auto-imports on first start.
- Document new ports in root `README.md` (Keycloak: 8180, MailHog UI: 8025).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 docker compose up starts Keycloak, MailHog, Postgres, backend, and frontend without errors.
- [x] #2 Keycloak admin console (http://localhost:8180) shows realm 'claude-ai' with both clients present.
- [x] #3 MailHog UI (http://localhost:8025) is reachable.
- [x] #4 Realm password policy rejects passwords shorter than 8 chars or missing required character classes.
- [x] #5 Brute-force protection is enabled on the realm.
- [x] #6 README.md lists the new services and their ports.
<!-- AC:END -->
