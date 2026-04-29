---
id: TASK-2.1
title: Make backend Docker build self-contained (no host-side mvn prerequisite)
status: To Do
assignee: []
created_date: '2026-04-29 15:42'
labels:
  - backend
  - docker
  - skaffold
dependencies: []
parent_task_id: TASK-2
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Currently `backend/Dockerfile` only does `COPY target/backend-1.0.1.jar app.jar`, which requires the host to have already run `mvn package`. This breaks `skaffold dev` hot-reload (see parent TASK-2).

Replace it with a build that is fully self-contained. Two options — implementer should pick one:

- **Option A — Jib (recommended):** Add `com.google.cloud.tools:jib-maven-plugin` to `backend/pom.xml`. Switch `backend/skaffold.yaml` artifact from `docker:` to `jib:`. Delete `backend/Dockerfile`. Jib integrates natively with Skaffold and watches `src/**` automatically.
- **Option B — Multi-stage Dockerfile:** Stage 1 uses `maven:3.9-eclipse-temurin-21` and runs `mvn -B -ntp package -DskipTests`. Stage 2 copies the jar via wildcard (`COPY --from=build /app/target/backend-*.jar app.jar`), removing the hard-coded version string. Keeps `docker compose up` flow unchanged with no extra tooling.

Critical files: `backend/Dockerfile`, `backend/skaffold.yaml`, `backend/pom.xml`, `backend/k8s/deployment.yaml` (confirm image name still matches).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 A clean checkout with no target/ directory can run skaffold build for the backend artifact and produces a runnable image
- [ ] #2 The image no longer references a hard-coded version string (e.g. backend-1.0.1.jar)
- [ ] #3 docker compose up from the repo root continues to work
- [ ] #4 backend/k8s/deployment.yaml image name matches the Skaffold artifact name
<!-- AC:END -->
