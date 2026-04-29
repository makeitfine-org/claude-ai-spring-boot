# Plan: Backlog tasks for `skaffold dev` backend hot-reload issue

## Context

Running `skaffold dev` against both modules:

- **Frontend** (`frontend/skaffold.yaml` + `frontend/Dockerfile`): code changes are reflected
  in the browser. The Dockerfile is multi-stage and builds inside Docker (`npm run build`),
  so when Skaffold rebuilds the image it picks up the new sources.

- **Backend** (`backend/skaffold.yaml` + `backend/Dockerfile`): code changes are **not**
  reflected. Even after `mvn clean package`, Skaffold rebuilds the image and pods restart,
  but the running app still serves the old behaviour.

### Root cause

`backend/Dockerfile` only does `COPY target/backend-1.0.1.jar app.jar` — it does **not**
build the jar. Skaffold's `docker` builder only runs `docker build`; it never invokes
Maven. There is also no `sync`/watch hook coupling Java sources or `target/*.jar` to the
image rebuild. As a result:

1. When sources change, Skaffold rebuilds the image but copies the **stale** jar that
   was last produced (or fails the COPY if `target/` is empty).
2. When the user manually runs `mvn package`, Skaffold's file watcher (configured
   implicitly off the Dockerfile context) may not consider the new jar a meaningful
   change, or may rebuild before the jar is fully written.
3. The hard-coded version `backend-1.0.1.jar` will silently break the moment
   `pom.xml` version is bumped per `.claude/rules/version-bump-procedure.md`.

The repo's `CLAUDE.md` agent guide says k8s/Skaffold work goes to `kubernetes-specialist`,
and Dockerfile work to `docker-expert` — this task spans both.

## Recommended approach: split into two backlog tasks

Both tasks live in `backend/`. They are tightly coupled (same component) so they should be
**subtasks of one parent task**, per the Backlog task-creation guide ("Multiple tasks all
modify the same component" → use subtasks).

### Parent task

**Title:** Fix `skaffold dev` hot-reload for backend module

**Description (WHY):** `skaffold dev` does not surface backend source changes in the
running pod, even after `mvn clean package`. Frontend works because its Dockerfile builds
inside Docker; backend's Dockerfile only copies a pre-built, version-pinned jar, so
Skaffold's rebuild loop is decoupled from the Maven build. Restore parity with the
frontend so that editing a `.java` file under `backend/src/` results in a redeployed pod
that serves the new code, without manual `mvn` invocations.

**Acceptance criteria:**
- Editing any file under `backend/src/main/java` while `skaffold dev` is running
  produces a pod whose behaviour reflects the change (verifiable via a trivial
  `/actuator/info` or controller change).
- `mvn clean package` is no longer required as a manual prerequisite.
- `pom.xml` version bumps no longer require editing the Dockerfile (no hard-coded
  `backend-1.0.1.jar`).
- `skaffold build` and `docker compose up` (existing flow per root `README.md`) both
  still produce a runnable image.
- Root `README.md` and/or `backend/README.md` updated with the new dev-loop instructions.
- `.claude/rules/version-bump-procedure.md` updated or removed if its Dockerfile-edit
  step is no longer needed.

### Subtask 1 — Make the backend Docker build self-contained

**Title:** Backend Dockerfile: build the jar inside the image (or via Jib)

**Description:** Replace the current `COPY target/backend-*.jar` with a build that does
not depend on a host-side `mvn package`. Two viable options — pick one in the task:

- **Option A (recommended): Jib.** Add `com.google.cloud.tools:jib-maven-plugin` to
  `backend/pom.xml`, switch `backend/skaffold.yaml` artifact from `docker:` to `jib:`,
  delete `backend/Dockerfile`. Jib watches `src/**` natively under Skaffold and produces
  reproducible images without a Docker daemon dependency at build time.
- **Option B: multi-stage Dockerfile.** Stage 1 `maven:3.9-eclipse-temurin-21` runs
  `mvn -B -ntp package -DskipTests`; stage 2 copies the jar via a wildcard
  (`COPY --from=build /app/target/backend-*.jar app.jar`) so the version is no longer
  hard-coded. Slower than Jib but keeps `docker compose` flow unchanged.

**Acceptance criteria:**
- A clean checkout (no `target/` directory) can be built with `skaffold build` for the
  backend artifact and produces a runnable image.
- Image no longer references a hard-coded version string.
- `docker compose up` continues to work (root `docker-compose.yml`).

### Subtask 2 — Wire Skaffold dev-loop watch + sync for backend

**Title:** Configure `skaffold dev` watch/sync for backend Java sources

**Description:** With the build self-contained (subtask 1), update
`backend/skaffold.yaml` so `skaffold dev` re-triggers on changes under
`backend/src/**` and `backend/pom.xml`. If Jib was chosen, this is automatic; if the
Dockerfile path was chosen, add an explicit `sync`/`custom` build dependency block so
Skaffold watches Java sources rather than just the Dockerfile context.

**Acceptance criteria:**
- `skaffold dev` from `backend/` rebuilds and redeploys when a `.java` file changes.
- Iteration time is documented in `backend/README.md` (rough order-of-magnitude).
- The pod's `/actuator/info` (or equivalent) reflects a code change without any
  manual `mvn` step.

## Critical files

- `backend/skaffold.yaml` — build artifact + watch config
- `backend/Dockerfile` — replace or delete
- `backend/pom.xml` — Jib plugin if Option A
- `backend/k8s/deployment.yaml` — confirm image name still matches Skaffold artifact
- Root `README.md`, `backend/README.md` — dev-loop docs
- `.claude/rules/version-bump-procedure.md` — may become obsolete

## Verification (after the work itself is done — not part of this planning task)

1. `cd backend && skaffold dev` — leave running.
2. Edit a `@RestController` method to return a new string; save.
3. Watch Skaffold logs: build → deploy → pod ready.
4. `curl localhost:8080/<endpoint>` returns the new string within ~30–60 s, no manual
   `mvn` invocation.
5. Bump `pom.xml` version (e.g. `1.0.1` → `1.0.2`) — Skaffold loop still works
   without editing the Dockerfile.

## What this planning task will actually do

Create the parent task and its two subtasks in Backlog.md via the `mcp__backlog__*`
tools. No code edits.