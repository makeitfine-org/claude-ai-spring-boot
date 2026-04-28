# Add Kubernetes Deployment for Frontend + Minikube/Skaffold Docs

## Context

The backend module already ships with full Kubernetes manifests (`backend/k8s/*.yaml`)
and a `backend/skaffold.yaml`, but the **frontend has no equivalent** — it can only
be run via `docker-compose`. The goal is to:

1. Mirror the backend's k8s + skaffold pattern for the frontend so both modules
   can be deployed to a local minikube cluster with `skaffold`.
2. Replace the docker-compose-only nginx upstream (`http://app:8080`) with the
   in-cluster backend service hostname (`http://claude-ai-spring-boot:8080`)
   without breaking docker-compose.
3. Add a self-contained section to the root `README.md` explaining how to
   install/configure minikube and deploy both backend and frontend with skaffold.

## Existing Patterns to Mirror (from `backend/`)

- Image name = artifact name; `local.push: false` (works with minikube docker-env)
- `LoadBalancer` Service (no Ingress in this repo)
- Labels: `app: <service-name>` everywhere
- Liveness + readiness probes, resource requests/limits, 2 replicas
- `skaffold.yaml` lists every manifest explicitly under `deploy.kubectl.manifests`
- `portForward` block for dev access

## Files to Create

### `frontend/k8s/configmap.yaml` (new)
Holds an nginx config tuned for in-cluster DNS. The backend service in
`backend/k8s/service.yaml` is named `claude-ai-spring-boot`, so the upstream
becomes `http://claude-ai-spring-boot:8080/api/` (replacing the docker-compose
hostname `app`). Mounted into the pod at `/etc/nginx/conf.d/default.conf` — this
overrides the `nginx.conf` baked into the image, so docker-compose behavior is
untouched.

### `frontend/k8s/deployment.yaml` (new)
- `replicas: 2`, image `claude-ai-frontend`
- Mounts the ConfigMap as `/etc/nginx/conf.d/default.conf` (subPath)
- `livenessProbe` / `readinessProbe`: HTTP GET `/` on port 80
- Modest resources (nginx is light): req 64Mi/50m, limit 128Mi/200m

### `frontend/k8s/service.yaml` (new)
- `type: LoadBalancer`, port `3000` → targetPort `80` (matches the
  docker-compose host-port mapping so users hit the same URL).

### `frontend/skaffold.yaml` (new)
Same shape as `backend/skaffold.yaml`:
- artifact image `claude-ai-frontend`, dockerfile `Dockerfile`, `local.push: false`
- manifests: `k8s/configmap.yaml`, `k8s/deployment.yaml`, `k8s/service.yaml`
- `portForward` for the frontend service on local 3000 → 3000

## Files to Modify

### `README.md` (root)
Replace the current short "Kubernetes Deployment" section (lines 83–91) with a
detailed, ordered guide:

1. **Prerequisites** — `minikube`, `kubectl`, `skaffold`, `docker` (link to
   official install pages; do not invent versions).
2. **Create & start a local minikube cluster**
   ```bash
   minikube start --cpus=4 --memory=6g --driver=docker
   minikube status
   ```
   Note: 6 GB needed because backend pod requests 1 GiB and postgres + frontend
   add overhead.
3. **Point local docker at minikube's daemon** (so skaffold's
   `local.push: false` works):
   ```bash
   eval $(minikube docker-env)
   ```
   (Repeat per shell.)
4. **Deploy backend** (existing flow, restated for completeness):
   ```bash
   cd backend
   skaffold dev      # or: skaffold run
   ```
5. **Deploy frontend** (new):
   ```bash
   cd frontend
   skaffold dev      # or: skaffold run
   ```
6. **Expose `LoadBalancer` services on minikube** — both modules use
   `type: LoadBalancer`, which on minikube requires a tunnel:
   ```bash
   minikube tunnel
   ```
   Then open `http://localhost:3000` (frontend) and
   `http://localhost:8080/actuator/health` (backend).

   Alternative without `minikube tunnel`: skaffold's `portForward` blocks
   already forward 3000 and 8080 to localhost while `skaffold dev` is running.
7. **Teardown**
   ```bash
   skaffold delete   # in each module dir
   minikube stop     # or: minikube delete
   ```
8. **Troubleshooting** — short list:
   - `ImagePullBackOff` → forgot `eval $(minikube docker-env)` before building
   - Pod stuck `Pending` → bump minikube memory/cpu
   - Frontend cannot reach backend → confirm `kubectl get svc claude-ai-spring-boot` exists in the same namespace

Keep the rest of the README intact. Update the "Repository Layout" snippet
(lines 56–72) to show `frontend/k8s/` and `frontend/skaffold.yaml`.

## Decisions (intentionally NOT changed)

- **Do not edit `frontend/nginx.conf`** — it stays correct for docker-compose
  (upstream `app`). The K8s ConfigMap supplies the cluster-specific version.
- **Do not introduce an Ingress** — backend already uses `LoadBalancer`; using
  the same pattern for the frontend keeps both modules symmetric.
- **Do not bake `VITE_API_BASE_URL` into the K8s image** — the nginx
  `/api/` proxy already gives the SPA a same-origin path, so the default build
  works as-is in cluster.
- **Do not move postgres manifests** — they remain owned by the backend module.

## Critical Files

- `frontend/Dockerfile` (read-only, multi-stage already correct)
- `frontend/nginx.conf` (read-only)
- `backend/skaffold.yaml` (template to mirror)
- `backend/k8s/service.yaml`, `backend/k8s/deployment.yaml` (templates)
- `README.md` (sections at lines 56–72 and 83–91)
- `frontend/CLAUDE.md` line 28 — confirms relative `/api` paths work because
  axios `baseURL` is a host (no path), so `nginx` proxy handles `/api/*`.

## Verification

1. `minikube start --cpus=4 --memory=6g`
2. `eval $(minikube docker-env)`
3. `cd backend && skaffold run` → `kubectl get pods` shows
   `claude-ai-spring-boot-*` and `postgres-0` Ready.
4. `cd ../frontend && skaffold run` → `kubectl get pods` shows
   `claude-ai-frontend-*` Ready (2 replicas).
5. In a second terminal: `minikube tunnel`.
6. `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.
7. `curl -I http://localhost:3000/` → `200 OK`, served by nginx.
8. Browser: open `http://localhost:3000`, log in with
   `test@example.com` / `password`, list persons — confirms the in-cluster
   `/api/` proxy hits the backend Service correctly.
9. Teardown: `skaffold delete` in each module, then `minikube stop`.