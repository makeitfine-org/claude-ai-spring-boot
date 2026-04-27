# CLAUDE.md — claude-ai-spring-boot

## Stack Reference
- Java 21, Spring Boot 3.4.1, PostgreSQL 16
- MapStruct 1.6.3 (no Lombok), JJWT 0.12.6
- Testcontainers 1.21.4, JaCoCo 0.8.12
- Maven · Flyway · GitHub Actions · Docker · Kubernetes + Skaffold

## Architecture Layers
```
domain/         → entities, repository interfaces only
application/    → services, DTOs, MapStruct mappers
presentation/   → REST controllers (@RestController)
infrastructure/ → security, exception handlers, config
config/         → Spring @Configuration classes
```
Never let a lower layer import from a higher layer.
DTOs live in `application/dto/`. Mappers go in `application/mapper/` — always use MapStruct interfaces, never manual mapping.

## Coding Rules
- Group ID / base package: `pl.piomin.services`
- Maven `artifactId` = parent directory name, version = semantic `MAJOR.MINOR.PATCH`
- Bump PATCH on every generated version; update `README.md` at the same time
- No Lombok — use plain Java records or classes
- Use Context7 MCP before writing any Spring Boot / library API code — training data drifts

## Database Migrations (Flyway)
- File pattern: `src/main/resources/db/migration/V{n}__{description}.sql`
  - Example: `V4__add_refresh_tokens_table.sql`
- Never modify an already-applied migration — always add a new one
- Increment `n` sequentially; leave no gaps

## Testing Requirements
- JaCoCo minimum: **85% line coverage per package** (enforced by `mvn verify`)
- Excluded from coverage: `PersonApplication`
- Integration tests: use Testcontainers (`postgres:16-alpine`) — never H2 for integration
- Required test files per feature:
  - `*ServiceTest.java` — unit, mocked repo
  - `*RepositoryTest.java` — `@DataJpaTest` + Testcontainers
  - `*IntegrationTest.java` — full slice via `@SpringBootTest`
- Both positive and negative cases required for every test class

## Version & Delivery Checklist
Before marking any task done:
1. `mvn verify` passes (tests + JaCoCo gate)
2. Flyway migration numbered correctly
3. `pom.xml` version bumped (PATCH), `README.md` updated
4. `docker-compose.yml` reflects any new components
5. GitHub Actions `.github/workflows/ci.yml` updated if pipeline steps changed
6. Kubernetes manifests in `k8s/` updated for deployment changes

## Agent Selection Guide
| Task | Agent |
|---|---|
| New feature / REST endpoint / JPA entity | `spring-boot-engineer` |
| Architectural decision / package restructure | `java-architect` |
| Test gaps / coverage failures | `test-automator` |
| Security config / JWT / auth flows | `security-engineer` |
| Dockerfile / docker-compose changes | `docker-expert` |
| k8s manifests / Skaffold / Helm | `kubernetes-specialist` |
| GitHub Actions pipeline changes | `devops-engineer` |
| Pre-merge quality gate | `code-reviewer` |

Delegate to subagents liberally — keep the main context window clean.
Load skills from `.claude/skills/` for targeted in-context capabilities (e.g. `jpa-patterns` for N+1 issues, `api-contract-review` before releasing endpoints).

## Workflow Defaults
- Plan mode for any task with 3+ steps or an architectural decision
- Session logging: see `.claude/rules/blackbox-policy.md` — do not duplicate here
- Use **Context7 MCP** proactively for library/API docs — don't wait to be asked
- Commits: semantic message ≤ 80 chars, no `Co-Authored-By` trailer
- Lessons from corrections → `tasks/lessons.md`; review at session start
- When compacting, always preserve the full list of modified files and any test commands
