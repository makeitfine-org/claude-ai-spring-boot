---
id: TASK-2.2
title: Configure skaffold dev file-watch for backend Java sources
status: Done
assignee: []
created_date: '2026-04-29 15:42'
updated_date: '2026-04-29 16:24'
labels:
  - backend
  - skaffold
  - dx
dependencies:
  - TASK-2.1
parent_task_id: TASK-2
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Once the backend Docker build is self-contained (TASK-2.1), Skaffold's dev loop must be wired to watch `backend/src/**` and `backend/pom.xml` so it re-triggers on Java source changes.

- If Jib was chosen in TASK-2.1: Skaffold's Jib integration handles file-watching automatically — this task mainly involves verification and documentation.
- If the multi-stage Dockerfile path was chosen: add an explicit `artifacts[].docker.buildArgs` + watch dependency in `backend/skaffold.yaml` so that `.java` file changes trigger a rebuild rather than just watching the Dockerfile context.

Document the expected round-trip iteration time (rough order-of-magnitude) in `backend/README.md`.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 skaffold dev from backend/ rebuilds and redeploys when any .java file under src/main/java changes
- [x] #2 skaffold dev rebuilds when pom.xml changes
- [x] #3 The pod reflects the code change without any manual mvn step
- [x] #4 backend/README.md documents the expected dev-loop iteration time
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
No skaffold.yaml changes needed. Skaffold's docker artifact type watches the entire build context filtered by `.dockerignore`. With `target/` excluded via `.dockerignore`, any change to `src/**` or `pom.xml` triggers a rebuild automatically — no explicit watch rules required. Documented expected iteration time (~2–5 min) in `backend/README.md` under the Kubernetes Deployment section.
<!-- SECTION:FINAL_SUMMARY:END -->
