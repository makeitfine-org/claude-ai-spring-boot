# Plan: Containerize Frontend and Wire into Docker Compose

## Context
The project has a React 19 + Vite frontend at `frontend/` with no Dockerfile. The backend is already containerized (`backend/Dockerfile`, Spring Boot on port 8080). Docker Compose currently runs only `postgres` and `app`. Goal: add a production-ready frontend container served by nginx, proxy API calls through nginx to the backend, and verify the full stack works end-to-end.

---

## Files to Create / Modify

| File | Action |
|---|---|
| `frontend/Dockerfile` | Create — multi-stage Node build + nginx serve |
| `frontend/nginx.conf` | Create — serve SPA, proxy `/api/` to backend |
| `docker-compose.yml` | Modify — add `frontend` service |

---

## Step 1 — `frontend/Dockerfile` (multi-stage)

```dockerfile
FROM node:22-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

- `node:22-alpine` — matches the project's modern JS stack (Vite 8, TS 6, React 19)
- `npm ci` before `COPY .` — maximizes layer cache reuse
- `nginx:1.27-alpine` — lightweight, production-grade static file server

---

## Step 2 — `frontend/nginx.conf`

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # SPA fallback — React Router HTML5 history
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy all API calls to the Spring Boot backend container
    location /api/ {
        proxy_pass         http://app:8080/api/;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
    }
}
```

- `try_files … /index.html` — required for React Router (prevents 404 on hard reload)
- `http://app:8080` — resolves via the `app-network` bridge network (same as backend service name in compose)

---

## Step 3 — `docker-compose.yml` additions

Add a `frontend` service after `app`:

```yaml
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: claude-ai-frontend
    depends_on:
      app:
        condition: service_healthy
    ports:
      - "3000:80"
    networks:
      - app-network
    restart: unless-stopped
```

- Port **3000** on host → port 80 in container
- `depends_on: app: condition: service_healthy` — waits for Spring Boot actuator health check to pass before nginx starts

---

## Verification

```bash
# 1. Build all images (including frontend)
docker compose build

# 2. Start full stack
docker compose up -d

# 3. Check all containers are running
docker compose ps

# 4. Tail logs to spot startup errors
docker compose logs -f

# 5. Frontend serves the React app
curl -s http://localhost:3000 | grep -o '<div id="root">'

# 6. API proxy works (unauthenticated endpoint — actuator exposed through backend, not proxied)
curl -s http://localhost:8080/actuator/health

# 7. Register a user and exercise API through the frontend container (nginx proxy)
curl -s -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Pass123!","role":"USER"}'

# 8. Login and get a JWT
TOKEN=$(curl -s -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"Pass123!"}' | jq -r '.token')

# 9. Fetch persons through the nginx proxy (authenticated)
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:3000/api/persons
```

A `200 OK` on step 9 confirms the full chain: browser → nginx → Spring Boot → PostgreSQL.