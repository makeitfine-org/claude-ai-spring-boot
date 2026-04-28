# claude-ai-spring-boot

A full-stack person management application: Spring Boot 3.4.1 REST API with JWT authentication backed by PostgreSQL, and a React 19 SPA served by Nginx. All three services run together with a single `docker-compose up`.

---

## Architecture

```
Browser
  └─► Nginx  (port 3000)
        ├─► React SPA          (static files)
        └─► /api/*  ──────►  Spring Boot  (port 8080)
                                  └─► PostgreSQL  (port 5432)
```

---

## Quick Start

```bash
docker-compose up
```

Starts all three services. Verify the backend is healthy:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

Then open **http://localhost:3000** in your browser.

---

## Services

| Service | Port | Description |
|---|---|---|
| `postgres` | 5432 | PostgreSQL 16 database |
| `app` | 8080 | Spring Boot REST API |
| `frontend` | 3000 | React SPA served by Nginx |

---

## Default Credentials

| Field | Value |
|---|---|
| Email | `test@example.com` |
| Password | `password` |

---

## Repository Layout

```
backend/              → Spring Boot application (Java 21, Maven)
  src/                → source code and tests
  k8s/                → Kubernetes manifests
  Dockerfile
  pom.xml
  skaffold.yaml

frontend/             → React 19 + Vite SPA
  src/                → source code and tests
  Dockerfile
  nginx.conf

docker-compose.yml    → local full-stack orchestration
.github/workflows/    → GitHub Actions CI/CD
```

---

## Module Documentation

- **Backend** — API reference, environment variables, curl examples, testing, Kubernetes deployment: [`backend/README.md`](backend/README.md)
- **Frontend** — dev setup, project structure, auth flow, Docker, testing: [`frontend/README.md`](frontend/README.md)

---

## Kubernetes Deployment

```bash
cd backend
skaffold dev    # live rebuild on file changes
skaffold run    # one-shot production deploy
```

See [`backend/README.md`](backend/README.md) for full Kubernetes and Skaffold instructions.
