# claude-ai-spring-boot

Spring Boot 4.x REST API with PostgreSQL, JWT/OAuth2 security (Keycloak), and Kubernetes deployment via Skaffold.

## Version

**1.0.1**

## Entities

- **Worker** — `id`, `name`, `surname`, `age`
- **Work** — `id`, `title`, `description`, `endDate`, `price`, `payDate`, `worker_id` (primary assigned worker)
- **WorkerWork** — junction table `id`, `worker_id`, `work_id`

## REST API

All endpoints require a valid JWT bearer token.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/workers` | List all workers |
| GET | `/api/workers/{id}` | Get worker by ID |
| POST | `/api/workers` | Create worker |
| PUT | `/api/workers/{id}` | Update worker |
| DELETE | `/api/workers/{id}` | Delete worker |
| GET | `/api/works` | List all works |
| GET | `/api/works/{id}` | Get work by ID |
| POST | `/api/works` | Create work |
| PUT | `/api/works/{id}` | Update work |
| DELETE | `/api/works/{id}` | Delete work |

## Running locally

### With Docker Compose

```bash
mvn package -DskipTests
docker-compose up
```

- Application: http://localhost:8080
- Keycloak admin: http://localhost:8180 (admin/admin)

### Get a token from Keycloak

```bash
TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -d "client_id=admin-cli&username=admin&password=admin&grant_type=password" \
  | jq -r '.access_token')

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/workers
```

## Running tests

```bash
mvn test
```

Tests use mocked JWT — no Keycloak required.

## Deploy to Kubernetes with Skaffold

```bash
mvn package -DskipTests
skaffold run
```

Prerequisites: local cluster running (minikube or kind), `kubectl` and `skaffold` installed.

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `appdb` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `DDL_AUTO` | `create-drop` | Hibernate DDL mode |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8180/realms/master` | Keycloak realm issuer URI |
