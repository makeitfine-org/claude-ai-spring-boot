---
id: TASK-13
title: Configure Keycloak for Kubernetes/Skaffold deployment
status: To Do
assignee: []
created_date: '2026-05-01 22:04'
labels:
  - kubernetes
  - skaffold
  - keycloak
  - auth
dependencies: []
priority: medium
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

The Kubernetes/Skaffold deployment does not include Keycloak, so authentication features that work in the `docker compose` environment fail when running on Minikube via Skaffold.

### Reproduction

```bash
eval $(minikube docker-env)
cd backend && skaffold dev
cd frontend && skaffold dev
```

Keycloak-backed login/OIDC functionality is broken because no Keycloak service/manifests exist in `k8s/`, and backend/frontend env vars are not wired to a Keycloak issuer running in-cluster.

## Goal

Make Keycloak work in the Skaffold/Minikube deployment with parity to the `docker compose` setup (same realm, clients, users, redirect URIs, and login/logout flows succeed end-to-end).

## Scope

- Add Keycloak Kubernetes manifests (Deployment, Service, ConfigMap/Secret, optional Ingress) under `k8s/` (or backend/k8s as appropriate)
- Provide realm import (mirroring whatever the docker-compose Keycloak uses)
- Wire backend `application.yml` / env vars (issuer-uri, client-id, client-secret) to the in-cluster Keycloak
- Wire frontend env (OIDC issuer / redirect URIs) to in-cluster Keycloak via Minikube hostname/Ingress
- Update Skaffold config(s) to deploy Keycloak alongside backend/frontend
- Document the Minikube login flow (host entry / `minikube ip` / Ingress) in README or k8s docs
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 Keycloak runs in the Minikube cluster when `skaffold dev` is started for backend and frontend
- [ ] #2 Realm, clients, and test users mirror the docker-compose Keycloak configuration
- [ ] #3 Backend OIDC config (issuer-uri, client-id/secret) resolves the in-cluster Keycloak service correctly
- [ ] #4 Frontend redirects to Keycloak login and back successfully via Minikube hostname/Ingress
- [ ] #5 Login and logout flows work end-to-end in the Skaffold/Minikube environment
- [ ] #6 README or k8s docs explain any /etc/hosts or Ingress setup required for Minikube login
- [ ] #7 `make clean build` passes
<!-- AC:END -->
