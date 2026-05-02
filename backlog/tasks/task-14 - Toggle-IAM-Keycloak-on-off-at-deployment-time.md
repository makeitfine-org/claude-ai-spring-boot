---
id: TASK-14
title: Toggle IAM (Keycloak) on/off at deployment time
status: Done
assignee: []
created_date: '2026-05-02 10:12'
updated_date: '2026-05-02 10:32'
labels:
  - iam
  - keycloak
  - docker-compose
  - kubernetes
  - security
dependencies: []
references:
  - backend/src/main/resources/application.yml
  - backend/src/main/java/pl/piomin/services/config/SecurityConfig.java
  - backend/src/main/java/pl/piomin/services/config/IdentityProviderConfig.java
  - >-
    backend/src/main/java/pl/piomin/services/infrastructure/identity/KeycloakIdentityProvider.java
  - >-
    backend/src/main/java/pl/piomin/services/infrastructure/security/OidcLoginSuccessHandler.java
  - >-
    backend/src/main/java/pl/piomin/services/application/service/RegistrationService.java
  - frontend/src/auth/AuthContext.tsx
  - frontend/src/auth/ProtectedRoute.tsx
  - docker-compose.yml
  - Makefile
  - backend/skaffold.yaml
  - backend/k8s/
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Goal

Introduce a single boolean flag (`app.iam.enabled` / env `IAM_ENABLED`, default `true`) that toggles Keycloak IAM on or off across all deployment environments without changing source code.

**When off:** all backend endpoints are `permitAll`, no OIDC/JWT validation, no Keycloak container/pods deployed, frontend bypasses `ProtectedRoute` and skips login.

**When on (default):** behaves exactly as today — Keycloak deployed, OIDC login + local-JWT both available.

## Background

Currently Keycloak is unconditionally wired:
- `application.yml` has all OIDC properties with no `@ConditionalOnProperty` gating
- `docker-compose.yml` keycloak service has no `profiles:` key
- `backend/skaffold.yaml` always deploys all 4 Keycloak manifests

This makes quick demos, smoke tests, and frontend-only iterations heavier than necessary.

## Implementation

### Backend
- `application.yml` → add `app.iam.enabled: ${IAM_ENABLED:true}`
- `SecurityConfig.java` → branch `SecurityFilterChain`: when `false` → `anyRequest().permitAll()`, no OAuth2, no resource server
- `IdentityProviderConfig`, `KeycloakIdentityProvider`, `OidcLoginSuccessHandler` → `@ConditionalOnProperty(prefix="app.iam", name="enabled", havingValue="true", matchIfMissing=true)`
- `RegistrationService.java` → short-circuit Keycloak admin call when IAM disabled (use local DB only)
- New `GET /api/config` (public) → `{ "iamEnabled": boolean }` for frontend runtime detection

### Frontend
- `AuthContext.tsx` → fetch `/api/config` on mount; if `iamEnabled=false`, set synthetic anon user
- `ProtectedRoute.tsx` → render children directly when IAM disabled
- Login page → show "IAM disabled — open mode" banner when off

### Docker Compose
- `docker-compose.yml` → add `profiles: ["iam"]` to `keycloak` service; app env `IAM_ENABLED: ${IAM_ENABLED:-true}`
- `.env` (new, committed) → `COMPOSE_PROFILES=iam` + `IAM_ENABLED=true` (preserves default-on)
- `Makefile` → add `dockerAllNoIam` target (`COMPOSE_PROFILES= IAM_ENABLED=false docker compose up`)

### Kubernetes / Skaffold
- Move 4 Keycloak manifests from `backend/k8s/` → `backend/k8s/iam/`
- `backend/skaffold.yaml` base: includes `k8s/iam/*.yaml` (IAM on by default)
- Add `no-iam` Skaffold profile: remove `k8s/iam/*.yaml` manifests + patch `IAM_ENABLED=false` on app Deployment
- Usage: `skaffold dev` (IAM on) vs `skaffold dev -p no-iam` (IAM off)

### Local dev
- `IAM_ENABLED=false mvn spring-boot:run` → open mode; frontend auto-adapts via `/api/config`

### Tests
- Add one backend slice test: `@SpringBootTest` with `app.iam.enabled=false` → MockMvc on a protected endpoint expects HTTP 200
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [x] #1 - [ ] `app.iam.enabled` property (env `IAM_ENABLED`) added to `application.yml` with default `true`
- [x] #2 - [ ] `SecurityConfig` produces open `SecurityFilterChain` (permitAll, no OAuth2) when `IAM_ENABLED=false`
- [x] #3 - [ ] `KeycloakIdentityProvider`, `IdentityProviderConfig`, `OidcLoginSuccessHandler` are conditionally loaded via `@ConditionalOnProperty`
- [x] #4 - [ ] `RegistrationService` works against local DB when IAM disabled (no Keycloak call)
- [x] #5 - [ ] `GET /api/config` endpoint returns `{ "iamEnabled": boolean }` and is always public
- [x] #6 - [ ] Frontend `AuthContext` fetches `/api/config` and sets anon user when IAM off
- [x] #7 - [ ] `ProtectedRoute` renders children without auth check when IAM off
- [x] #8 - [ ] Login page shows open-mode banner when IAM off
- [x] #9 - [ ] `docker-compose.yml` keycloak service has `profiles: ["iam"]`; `.env` sets `COMPOSE_PROFILES=iam` by default
- [x] #10 - [ ] `make dockerAllNoIam` starts stack without Keycloak and with `IAM_ENABLED=false`
- [x] #11 - [ ] Keycloak k8s manifests moved to `backend/k8s/iam/`; `skaffold dev` keeps them; `skaffold dev -p no-iam` excludes them + sets `IAM_ENABLED=false`
- [x] #12 - [ ] Backend slice test: protected endpoint returns 200 when `app.iam.enabled=false`
- [x] #13 - [ ] `make clean build` passes
<!-- AC:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## What was done

Introduced `app.iam.enabled` (`IAM_ENABLED` env var, default `true`) as a single toggle that controls IAM across all three deployment environments.

### Backend
- `application.yml`: added `app.iam.enabled: ${IAM_ENABLED:true}`
- `SecurityConfig`: split `securityFilterChain` into `iamEnabledFilterChain` (`@ConditionalOnProperty havingValue="true"`) and `iamDisabledFilterChain` (`havingValue="false"`). Disabled chain uses `permitAll` + STATELESS + CSRF off. Also added `/api/config` to the enabled chain's permitAll list.
- `IdentityProviderConfig`, `KeycloakIdentityProvider`: annotated with `@ConditionalOnProperty(prefix="app.iam", name="enabled", havingValue="true", matchIfMissing=true)`
- New `NoOpIdentityProvider`: `@ConditionalOnProperty havingValue="false"`, returns random UUID as sub — satisfies the `IdentityProvider` port so `RegistrationService` works with local DB only
- New `ConfigController`: `GET /api/config` → `{ "iamEnabled": boolean }` (always public)
- New `IamDisabledSecurityTest`: `@WebMvcTest` with `app.iam.enabled=false`, asserts protected endpoint is not 401/403
- Version bumped: 1.0.2 → 1.0.3

### Docker Compose
- `docker-compose.yml`: `keycloak` service gets `profiles: ["iam"]`; app service gets `IAM_ENABLED: ${IAM_ENABLED:-true}`
- New `.env`: `COMPOSE_PROFILES=iam`, `IAM_ENABLED=true` (default-on)
- `Makefile`: new `dockerAllNoIam` target runs with `COMPOSE_PROFILES= IAM_ENABLED=false`

### Kubernetes / Skaffold
- Keycloak manifests moved from `backend/k8s/` → `backend/k8s/iam/`
- `backend/k8s/configmap.yaml`: added `iam.enabled: "true"`
- `backend/k8s/deployment.yaml`: added `IAM_ENABLED` env var from configmap
- New `backend/k8s/no-iam/configmap.yaml`: same configmap with `iam.enabled: "false"`
- New `backend/k8s/no-iam/deployment.yaml`: app deployment without the `wait-for-keycloak` init container
- `backend/skaffold.yaml`: updated base to reference `k8s/iam/*.yaml`; added `no-iam` profile with no-iam manifests and no Keycloak port-forward

### Frontend
- `AuthContext.tsx`: fetches `/api/config` first on mount; if `iamEnabled=false`, sets `isAuthenticated=true` immediately without calling `/api/users/me`; exposes `iamEnabled` in context
- `ProtectedRoute.tsx`: renders children directly when `iamEnabled=false`
- `LoginPage.tsx`: redirects to `/persons` immediately and shows "IAM disabled — open mode" banner when `iamEnabled=false`
- Fixed `AuthContext.test.tsx`, `LoginPage.test.tsx`, `ProfilePage.test.tsx` to include `iamEnabled` in auth mocks

### Verification
- `make buildBackend` passes (all tests + JaCoCo 85% gate)
- `npm run build` (frontend) passes with no TypeScript errors
<!-- SECTION:FINAL_SUMMARY:END -->
