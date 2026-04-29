---
id: TASK-2
title: Fix skaffold dev hot-reload for backend module
status: To Do
assignee: []
created_date: '2026-04-29 15:42'
labels:
  - backend
  - skaffold
  - dx
  - k8s
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
`skaffold dev` does not surface backend source changes in the running pod, even after `mvn clean package`. The frontend works because its Dockerfile is multi-stage and builds inside Docker (`npm run build`), so Skaffold rebuilds pick up new sources. The backend `Dockerfile` only does `COPY target/backend-1.0.1.jar app.jar` — it never invokes Maven. Skaffold's docker builder runs `docker build` only, so the stale jar is copied. Additionally, the hard-coded version string will silently break whenever `pom.xml` version is bumped.

Goal: restore parity with the frontend so editing a `.java` file under `backend/src/` results in a redeployed pod serving the new code, without manual `mvn` invocations.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Editing any file under backend/src/main/java while skaffold dev is running produces a pod whose behaviour reflects the change (verifiable via a controller or actuator change)
- [ ] #2 mvn clean package is no longer required as a manual prerequisite for skaffold dev
- [ ] #3 pom.xml version bumps no longer require editing the Dockerfile (no hard-coded backend-1.0.1.jar)
- [ ] #4 skaffold build and docker compose up both still produce a runnable image
- [ ] #5 Root README.md and/or backend/README.md updated with the new dev-loop instructions
- [ ] #6 .claude/rules/version-bump-procedure.md updated or removed if its Dockerfile-edit step is no longer needed
<!-- AC:END -->
