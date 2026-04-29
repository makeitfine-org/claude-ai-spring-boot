---
id: TASK-1
title: Rename backend module from claude-ai-spring-boot to backend
status: Done
assignee: []
created_date: '2026-04-29 13:24'
updated_date: '2026-04-29 14:07'
labels:
  - refactor
  - backend
dependencies: []
priority: medium
ordinal: 1000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
The Maven artifact and module directory are currently named `claude-ai-spring-boot`. Rename it to `backend` to align with the monorepo layout convention (backend/, frontend/, e2e/).
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 pom.xml artifactId changed from claude-ai-spring-boot to backend
- [x] #2 Dockerfile COPY line updated to reference backend-{version}.jar
- [x] #3 docker-compose.yml updated if it references the old jar/image name
- [x] #4 All internal references (README, CI workflow, Makefile, CLAUDE.md) updated to use the new module name
- [x] #5 mvn clean install succeeds
- [x] #6 docker compose up starts cleanly with no errors
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Renamed Maven artifactId and name from `claude-ai-spring-boot` to `backend` in pom.xml. Updated all downstream references: Dockerfile JAR path, docker-compose container_name, spring.application.name, CI workflow JAR glob and Docker image tags, skaffold.yaml metadata/image/portForward, all k8s manifests (deployment, service, configmap, secret) labels and resource names, frontend k8s configmap proxy_pass hostname, backend/CLAUDE.md coding rule, version-bump-procedure.md, and README kubectl commands. Build passes: 68 tests, 0 failures, JAR produced as backend-1.0.1.jar.
<!-- SECTION:FINAL_SUMMARY:END -->
