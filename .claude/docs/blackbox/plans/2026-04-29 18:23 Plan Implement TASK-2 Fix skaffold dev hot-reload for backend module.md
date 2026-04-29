# Plan: Implement TASK-2 — Fix skaffold dev hot-reload for backend module

## Context

`backend/Dockerfile` only does `COPY target/backend-1.0.1.jar app.jar` and never invokes
Maven. Skaffold's `docker` builder just runs `docker build`, so every rebuild copies the
stale (or absent) host jar. Frontend works because its Dockerfile runs `npm run build`
inside Docker.

**Chosen approach: Option B — multi-stage Dockerfile** (not Jib).
- No new Maven plugins; one build path for both `docker compose` and `skaffold dev/build`
- Skaffold's docker artifact type watches all non-ignored files in the build context —
  a `.dockerignore` excluding `target/` makes it watch exactly `src/**` + `pom.xml`
- Expected iteration time: ~2–5 min per change (full Maven build inside Docker)
- Jib would give ~30–60 s iterations but requires a new plugin, separate Dockerfile for
  docker-compose, and more moving parts; deferred.

---

## Files to change

### 1. `backend/Dockerfile` — replace entirely

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline -q
COPY src ./src
RUN mvn -B -ntp package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Key points:
- Dependency layer cached separately from source layer (faster rebuilds when only src changes)
- Wildcard `backend-*.jar` eliminates the hard-coded `1.0.1` version string
- `docker compose up` continues to work (same build context `./backend`)

### 2. `backend/.dockerignore` — create

```
target/
.git/
.github/
.idea/
*.iml
*.md
```

Excluding `target/` ensures:
- Skaffold does not watch `target/` (no false triggers from prior local builds)
- Build context sent to Docker daemon is small and version-clean

### 3. `backend/skaffold.yaml` — no code changes needed

The docker artifact type already watches the build context dir, filtered by
`.dockerignore`. Once `target/` is excluded, any `.java` or `pom.xml` change triggers a
rebuild automatically. No explicit watch rules required.

### 4. `.claude/rules/version-bump-procedure.md` — remove step 1, renumber

Step 1 "Update Dockerfile" is obsolete. New procedure:

```markdown
## Steps (execute in order; fix any errors before continuing)

1. **Build** — `mvn clean install`
2. **Remove old Docker image** — `docker rmi spring-cloud2-api-gateway:latest`
   (ignore "image not found" errors)
3. **Stop running containers** — `docker compose down`
4. **Start fresh** — `docker compose up`
```

### 5. Root `README.md` — remove pre-build step from "Running Acceptance Tests"

Remove:
```bash
# 1. Build the backend JAR (required before first docker compose up)
mvn -f backend/pom.xml package -DskipTests -q
```

Renumber remaining steps (was 2→4, now 1→3). Update explanatory note on step 1.

### 6. `backend/README.md` — update Kubernetes Deployment section

Under "Deploy the backend" (`skaffold dev`), add a note:
> File-watch: Skaffold rebuilds the image when any file under `src/` or `pom.xml`
> changes. Expected round-trip time: **2–5 minutes** (full Maven build inside Docker).

---

## Verification steps (after implementation)

1. `docker compose up` from repo root — backend starts without a host-side `mvn package`
2. `cd backend && skaffold dev` — pods come up healthy
3. Edit any controller method; save — Skaffold logs show a new build triggering within
   seconds; pod redeploys; `curl localhost:8080/actuator/info` reflects the change
4. Bump pom.xml `<version>` to `1.0.2` — `docker compose up` still produces a running
   image without any Dockerfile edit
5. Root README acceptance test flow works from step 1 (no `mvn package` pre-step)