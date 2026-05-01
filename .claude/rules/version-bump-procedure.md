# Version Bump Procedure

Triggered whenever `<version>` changes under the `backend` artifact in `pom.xml`:

```xml
<groupId>pl.piomin.services</groupId>
<artifactId>backend</artifactId>
<version>X.Y.Z</version>
```

The Dockerfile uses a wildcard `COPY --from=build /app/target/backend-*.jar app.jar` so
no Dockerfile edit is needed when bumping the version.

## Steps (execute in order; fix any errors before continuing)

1. **Build** — `mvn clean verify`

2. **Remove old Docker image** — `docker rmi spring-cloud2-api-gateway:latest`
   (ignore "image not found" errors)

3. **Stop running containers** — `docker compose down`

4. **Start fresh** — `docker compose up`

If any step fails, diagnose and fix the root cause before retrying — do not skip steps.
