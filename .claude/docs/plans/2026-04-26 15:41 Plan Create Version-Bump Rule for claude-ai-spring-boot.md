# Plan: Create Version-Bump Rule for claude-ai-spring-boot

## Context

The user wants a persistent rule in `.claude/rules/` that codifies what must happen every time the application version in `pom.xml` changes. Currently `Dockerfile` has the version hardcoded (`claude-ai-spring-boot-1.0.0.jar`) and must be kept in sync. The rule will also trigger a full rebuild and Docker Compose restart.

---

## Files to Create / Modify

| File | Change |
|---|---|
| `.claude/rules/version-bump-procedure.md` | New rule file |
| `Dockerfile` | Fix hardcoded `1.0.0` → `1.0.1` to match current version (one-time fix as part of this task) |

---

## Step-by-Step Implementation

### Step 1 — Fix the Dockerfile now (1.0.0 → 1.0.1)

`Dockerfile` line 3 currently reads:
```
COPY target/claude-ai-spring-boot-1.0.0.jar app.jar
```
Change to:
```
COPY target/claude-ai-spring-boot-1.0.1.jar app.jar
```

### Step 2 — Create `.claude/rules/version-bump-procedure.md`

Rule content (see Final Plan section below).

---

## Rule Content

```markdown
# Version Bump Procedure

Triggered whenever `<version>` changes under the `claude-ai-spring-boot` artifact in `pom.xml`.

## Steps (execute in order; fix any errors before continuing)

1. **Update Dockerfile** — change the `COPY target/claude-ai-spring-boot-*.jar` line to use the new version:
   ```
   COPY target/claude-ai-spring-boot-{NEW_VERSION}.jar app.jar
   ```

2. **Build** — `mvn clean install`

3. **Remove old image** — `docker rmi spring-cloud2-api-gateway:latest` (ignore "not found" errors)

4. **Stop running containers** — `docker compose down`

5. **Start fresh** — `docker compose up`

If any step fails, diagnose and fix the root cause before retrying — do not skip steps.
```

---

## Verification

After creating the rule and fixing the Dockerfile, confirm:
- `Dockerfile` references `1.0.1`
- Rule file exists at `.claude/rules/version-bump-procedure.md`