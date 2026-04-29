# CLAUDE.md — backend

## Stack

- Java 21, Spring Boot 3.4.1, PostgreSQL 16
- MapStruct 1.6.3 (no Lombok), JJWT 0.12.6
- Testcontainers 1.21.4, JaCoCo 0.8.12
- Maven, Flyway, Docker, Kubernetes + Skaffold

## Architecture Layers

```
domain/         → entities, repository interfaces only
application/    → services, DTOs, MapStruct mappers
presentation/   → REST controllers (@RestController)
infrastructure/ → security filters, exception handlers
config/         → Spring @Configuration classes
```

- Never let a lower layer import from a higher layer.
- DTOs live in `application/dto/`. Mappers in `application/mapper/` — always use MapStruct interfaces, never manual mapping.

## Coding Rules

- Group ID / base package: `pl.piomin.services`
- Maven `artifactId` = `backend`, version = semantic `MAJOR.MINOR.PATCH`
- No Lombok — use plain Java records or classes
- Use **Context7 MCP** before writing any Spring Boot / library API code — training data drifts

## Database Migrations (Flyway)

- File pattern: `src/main/resources/db/migration/V{n}__{description}.sql`
- Never modify an already-applied migration — always add a new one
- Increment `n` sequentially; leave no gaps (currently V1–V4)

## Testing Requirements

- JaCoCo minimum: **85% line coverage per package** (enforced by `mvn verify`)
- Excluded from coverage: `PersonApplication`
- Integration tests: Testcontainers (`postgres:16-alpine`) — never H2 for integration
- Required test files per feature:
  - `*ServiceTest.java` — unit, mocked repo
  - `*RepositoryTest.java` — `@DataJpaTest` + Testcontainers
  - `*IntegrationTest.java` — full slice via `@SpringBootTest`
- Both positive and negative cases required for every test class

## Version & Delivery Checklist

Before marking any backend task done:
1. `mvn verify` passes (tests + JaCoCo gate)
2. Flyway migration numbered correctly (no gaps, no edits to existing files)
3. `pom.xml` version bumped (PATCH); `backend/README.md` version line updated
4. `docker-compose.yml` reflects any new env vars or services
5. GitHub Actions `.github/workflows/ci.yml` updated if pipeline steps changed
6. Kubernetes manifests in `backend/k8s/` updated for deployment changes
7. Follow `.claude/rules/version-bump-procedure.md` for the full Dockerfile + rebuild sequence
