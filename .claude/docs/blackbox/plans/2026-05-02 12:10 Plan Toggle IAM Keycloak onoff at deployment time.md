# Plan: Toggle IAM (Keycloak) on/off at deployment time

## Context

Today the app is **unconditionally** wired to Keycloak: base `application.yml`
declares all OIDC properties without `@ConditionalOnProperty`/`@Profile` gating,
the `keycloak` service in `docker-compose.yml` has no `profiles:` key, and
`backend/skaffold.yaml` always deploys all four Keycloak manifests
(`keycloak-{configmap,deployment,secret,service}.yaml`). Spinning up the stack
for a quick demo, smoke test, or frontend-only iteration therefore costs the
~5–10s Keycloak boot and the operational burden of running an extra container
or LoadBalancer service.

The user wants a single switch that:
- when **on** (default), behaves as today: Keycloak deployed, OIDC + local-JWT
  login both available;
- when **off**, runs **fully open** (no auth at all): no Keycloak deployed, all
  endpoints `permitAll`, frontend bypasses login and `ProtectedRoute`.

The toggle must work in three environments: Docker Compose, Kubernetes via
Skaffold, and local `mvn spring-boot:run` / `npm run dev`.

## Approach — single boolean flag, three deployment overlays

**One source of truth:** a Spring property `app.iam.enabled` (env var
`IAM_ENABLED`), default `true`. The backend gates all IAM behaviour on this
flag; deployment tooling controls *both* the flag *and* whether the Keycloak
container/manifests are spun up.

### Backend (`backend/`)

Files to modify:

- `src/main/resources/application.yml` — add `app.iam.enabled: ${IAM_ENABLED:true}`.
- `config/SecurityConfig.java` — branch the `SecurityFilterChain` bean: when
  `app.iam.enabled=false`, return a chain with `.authorizeHttpRequests(a ->
  a.anyRequest().permitAll())`, CSRF disabled, no OAuth2 login, no resource
  server. Use a constructor-injected `@Value("${app.iam.enabled}")` flag (not
  two separate beans — avoids `@ConditionalOnProperty` interactions with
  Spring Security auto-config).
- `config/IdentityProviderConfig.java`, `infrastructure/identity/KeycloakIdentityProvider.java`,
  `infrastructure/security/OidcLoginSuccessHandler.java` —
  annotate with `@ConditionalOnProperty(prefix = "app.iam", name = "enabled",
  havingValue = "true", matchIfMissing = true)`.
- `application/service/RegistrationService.java` — when IAM disabled, registration
  endpoint should still work against the local DB (no Keycloak admin call). Inject
  the flag and short-circuit the `KeycloakIdentityProvider` call.
- New endpoint `GET /api/config` (public) returning `{ "iamEnabled": boolean }`
  so the frontend can adapt at runtime. Add to a small `ConfigController`.

### Frontend (`frontend/`)

Files to modify:

- `src/auth/AuthContext.tsx` — on first mount, fetch `/api/config`. If
  `iamEnabled=false`, set a synthetic anonymous user and skip all session checks.
- `src/auth/ProtectedRoute.tsx` — when context says IAM disabled, render
  children directly.
- Login page — hide the form / show a banner "IAM disabled — open mode" when off.

### Docker Compose

Files to modify:

- `docker-compose.yml` — add `profiles: ["iam"]` to the `keycloak` service.
  App service env: `IAM_ENABLED: ${IAM_ENABLED:-true}`.
- `.env` (new, committed) — `COMPOSE_PROFILES=iam`, `IAM_ENABLED=true`. This
  preserves "IAM on by default" for `docker compose up` and `make dockerAll`.
- `Makefile` — add `dockerAllNoIam` target that runs with
  `COMPOSE_PROFILES= IAM_ENABLED=false docker compose up`.

### Kubernetes / Skaffold

Files to modify:

- `backend/k8s/` — move the four Keycloak manifests into `backend/k8s/iam/`.
- `backend/skaffold.yaml` — keep base `manifests.rawYaml` listing both app
  manifests **and** `k8s/iam/*.yaml` (default = IAM on). Add a `no-iam`
  Skaffold profile that:
  - drops the `k8s/iam/*.yaml` entries from `manifests.rawYaml` via patch;
  - sets `IAM_ENABLED=false` on the app Deployment env via `setValueTemplates`
    or a Kustomize patch.
- Usage: `skaffold dev` → IAM on; `skaffold dev -p no-iam` → IAM off.

### Local dev (mvn / npm)

No code changes beyond the above. Document:
- `IAM_ENABLED=false mvn spring-boot:run` runs the backend open.
- Frontend auto-adapts via `/api/config`.

### Tests

- e2e Cucumber/Playwright scenarios assume IAM on — keep them on the default
  path (`make build` already runs full stack).
- Add one backend slice test that asserts `SecurityConfig` produces an open
  chain when `app.iam.enabled=false` (e.g. `@SpringBootTest` with property
  override + MockMvc hitting a protected endpoint expecting 200).

## Critical files

- `backend/src/main/resources/application.yml`
- `backend/src/main/java/pl/piomin/services/config/SecurityConfig.java`
- `backend/src/main/java/pl/piomin/services/config/IdentityProviderConfig.java`
- `backend/src/main/java/pl/piomin/services/infrastructure/identity/KeycloakIdentityProvider.java`
- `backend/src/main/java/pl/piomin/services/infrastructure/security/OidcLoginSuccessHandler.java`
- `backend/src/main/java/pl/piomin/services/application/service/RegistrationService.java`
- `frontend/src/auth/AuthContext.tsx`, `frontend/src/auth/ProtectedRoute.tsx`
- `docker-compose.yml`, `.env`, `Makefile`
- `backend/skaffold.yaml`, `backend/k8s/iam/*` (relocated)

## Reusing existing code

- The local-JWT login path in `RegistrationService` and `/api/auth/login`
  (added in TASK-12) already issues app-signed JWTs without Keycloak — this is
  the foundation that makes "no Keycloak in IAM-on mode is acceptable" hold. In
  the **off** mode we go further: skip auth entirely.
- The nginx reverse-proxy config (TASK-13) needs no changes — `/oauth2/*`
  routes simply 404 when IAM is off, which is acceptable since the frontend
  won't call them.

## Deliverable for this turn

Per the user's actual ask: **create one backlog task** capturing this plan.
No code changes yet — implementation is a separate session.

## Verification (when implemented)

1. `IAM_ENABLED=true make dockerAll` → Keycloak comes up, login at `/login`
   works against Keycloak, e2e tests pass.
2. `make dockerAllNoIam` → no Keycloak container, frontend lands directly on
   protected pages, `GET /api/config` returns `{"iamEnabled":false}`.
3. `skaffold dev` → 5 deployments running (postgres, app, frontend, keycloak,
   …); `kubectl get pods` shows keycloak.
4. `skaffold dev -p no-iam` → no keycloak pod, app `env` shows `IAM_ENABLED=false`.
5. Backend slice test for open `SecurityFilterChain` passes.
6. `make clean build` passes.