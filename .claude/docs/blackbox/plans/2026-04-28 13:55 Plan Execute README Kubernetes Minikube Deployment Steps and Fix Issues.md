# Plan: Execute README Kubernetes (Minikube) Deployment Steps and Fix Issues

## Context

The README documents a full minikube deployment workflow (steps 1–6 in `Kubernetes Deployment (Minikube)`). User wants me to walk through every step, exercise both `backend/` and `frontend/` Skaffold pipelines, and patch any config or manifest defect that surfaces along the way.

State going in:
- `backend/skaffold.yaml` was already corrected earlier this session (top-level `manifests.rawYaml`, empty `deploy.kubectl: {}`) for v4beta11 schema.
- Exploration of `backend/k8s/*` and `frontend/k8s/*` shows configs are internally consistent (image names, env var names, service DNS, probes, ports, RBAC-free). No structural defects spotted statically.
- A previous session note confirms the frontend ConfigMap (`frontend-nginx-config`) overrides the in-image `nginx.conf` at runtime, so its `claude-ai-spring-boot:8080` upstream is the one that takes effect inside the cluster.

So the real work is **operational**: run the steps, observe failures, fix them, re-run. The plan below sequences that and lists the issues most likely to bite.

## Pre-flight checks (read-only, no changes)

1. Confirm host has `docker`, `minikube`, `kubectl`, `skaffold` on PATH.
2. Confirm host ports 3000, 5432, 8080 aren't already bound by `docker-compose` from a previous session — if they are, run `docker compose down` first (the README's quick-start uses the same ports).
3. Verify `backend/target/claude-ai-spring-boot-1.0.1.jar` exists (Dockerfile copies it). If missing, run `mvn clean package -DskipTests` in `backend/`.

## Step-by-step execution plan

### Step 1 — Start minikube
```
minikube start --cpus=4 --memory=6g --driver=docker
minikube status
```
Anticipated issues: insufficient host memory; Docker daemon not running. Surface the actual error to the user before patching.

### Step 2 — Point Docker at minikube
```
eval $(minikube docker-env)
docker info | grep -i name   # should show "minikube"
```

### Step 3 — Deploy backend (`cd backend && skaffold run`)

Use `skaffold run` (one-shot) for each module so we can inspect state cleanly between steps; the README presents `skaffold dev` and `skaffold run` as alternatives.

Anticipated failure modes and fixes:

| Failure | Likely cause | Fix |
|---|---|---|
| `parsing skaffold config: ... field manifests not found in type v4beta11.KubectlDeploy` | Already fixed earlier in session for backend; **frontend uses the deprecated nested form** and may also fail on stricter Skaffold versions | If it fires for frontend, mirror backend's structure in `frontend/skaffold.yaml`: top-level `manifests.rawYaml` + `deploy.kubectl: {}`. |
| `COPY failed: target/claude-ai-spring-boot-1.0.1.jar: not found` | JAR not built or version drift between `pom.xml` and `Dockerfile` | Run `mvn clean package -DskipTests` in `backend/`. If versions diverged, follow `.claude/rules/version-bump-procedure.md`. |
| Backend pod stuck in `CrashLoopBackOff` with Flyway "could not connect" | Postgres pod still initializing; backend has no init/wait | Check `kubectl logs deploy/claude-ai-spring-boot`; if recurrent, add `initialDelaySeconds` bump to readiness probe or an init container that waits for `postgres-service:5432`. Prefer probe tuning first — minimal change. |
| `ImagePullBackOff` | Skipped `eval $(minikube docker-env)` or `imagePullPolicy: Always` forcing a registry pull | Re-run the eval; if image manifest specifies `imagePullPolicy: Always`, change to `IfNotPresent` for local builds. (Currently neither manifest sets `imagePullPolicy`, so default `IfNotPresent` for non-`:latest` tags applies — should be fine.) |
| Pods `Pending` due to memory | 2 backend replicas × 1 GiB limit + postgres + frontend may pressure the 6GB cap once kube-system overhead is accounted | Reduce `backend/k8s/deployment.yaml` `replicas: 2` → `1` for local minikube, or restart minikube with `--memory=8g`. Recommend the replica reduction for dev. |
| `kubectl get svc claude-ai-spring-boot` shows `EXTERNAL-IP <pending>` | LoadBalancer needs `minikube tunnel`; expected on minikube | Document; not a config bug. Skaffold's `portForward` block makes the service reachable on `localhost:8080` regardless. |

Verification after Step 3:
```
kubectl get pods                               # postgres + backend Running
kubectl logs -l app=claude-ai-spring-boot --tail=50
curl -fsS http://localhost:8080/actuator/health   # uses skaffold portForward
```

### Step 4 — Deploy frontend (`cd frontend && skaffold run`)

Open a new shell, **re-run** `eval $(minikube docker-env)` (env vars don't propagate across terminals — README already calls this out).

Anticipated failure modes and fixes:

| Failure | Likely cause | Fix |
|---|---|---|
| Skaffold parse error on `manifests` | Frontend `skaffold.yaml` still uses the older nested form under `deploy.kubectl` | Migrate to the same top-level `manifests.rawYaml` shape as the backend. Path: `/home/eug/dev/projects/my/claude-ai-spring-boot/frontend/skaffold.yaml`. |
| Frontend pod up but `/api/*` returns 502 | nginx ConfigMap mounted, backend service name resolves, but backend pod not yet ready | Wait for backend readiness; if persistent, verify `kubectl get svc claude-ai-spring-boot` exists in default namespace and `kubectl exec` into frontend pod, run `wget -O- http://claude-ai-spring-boot:8080/actuator/health`. |
| `ConfigMap "frontend-nginx-config"` mount issue | subPath mount mismatch | Already verified consistent in exploration — only patch if reproducible. |

Verification after Step 4:
```
kubectl get pods                                # 2 frontend pods Running
curl -fsS http://localhost:3000/                # SPA index.html
curl -fsS http://localhost:3000/api/auth/login -X POST \
   -H 'Content-Type: application/json' \
   -d '{"email":"test@example.com","password":"password"}'
```

### Step 5 — Access the services

`minikube tunnel` in a dedicated terminal (only needed for direct LoadBalancer access; Skaffold port-forwards already cover localhost:3000 and localhost:8080). Confirm:
- http://localhost:3000 renders SPA
- http://localhost:8080/actuator/health returns `{"status":"UP"}`
- Browser login round-trip works end-to-end

### Step 6 — Teardown (only on user request)

```
cd backend && skaffold delete
cd frontend && skaffold delete
minikube stop     # or `minikube delete` for full reset
```

## Files most likely to need edits

In priority order:

1. **`frontend/skaffold.yaml`** — migrate to top-level `manifests.rawYaml` if the current nested form fails to parse on the installed Skaffold binary. (This was the earlier symptom for the backend.)
2. **`backend/k8s/deployment.yaml`** — drop `replicas: 2` → `1` if pods stay `Pending` due to memory pressure on a 6GB minikube. Same potentially for `frontend/k8s/deployment.yaml`.
3. **`backend/Dockerfile`** — only if `pom.xml` version differs from the hard-coded `claude-ai-spring-boot-1.0.1.jar` path; follow `.claude/rules/version-bump-procedure.md`.
4. **README** — append any new troubleshooting row that arose during execution (only if a genuinely new failure mode appears).

No edits planned to: ConfigMaps, Secrets, Services, postgres StatefulSet, probes — exploration found these consistent.

## Verification (end-to-end)

After all fixes:
```
kubectl get pods                                              # all Running
kubectl get svc                                               # backend+frontend services present
curl -fsS http://localhost:8080/actuator/health               # {"status":"UP"}
curl -fsS http://localhost:3000/ | head -5                    # SPA HTML
# Auth round-trip via the nginx /api/ proxy:
curl -fsS http://localhost:3000/api/auth/login -X POST \
   -H 'Content-Type: application/json' \
   -d '{"email":"test@example.com","password":"password"}'
```

If all four return 2xx, the deployment is green and the plan is complete. Update `blackbox/session-log.md` per the blackbox policy with: decisions made, files modified, and any issues fixed.

## Out of scope

- Changing service types away from `LoadBalancer` (the README explicitly relies on `minikube tunnel` / Skaffold portForward — both work).
- CI changes (`.github/workflows/ci.yml`) unless a manifest change forces it.
- Production-hardening (CORS lockdown, image registry push, Helm conversion).