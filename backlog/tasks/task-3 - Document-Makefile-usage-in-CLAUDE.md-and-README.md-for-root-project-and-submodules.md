---
id: TASK-3
title: >-
  Document Makefile usage in CLAUDE.md and README.md for root project and
  submodules
status: To Do
assignee: []
created_date: '2026-04-29 17:45'
labels:
  - docs
  - dx
dependencies: []
priority: low
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The project has a Makefile (or should have one) that provides developer convenience targets. Neither the root CLAUDE.md / README.md nor the submodule-level files (backend/CLAUDE.md, frontend/CLAUDE.md, e2e/CLAUDE.md) currently reference the Makefile or its available targets.

Update all relevant documentation files so developers know:
- That a Makefile exists and where it lives
- Which targets are available (e.g. build, test, docker-up, etc.)
- When to prefer `make <target>` over running the raw commands directly
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Root CLAUDE.md references the Makefile and lists key targets
- [ ] #2 Root README.md quick-start section uses `make` commands where appropriate
- [ ] #3 backend/CLAUDE.md, frontend/CLAUDE.md, and e2e/CLAUDE.md each mention relevant Makefile targets for their module
- [ ] #4 All documented targets actually exist in the Makefile
<!-- AC:END -->
