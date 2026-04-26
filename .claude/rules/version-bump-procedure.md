# Version Bump Procedure

Triggered whenever `<version>` changes under the `claude-ai-spring-boot` artifact in `pom.xml`:

```xml
<groupId>pl.piomin.services</groupId>
<artifactId>claude-ai-spring-boot</artifactId>
<version>X.Y.Z</version>
```

## Steps (execute in order; fix any errors before continuing)

1. **Update Dockerfile** — change the `COPY` line to reference the new version:
   ```
   COPY target/claude-ai-spring-boot-{NEW_VERSION}.jar app.jar
   ```

2. **Build** — `mvn clean install`

3. **Remove old Docker image** — `docker rmi spring-cloud2-api-gateway:latest`
   (ignore "image not found" errors)

4. **Stop running containers** — `docker compose down`

5. **Start fresh** — `docker compose up`

If any step fails, diagnose and fix the root cause before retrying — do not skip steps.