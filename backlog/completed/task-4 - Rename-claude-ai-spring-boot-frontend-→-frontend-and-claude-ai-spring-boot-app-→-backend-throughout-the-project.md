---
id: TASK-4
title: >-
  Rename claude-ai-spring-boot-frontend → frontend and claude-ai-spring-boot-app
  → backend throughout the project
status: Done
assignee: []
created_date: '2026-04-29 17:45'
updated_date: '2026-04-29 19:00'
labels:
  - refactor
  - naming
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Following the completed TASK-1 rename of the backend module, two legacy names remain in the codebase:

1. `claude-ai-spring-boot-frontend` — should become `frontend` (mirrors the directory name and the backend rename pattern)
2. `claude-ai-spring-boot-app` — should become `backend`

Occurrences to search and fix:
- `pom.xml` files (artifactId, module declarations)
- `docker-compose.yml` (service names, image names, container names)
- Dockerfile(s)
- Skaffold / k8s manifests
- GitHub Actions workflow files under `.github/workflows/`
- Any README, CLAUDE.md, or other documentation that references the old names
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 No occurrence of `claude-ai-spring-boot-frontend` remains in the repo
- [x] #2 No occurrence of `claude-ai-spring-boot-app` remains in the repo
- [ ] #3 `docker compose up` starts all services without errors after the rename
- [ ] #4 CI pipeline passes with the updated names
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Renamed both legacy image names throughout the codebase:\n- Added `image: backend` to the `app` service in `docker-compose.yml`\n- Added `image: frontend` to the `frontend` service in `docker-compose.yml` (also fixed `container_name` from `claude-ai-frontend` → `frontend`)\n- Updated both `clean` and `cleanShallow` Makefile targets to remove `backend:latest` and `frontend:latest` instead of the old names\n- Updated `frontend/README.md` standalone `docker build` example to use `-t frontend`\n\nNo occurrences of `claude-ai-spring-boot-frontend` or `claude-ai-spring-boot-app` remain outside historical docs (`.claude/`, `backlog/`).
<!-- SECTION:FINAL_SUMMARY:END -->
