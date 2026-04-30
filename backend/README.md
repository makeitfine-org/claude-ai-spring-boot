# Spring Boot REST API with JWT Authentication

A production-ready Spring Boot 3.4.1 application featuring a RESTful API for person management with JWT authentication, PostgreSQL database, and Kubernetes deployment support.

Part of the full-stack **claude-ai-spring-boot** project. Running `docker compose up` from the repository root also starts the React frontend on **port 3000**. See [`../frontend/README.md`](../frontend/README.md) for frontend documentation.

## Features

- **Spring Boot 3.4.1** with Java 21
- **JWT Authentication** — access tokens (15 min) + refresh tokens (7 days)
- **PostgreSQL 16** with Flyway migrations
- **RESTful API** with full CRUD for the Person entity
- **Pagination and sorting** on list endpoints
- **MapStruct 1.6.3** for DTO mapping (no Lombok)
- **Comprehensive testing** — JUnit 5, Mockito, Testcontainers
- **85%+ line coverage** enforced by JaCoCo
- **Docker / Docker Compose** support
- **Kubernetes** deployment with Skaffold
- **GitHub Actions** CI/CD pipeline
- **Spring Actuator** health and liveness probes

---

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker and Docker Compose
- `jq` (optional, for the token-capture shell examples below)
- PostgreSQL 16 (optional — only for bare-metal local dev)
- Kubernetes cluster + Skaffold (only for k8s deployment)

---

## Quick Start

### 1. Clone

```bash
git clone <repository-url>
cd claude-ai-spring-boot
```

### 2. Run with Docker Compose

```bash
docker compose up
```

Starts:
- PostgreSQL on port **5432**
- Spring Boot app on port **8080**

### 3. Verify

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

---

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/persondb` |
| `DATABASE_USER` | Database username | `postgres` |
| `DATABASE_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | Base64-encoded secret (min 256 bits) | Built-in test key |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile (`dev` / `prod`) | `dev` |

---

## API Reference

### Default credentials (seed data)

| Field | Value |
|---|---|
| Email | `test@example.com` |
| Password | `password` |

---

### Quick auth flow — capture token once, reuse everywhere

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}' \
  | jq -r '.accessToken')

echo $TOKEN   # paste into subsequent commands if not using the variable
```

All protected endpoints below use `-H "Authorization: Bearer $TOKEN"`.

---

### Authentication

#### POST /api/auth/login

Authenticate a user and receive access + refresh tokens. No auth header required.

**Request**

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password"
  }'
```

**Success — 200 OK**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer"
}
```

**Error — 401 Unauthorized** (wrong credentials)

```json
{
  "message": "Bad credentials"
}
```

---

#### POST /api/auth/refresh

Exchange a (non-expired) refresh token for a new token pair. Pass the refresh token as a Bearer token.

**Request**

```bash
REFRESH_TOKEN="<your-refresh-token>"

curl -s -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer $REFRESH_TOKEN"
```

**Success — 200 OK** — same shape as login response.

**Error — 400 Bad Request** (malformed or missing header)

```json
{
  "message": "Authorization header is missing or invalid"
}
```

---

### Person Management

All `/api/persons/**` endpoints require a valid access token.

#### PersonRequest — field rules

| Field | Type | Required | Constraints |
|---|---|---|---|
| `firstName` | string | Yes | max 100 chars |
| `lastName` | string | Yes | max 100 chars |
| `email` | string | Yes | valid email format, max 150 chars, unique |
| `phoneNumber` | string | No | max 20 chars |
| `street` | string | No | max 200 chars |
| `city` | string | No | max 100 chars |
| `postalCode` | string | No | max 10 chars |
| `country` | string | No | max 100 chars |
| `dateOfBirth` | date (YYYY-MM-DD) | No | must be a past date |
| `active` | boolean | No | defaults to `true` |

#### PersonResponse — shape

```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "phoneNumber": "+1234567890",
  "street": "123 Main St",
  "city": "New York",
  "postalCode": "10001",
  "country": "USA",
  "dateOfBirth": "1990-01-15",
  "active": true,
  "createdAt": "2026-04-26T10:30:00",
  "updatedAt": "2026-04-26T10:30:00",
  "version": 0
}
```

---

#### POST /api/persons — Create a person

```bash
curl -s -X POST http://localhost:8080/api/persons \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+1234567890",
    "street": "123 Main St",
    "city": "New York",
    "postalCode": "10001",
    "country": "USA",
    "dateOfBirth": "1990-01-15",
    "active": true
  }'
```

**Success — 201 Created** — returns a PersonResponse.

**Error — 400 Bad Request** (validation failure)

```json
{
  "message": "Validation failed",
  "errors": {
    "email": "must be a well-formed email address",
    "firstName": "must not be blank"
  }
}
```

**Error — 401 Unauthorized** — missing or expired token.

---

#### GET /api/persons — List persons (paginated)

Query parameters: `page` (default 0), `size` (default 20), `sort` (default `lastName,asc`).

```bash
# Default page
curl -s "http://localhost:8080/api/persons" \
  -H "Authorization: Bearer $TOKEN"

# Page 1, 10 per page, sorted by email ascending
curl -s "http://localhost:8080/api/persons?page=1&size=10&sort=email,asc" \
  -H "Authorization: Bearer $TOKEN"
```

**Success — 200 OK**

```json
{
  "content": [ /* array of PersonResponse */ ],
  "totalElements": 42,
  "totalPages": 3,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

#### GET /api/persons/{id} — Get person by ID

```bash
curl -s http://localhost:8080/api/persons/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Success — 200 OK** — returns a PersonResponse.

**Error — 404 Not Found**

```json
{
  "message": "Person not found with id: 1"
}
```

---

#### PUT /api/persons/{id} — Update a person

Send only the fields you want to change; all fields follow the same validation rules as POST.

```bash
curl -s -X PUT http://localhost:8080/api/persons/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@example.com",
    "phoneNumber": "+9876543210",
    "street": "456 Oak Ave",
    "city": "Los Angeles",
    "postalCode": "90001",
    "country": "USA",
    "dateOfBirth": "1992-05-20",
    "active": true
  }'
```

**Success — 200 OK** — returns the updated PersonResponse.

**Error — 404 Not Found** — person does not exist.

---

#### DELETE /api/persons/{id} — Delete a person

```bash
curl -s -X DELETE http://localhost:8080/api/persons/1 \
  -H "Authorization: Bearer $TOKEN"
```

**Success — 204 No Content** (empty body).

**Error — 404 Not Found** — person does not exist.

---

#### GET /api/persons/search — Search by email

```bash
curl -s "http://localhost:8080/api/persons/search?email=john.doe@example.com" \
  -H "Authorization: Bearer $TOKEN"
```

**Success — 200 OK** — returns a single PersonResponse.

**Error — 404 Not Found** — no person with that email.

---

### Health / Actuator

No authentication required.

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Kubernetes liveness probe
curl http://localhost:8080/actuator/health/liveness

# Kubernetes readiness probe
curl http://localhost:8080/actuator/health/readiness

# Metrics
curl http://localhost:8080/actuator/metrics

# App info
curl http://localhost:8080/actuator/info
```

---

## Local Development

### Start only the database

```bash
docker-compose up postgres -d
mvn spring-boot:run
```

### Build and test

```bash
# Compile + unit tests
mvn clean package

# Full test suite including Testcontainers integration tests
mvn verify

# Coverage report (opens at target/site/jacoco/index.html)
mvn jacoco:report
```

JaCoCo enforces **85% line coverage per package**. `mvn verify` fails if the gate is missed.

---

## Docker Deployment

```bash
# Build image
docker build -t backend .

# Start all services
docker-compose up -d

# Tail app logs
docker-compose logs -f app

# Stop all services
docker-compose down
```

---

## Kubernetes Deployment

`skaffold dev` watches `src/` and `pom.xml` for changes and triggers a full image
rebuild automatically (no manual `mvn` step required). Expected round-trip time after a
source change: **2–5 minutes** (Maven runs inside Docker on each rebuild).

```bash
# Deploy (live rebuild on file changes)
skaffold dev

# One-shot production deploy
skaffold run

# Port-forward to localhost
kubectl port-forward svc/backend 8080:8080

# Check pod status
kubectl get pods

# View logs
kubectl logs -f deployment/backend
```

---

## Database Schema

Flyway runs migrations automatically on startup. Migration files: `src/main/resources/db/migration/V{n}__{description}.sql`.

### Person table

| Column | Type | Constraints |
|---|---|---|
| id | BIGSERIAL | PRIMARY KEY |
| first_name | VARCHAR(100) | NOT NULL |
| last_name | VARCHAR(100) | NOT NULL |
| email | VARCHAR(150) | NOT NULL, UNIQUE |
| phone_number | VARCHAR(20) | |
| street | VARCHAR(200) | |
| city | VARCHAR(100) | |
| postal_code | VARCHAR(10) | |
| country | VARCHAR(100) | |
| date_of_birth | DATE | |
| active | BOOLEAN | NOT NULL, DEFAULT true |
| created_at | TIMESTAMP | NOT NULL |
| updated_at | TIMESTAMP | NOT NULL |
| version | BIGINT | NOT NULL, DEFAULT 0 |

Indexes: `idx_persons_email` (unique), `idx_persons_phone`, `idx_persons_active`, `idx_persons_created_at`.

---

## Testing

| Test type | Class pattern | Tool |
|---|---|---|
| Unit | `*ServiceTest.java` | JUnit 5 + Mockito |
| Repository | `*RepositoryTest.java` | `@DataJpaTest` + Testcontainers |
| Integration | `*IntegrationTest.java` | `@SpringBootTest` + Testcontainers |
| Security | `*SecurityTest.java` | Spring Security Test |

```bash
# Unit tests only
mvn test

# Full suite (integration + coverage gate)
mvn verify
```

Docker must be running for Testcontainers (PostgreSQL `postgres:16-alpine`).

---

## CI/CD Pipeline

GitHub Actions (`.github/workflows/ci.yml`):

1. **Build & Test** — compile, run all tests, enforce JaCoCo ≥ 85%, upload coverage report
2. **Security Scan** — OWASP dependency check
3. **Docker Build** — build and push image to Docker Hub (`main` branch only)

Required repository secrets: `DOCKER_USERNAME`, `DOCKER_PASSWORD`.

---

## Security

- All `/api/persons/**` endpoints require a valid JWT
- Access tokens expire after **15 minutes**; refresh tokens after **7 days**
- Passwords hashed with BCrypt
- CSRF disabled (stateless API)
- CORS enabled for all origins in dev; restrict in prod via `SPRING_PROFILES_ACTIVE=prod`
- Kubernetes secrets used for sensitive config

---

## Project Structure

```
src/
├── main/
│   ├── java/pl/piomin/services/
│   │   ├── config/           # Spring @Configuration classes
│   │   ├── domain/           # Entities and repository interfaces
│   │   ├── application/      # Services, DTOs, MapStruct mappers
│   │   ├── infrastructure/   # Security filters, exception handlers
│   │   └── presentation/     # REST controllers
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── application-prod.yml
│       └── db/migration/     # Flyway SQL migrations
└── test/
    └── java/pl/piomin/services/
        ├── controller/
        ├── service/
        ├── repository/
        └── integration/
```

---

## Technology Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.4.1, Java 21 |
| Security | Spring Security 6, JJWT 0.12.6 |
| Database | PostgreSQL 16, Spring Data JPA, Flyway |
| Mapping | MapStruct 1.6.3 |
| Testing | JUnit 5, Mockito, Testcontainers 1.21.4 |
| Coverage | JaCoCo 0.8.12 |
| Build | Maven |
| Containers | Docker, Docker Compose |
| Orchestration | Kubernetes, Skaffold |
| CI/CD | GitHub Actions |
| Monitoring | Spring Actuator |

---

## Troubleshooting

**App won't start**
```bash
docker-compose ps          # is postgres running?
docker-compose logs app    # check startup errors
```

**Database connection error** — verify `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD` match your running PostgreSQL instance.

**JWT auth fails** — confirm `JWT_SECRET` is set and Base64-encoded with at least 256 bits. Default test secret works out of the box.

**Tests fail** — Docker must be running (Testcontainers spins up a real PostgreSQL). Run with `-X` for verbose output: `mvn test -X`.

---

## Version

Current version: **1.0.2**
