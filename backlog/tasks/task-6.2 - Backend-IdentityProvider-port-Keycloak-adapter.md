---
id: TASK-6.2
title: 'Backend: IdentityProvider port + Keycloak adapter'
status: To Do
assignee: []
created_date: '2026-04-30 16:39'
labels:
  - backend
  - keycloak
  - oidc
  - architecture
dependencies: []
parent_task_id: TASK-6
priority: high
ordinal: 2000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Define the `IdentityProvider` port (Spring interface) that abstracts all IdP operations, and implement the `KeycloakIdentityProvider` adapter. No application code outside this adapter may use Keycloak-specific APIs.

## Interface

```java
public interface IdentityProvider {
    String createUser(CreateUserCommand command);   // returns IdP sub
    void deleteUser(String sub);
    void triggerEmailVerification(String sub);
}
```

## Keycloak Adapter

- Calls Keycloak Admin REST API (using `keycloak-admin-client` or plain `RestClient`) — confined to this adapter class.
- Reads `OIDC_ISSUER_URI`, `KEYCLOAK_ADMIN_CLIENT_ID`, `KEYCLOAK_ADMIN_CLIENT_SECRET` from env/config.
- Handles error mapping: duplicate username/email → typed exceptions consumed by the registration service.

## Spring Security OIDC Config

- `application.yaml` wired via `OIDC_ISSUER_URI`, `OIDC_CLIENT_ID`, `OIDC_CLIENT_SECRET` (no `keycloak.*` properties).
- Resource server validates JWTs via JWKS endpoint discovered from issuer URI.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 IdentityProvider interface exists with createUser, deleteUser, triggerEmailVerification methods.
- [ ] #2 KeycloakIdentityProvider implements the interface; no Keycloak-specific imports exist outside this class.
- [ ] #3 Spring context loads with OIDC_ISSUER_URI / OIDC_CLIENT_ID / OIDC_CLIENT_SECRET env vars; no keycloak.* properties.
- [ ] #4 Duplicate username passed to createUser throws a typed DuplicateUsernameException (not a raw HTTP error).
- [ ] #5 Swapping the adapter (replacing KeycloakIdentityProvider with another impl) requires no changes to any service or controller.
<!-- AC:END -->
