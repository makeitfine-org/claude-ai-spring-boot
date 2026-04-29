---
id: TASK-3
title: >-
  Document Makefile usage in CLAUDE.md and README.md for root project and
  submodules
status: Done
assignee:
  - claude
created_date: '2026-04-29 17:45'
updated_date: '2026-04-29 18:07'
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
- [x] #1 Root CLAUDE.md references the Makefile and lists key targets
- [x] #2 Root README.md quick-start section uses `make` commands where appropriate
- [x] #3 backend/CLAUDE.md, frontend/CLAUDE.md, and e2e/CLAUDE.md each mention relevant Makefile targets for their module
- [x] #4 All documented targets actually exist in the Makefile
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
## Implementation Plan

1. Root `CLAUDE.md` — Add a `## Makefile` section listing key targets grouped by category (build, test, docker, GitHub Actions, notifications).
2. Root `README.md` — Update Quick Start section to show `make dockerAll` as the recommended first-run command; add `make help` tip.
3. `backend/CLAUDE.md` — Add a note pointing to `make buildBackend` and `make clean` / `make cleanShallow` under a Makefile reference bullet.
4. `frontend/CLAUDE.md` — Add a note pointing to `make buildFrontend` and `make updateFrontend`.
5. `e2e/CLAUDE.md` — Update the Running section to mention `make acceptanceTest` as the one-liner alternative.

All targets referenced exist in the root Makefile (verified).
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Added Makefile documentation to all five files. Root CLAUDE.md gained a full Makefile section with a target table. Root README.md Quick Start now leads with `make dockerAll`. backend/CLAUDE.md, frontend/CLAUDE.md, and e2e/CLAUDE.md each got a focused table of their relevant targets. All documented targets were verified to exist in the root Makefile.
<!-- SECTION:FINAL_SUMMARY:END -->
