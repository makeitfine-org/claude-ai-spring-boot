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
make dockerAll
```

Builds backend + frontend, runs acceptance tests, then starts all three services with `docker compose up`.
Run `make help` to see all available targets.

Alternatively, start only the pre-built images (no build step):

```bash
docker compose up
```

Verify the backend is healthy:

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
  k8s/                → Kubernetes manifests
  Dockerfile
  nginx.conf
  skaffold.yaml

e2e/                  → Cucumber + Playwright acceptance tests
  features/           → Gherkin scenarios (auth, UI, API, DB)
  src/                → step definitions and support utilities

docker-compose.yml    → local full-stack orchestration
.github/workflows/    → GitHub Actions CI/CD
```

---

## Module Documentation

- **Backend** — API reference, environment variables, curl examples, testing, Kubernetes deployment: [`backend/README.md`](backend/README.md)
- **Frontend** — dev setup, project structure, auth flow, Docker, testing: [`frontend/README.md`](frontend/README.md)
- **E2E** — acceptance test setup, running, writing new scenarios, CI integration: [`e2e/README.md`](e2e/README.md)

---

## Running Acceptance Tests (E2E)

```bash
# 1. Start the full stack (Docker builds the backend JAR automatically)
docker compose up -d --wait

# 2. Install e2e dependencies (first time only)
cd e2e && cp .env.example .env && npm install && npx playwright install --with-deps chromium

# 3. Run all scenarios
npm test

# Run by scope
npm run test:ui     # browser-driven SPA scenarios
npm run test:api    # REST contract scenarios
npm run test:auth   # login / JWT lifecycle
npm run test:db     # database integrity
```

Reports land in `e2e/reports/cucumber.html`. See [`e2e/README.md`](e2e/README.md) for the full guide.

---

## Kubernetes Deployment (Minikube)

Both modules ship with Kubernetes manifests and a `skaffold.yaml`. The steps below
deploy the full stack — backend + frontend — to a local minikube cluster.

### Prerequisites

Install the following tools before starting:

- [Docker](https://docs.docker.com/get-docker/)
- [minikube](https://minikube.sigs.k8s.io/docs/start/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Skaffold](https://skaffold.dev/docs/install/)

### 1. Start a local minikube cluster

```bash
minikube start --cpus=4 --memory=6g --driver=docker
minikube status
```

> 6 GB of memory is recommended: the backend pod requests 1 GiB, and postgres + frontend add overhead.

### 2. Point Docker at minikube's daemon

Skaffold builds images locally with `push: false`. For minikube to see them, your
shell's Docker client must talk to minikube's Docker daemon:

```bash
eval $(minikube docker-env)
```

Run this in every new terminal before building or deploying.

### 3. Deploy the backend

```bash
cd backend
skaffold dev    # live rebuild on file changes
# or:
skaffold run    # one-shot deploy
```

Verify pods are ready:

```bash
kubectl get pods
# NAME                                  READY   STATUS    RESTARTS
# backend-<hash>          1/1     Running   0
# postgres-0                            1/1     Running   0
```

### 4. Deploy the frontend

Open a new terminal (remember to re-run `eval $(minikube docker-env)`):

```bash
cd frontend
skaffold dev    # live rebuild on file changes
# or:
skaffold run    # one-shot deploy
```

Verify:

```bash
kubectl get pods
# NAME                                  READY   STATUS    RESTARTS
# claude-ai-frontend-<hash>             1/1     Running   0  (×2 replicas)
```

### 5. Access the services

Both modules use `type: LoadBalancer`. On minikube, run a tunnel to expose them:

```bash
minikube tunnel    # keep this running in a dedicated terminal
```

Then open:

- **Frontend** → http://localhost:3000
- **Backend health** → http://localhost:8080/actuator/health

> **Without `minikube tunnel`**: Skaffold's `portForward` blocks already forward
> ports 3000 and 8080 to localhost while `skaffold dev` is running.

### 6. Teardown

```bash
# In each module directory:
skaffold delete

# Stop or delete the cluster:
minikube stop     # pause (preserves state)
minikube delete   # full reset
```

### Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `ImagePullBackOff` | minikube can't find the image | Re-run `eval $(minikube docker-env)` then redeploy |
| Pod stuck `Pending` | Insufficient cluster resources | Restart minikube with more `--memory` / `--cpus` |
| Frontend can't reach backend | Backend service missing | Run `kubectl get svc backend` and confirm it exists |
| 502 Bad Gateway on `/api/` | Backend not ready yet | Wait for backend pods to pass readiness probes |
