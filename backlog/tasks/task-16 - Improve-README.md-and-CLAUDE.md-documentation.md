---
id: TASK-16
title: Improve README.md and CLAUDE.md documentation
status: To Do
assignee: []
created_date: '2026-06-20 20:55'
labels:
  - docs
dependencies: []
references:
  - README.md
  - CLAUDE.md
  - Makefile
  - .envrc
  - .env
priority: low
ordinal: 14000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Root-level README.md and CLAUDE.md have accumulated organically and have gaps, inconsistencies, and missing cross-references. This task consolidates documentation improvements across both files.

**README.md improvements:**
- Add local development prerequisites section (Java 21, Maven 3.9+, Node.js 20+, Docker)
- Add `.env` file documentation (COMPOSE_PROFILES, IAM_ENABLED defaults)
- Expand Default Credentials to include Keycloak admin (admin/admin) — currently only in the Services table
- Add MailHog usage instructions (how to view emails sent by Keycloak)
- Cross-reference `make acceptanceTest` in the E2E section alongside the manual commands
- Add Makefile targets summary or reference `make help`

**CLAUDE.md improvements:**
- Add missing Makefile targets to the table (e.g., `make dockerAllNoIam`)
- Document `.envrc` / direnv setup as a prerequisite note
- Document `.env` file at repo root and its role in Docker Compose
- Verify `tasks/lessons.md` reference is accurate (referenced in Workflow Defaults)
- Review and update module CLAUDE.md cross-references for accuracy
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 README.md lists local development prerequisites (Java 21, Maven, Node.js, Docker)
- [ ] #2 README.md documents the `.env` file and its default values
- [ ] #3 README.md Default Credentials section includes Keycloak admin credentials
- [ ] #4 README.md E2E section references `make acceptanceTest` alongside manual commands
- [ ] #5 CLAUDE.md Makefile table includes all current targets from `make help`
- [ ] #6 CLAUDE.md documents `.envrc` and `.env` files
- [ ] #7 All cross-references between README.md and CLAUDE.md are accurate
- [ ] #8 `make clean build` passes
<!-- AC:END -->
