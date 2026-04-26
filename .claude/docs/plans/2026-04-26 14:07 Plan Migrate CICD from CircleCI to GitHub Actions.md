# Plan: Migrate CI/CD from CircleCI to GitHub Actions

## Context

The project currently uses CircleCI (`.circleci/config.yml`) with three jobs — build/test, OWASP security scan, and Docker publish. The goal is to replace this with an equivalent GitHub Actions workflow so the pipeline runs natively in the GitHub repository without a third-party CI account.

---

## Current CircleCI Pipeline (reference)

| Job | Trigger | Steps |
|---|---|---|
| `build-and-test` | every push | `mvn clean verify`, JaCoCo report, artifact upload |
| `security-scan` | after build | `mvn org.owasp:dependency-check-maven:check` |
| `docker-build` | after build, `main` only | Docker build + push to Docker Hub |

CircleCI-specific variables being replaced:
- `$CIRCLE_PROJECT_USERNAME` → `${{ github.repository_owner }}`
- `$CIRCLE_SHA1` → `${{ github.sha }}`
- CircleCI `docker-hub` context → GitHub repository secrets

---

## Target: GitHub Actions Workflow

### Files to create / modify

| File | Action |
|---|---|
| `.github/workflows/ci.yml` | **Create** — replaces `.circleci/config.yml` |
| `.circleci/config.yml` | **Delete** |
| `README.md` | **Update** CI/CD section (~lines 293–310) |
| `CLAUDE.md` | **Update** stack reference: CircleCI → GitHub Actions |

---

## Workflow Design (`.github/workflows/ci.yml`)

**Triggers:** `push` to any branch, `pull_request` targeting `main`.

### Job 1 — `build-and-test`

Runner: `ubuntu-latest`  
Steps:
1. `actions/checkout@v4`
2. `actions/setup-java@v4` — Java 21 (temurin), `cache: 'maven'`
3. `mvn clean verify` (env: `MAVEN_OPTS=-Xmx3200m`)
4. `mvn jacoco:report`
5. Upload `target/site/jacoco/` as artifact `jacoco-report`
6. Upload `target/surefire-reports/` as artifact `test-results`
7. Upload `target/claude-ai-spring-boot-*.jar` as artifact `app-jar`

### Job 2 — `security-scan`

Needs: `build-and-test`  
Runner: `ubuntu-latest`  
Steps:
1. `actions/checkout@v4`
2. `actions/setup-java@v4` — Java 21, `cache: 'maven'`
3. Download artifact `app-jar`
4. `mvn org.owasp:dependency-check-maven:check`
5. Upload `target/dependency-check-report.html` as artifact `security-report`

### Job 3 — `docker-build`

Needs: `build-and-test`  
Condition: `github.ref == 'refs/heads/main'`  
Runner: `ubuntu-latest`  
Steps:
1. `actions/checkout@v4`
2. Download artifact `app-jar` → restore to `target/`
3. `docker/setup-buildx-action@v3`
4. `docker/login-action@v3` — secrets: `DOCKER_USERNAME`, `DOCKER_PASSWORD`
5. `docker/build-push-action@v5`:
   - tags: `${{ secrets.DOCKER_USERNAME }}/claude-ai-spring-boot:${{ github.sha }}` and `:latest`
   - push: true

---

## Required GitHub Secrets

Add these in the repository **Settings → Secrets → Actions**:

| Secret | Value |
|---|---|
| `DOCKER_USERNAME` | Docker Hub username |
| `DOCKER_PASSWORD` | Docker Hub password or access token |

---

## Verification

1. Push a feature branch — jobs `build-and-test` and `security-scan` should run; `docker-build` should be skipped.
2. Merge / push to `main` — all three jobs should run; Docker image should appear on Docker Hub tagged with the commit SHA and `latest`.
3. Introduce a test coverage failure (drop a test) — `build-and-test` should fail at `mvn verify` (JaCoCo gate).
4. Check the **Actions** tab for artifact uploads: `jacoco-report`, `test-results`, `security-report`.

---

## Mutations Log

| # | Type | Description |
|---|---|---|
| — | — | Initial plan |