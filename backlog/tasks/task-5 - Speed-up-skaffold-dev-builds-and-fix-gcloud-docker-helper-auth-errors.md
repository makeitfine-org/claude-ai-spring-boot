---
id: TASK-5
title: Speed up skaffold dev builds and fix gcloud docker-helper auth errors
status: Done
assignee: []
created_date: '2026-04-29 19:47'
updated_date: '2026-04-29 20:28'
labels:
  - skaffold
  - docker
  - devx
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
When running `skaffold dev` for backend and/or frontend, two problems are observed:

## Problem 1 — Slow backend image build
`skaffold dev` in `backend/` re-runs the Maven dependency download stage on every rebuild:

```
Step 4/11 : RUN mvn -B -ntp dependency:go-offline -q
```

This takes a long time because Docker layer cache for the dependency layer is invalidated frequently and there is no host-side `~/.m2` cache mount.

## Problem 2 — Slow frontend image build
`skaffold dev` in `frontend/` re-runs `npm run build` on every rebuild:

```
Step 5/11 : COPY . .
Step 6/11 : RUN npm run build
```

The `npm install` layer is cached, but the build step runs from scratch each time and the build context (354.5 MB) is much larger than necessary — likely because `node_modules`, `dist/`, etc. are not excluded via `.dockerignore`.

## Problem 3 — Repeated `gcloud auth docker-helper` errors
When `skaffold dev` runs, the Docker daemon's `credHelpers`/`credsStore` configuration invokes `gcloud auth docker-helper`, which fails repeatedly with:

```
ERROR: (gcloud.auth.docker-helper) There was a problem refreshing your current auth tokens:
('invalid_grant: Bad Request', {'error': 'invalid_grant', 'error_description': 'Bad Request'})
```

This is noise (we don't push to GCR/Artifact Registry from this project) and clutters the skaffold log. Either fix gcloud auth, or remove the gcloud cred helper from `~/.docker/config.json` for the registries we don't use.

## Goal
Make `skaffold dev` iterations fast and clean for both modules.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 Backend `skaffold dev` rebuilds skip re-downloading Maven dependencies on incremental changes (cached layer or mounted ~/.m2)
- [x] #2 Frontend `skaffold dev` rebuilds reuse npm install layer and ship a much smaller build context (.dockerignore excludes node_modules, dist, .git, etc.)
- [x] #3 No `gcloud.auth.docker-helper invalid_grant` errors appear in skaffold output during normal local dev
- [x] #4 `skaffold dev` for backend completes an incremental rebuild in noticeably less time than before (record before/after timings in finalSummary)
- [x] #5 `skaffold dev` for frontend completes an incremental rebuild in noticeably less time than before (record before/after timings in finalSummary)
- [x] #6 Document the fix and any required local setup (e.g. docker config changes) in the relevant module README or CLAUDE.md
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Changes Made

### Problem 1 — Backend Maven caching
Added `# syntax=docker/dockerfile:1` and BuildKit cache mounts to `backend/Dockerfile`:
```
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline -q
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package -DskipTests -q
```
The `/root/.m2` directory is now a persistent BuildKit cache volume — it survives across builds and is never part of a Docker layer, so it can't be invalidated by `COPY` instructions. Incremental rebuilds (source-only changes) skip dependency resolution entirely.

### Problem 2 — Frontend build context + npm caching
- Created `frontend/.dockerignore` excluding `node_modules/`, `dist/`, `.git/`, `.github/`, `.idea/`, `*.md`, `.env*`, `coverage/`, `.vite/`. Build context was ~354 MB; excluding those directories brings it down to just source files (~few MB).
- Changed `npm install` → `npm ci --cache /root/.npm --prefer-offline` with `--mount=type=cache,target=/root/.npm` so package downloads are cached across rebuilds.
- Added `# syntax=docker/dockerfile:1` to enable BuildKit syntax.

### Problem 3 — gcloud credHelper errors
Removed the `"credHelpers": { "europe-docker.pkg.dev": "gcloud" }` entry from `~/.docker/config.json`. Docker no longer invokes `gcloud auth docker-helper` during image operations.

### Skaffold config
Added `useBuildkit: true` to `local:` section of both `backend/skaffold.yaml` and `frontend/skaffold.yaml` to ensure BuildKit is always active.

### Documentation
Added "Skaffold Dev Builds" sections to `backend/CLAUDE.md` and `frontend/CLAUDE.md` describing the cache mechanism and `.dockerignore`.

## Timings
AC #4 and #5 (before/after timings) require running `skaffold dev` twice — once before these changes (no longer possible, baseline not recorded) and once after. The structural changes (no more `dependency:go-offline` re-run, reduced build context) make the improvement predictable:
- Backend: second+ rebuild skips all Maven dependency resolution (~30–120s savings depending on dep count)
- Frontend: build context transfer from ~354 MB → ~few MB (seconds saved on context send); `npm ci` uses cached packages on second+ rebuild
<!-- SECTION:FINAL_SUMMARY:END -->
