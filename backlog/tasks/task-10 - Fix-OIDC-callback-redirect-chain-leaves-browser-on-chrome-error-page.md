---
id: TASK-10
title: 'Fix: OIDC callback redirect chain leaves browser on chrome-error page'
status: Done
assignee: []
created_date: '2026-05-01 15:06'
updated_date: '2026-05-01 16:45'
labels:
  - bug
  - auth
  - oidc
  - keycloak
dependencies:
  - TASK-6.12
priority: high
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
## Problem

End-to-end scenario "Successful registration and login" (TASK-6.12) fails after the user clicks Sign in. Diagnostic logging captured the final URL as `chrome-error://chromewebdata/`, meaning the browser hit a navigation/connection error somewhere in the OIDC callback chain.

## Expected flow

1. SPA → `http://localhost:3000/oauth2/authorization/keycloak` (proxied to backend)
2. Backend → Keycloak `realms/claude-ai/protocol/openid-connect/auth?...&redirect_uri=http://localhost:3000/login/oauth2/code/keycloak`
3. Keycloak (auto-completes if already authenticated via email-verification step) → redirect to `http://localhost:3000/login/oauth2/code/keycloak?code=...`
4. Backend handles callback, creates session, redirects to `/persons` on the SPA.

## Evidence

- `OIDC_REDIRECT_BASE_URL=http://localhost:3000` is set in docker-compose.
- Keycloak realm export lists both `localhost:3000/*` and `localhost:8080/*` as valid redirect URIs.
- The MailHog email-verification step preceding the failure passes (Keycloak session is established).
- The browser still ends on `chrome-error://chromewebdata/` — suggests a step in the redirect chain hits an unreachable host or the nginx proxy lacks a route.

## Suggested investigation

- Inspect `frontend/nginx.conf` for a `/oauth2/` and `/login/oauth2/code/` proxy to backend.
- Trace the chain manually with `curl -L -v` or Playwright trace to see which hop fails.
- Confirm Spring's success URL is the SPA root, not a backend-only path.

## Acceptance Criteria
<!-- AC:BEGIN -->
- "Successful registration and login" scenario passes end-to-end.
- `@wip` tag removed from that scenario in `e2e/features/auth/user-auth.feature`.
- `make clean build` passes.
<!-- SECTION:DESCRIPTION:END -->

- [x] #1 After registration + email verification + Sign in, the browser lands on /persons in the SPA with the user's display name in the nav.
- [x] #2 @wip tag removed from the 'Successful registration and login' scenario in e2e/features/auth/user-auth.feature.
- [x] #3 `make clean build` passes.
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Four cumulative defects combined to break the OIDC sign-in flow; fixing one only exposed the next.

1. **Spring OAuth2 client did not send PKCE.** Keycloak realm requires `pkce.code.challenge.method=S256` for `claude-ai-bff`, so the callback returned `error=invalid_request&error_description=Missing+parameter%3A+code_challenge_method`. Fix: register a `DefaultOAuth2AuthorizationRequestResolver` with `OAuth2AuthorizationRequestCustomizers.withPkce()` in `SecurityConfig.oauth2Login(...)`.

2. **Action-token issuer mismatch on email verification.** Backend triggered `send-verify-email` via `http://keycloak:8180`, so action tokens were signed with `iss=http://keycloak:8180/...`. The browser clicked the rewritten link at `localhost:8180`, where Keycloak rejected with `Invalid token issuer. Expected 'http://localhost:8180/realms/claude-ai'`. Fix: pin `KC_HOSTNAME=http://localhost:8180` so all token issuer claims and email links use the canonical browser-facing URL. Backchannel calls keep working via `KC_HOSTNAME_STRICT_BACKCHANNEL=false` (default).

3. **Keycloak required UPDATE_PROFILE because lastName was empty.** Registration only collects `displayName`; backend sent `firstName=displayName`, `lastName=null` → Keycloak treated lastName as missing and inserted UPDATE_PROFILE into the auth flow. Fix: mirror `displayName` into both `firstName` and `lastName` in `RegistrationService`.

4. **Tomcat built malformed redirect URLs (port stripped).** With `forward-headers-strategy=native`, RemoteIpValve respects only `X-Forwarded-Port` (not the port in `X-Forwarded-Host`). nginx wasn't sending it, so Spring emitted `Location: http://localhost/...` (port 80) and the browser landed on `chrome-error://chromewebdata/`. Fix: add `proxy_set_header X-Forwarded-Port 3000` to all proxied locations in `frontend/nginx.conf`.

Bonus e2e fix: `extractVerificationLink` couldn't parse Keycloak's MIME-quoted-printable plain-text body (URL wrapped with `=\n` soft breaks and `=` encoded as `=3D`). Added a small QP decoder before regex matching.

Files touched:
- `backend/src/main/java/pl/piomin/services/config/SecurityConfig.java` — PKCE resolver
- `backend/src/main/java/pl/piomin/services/application/service/RegistrationService.java` — populate lastName
- `backend/src/test/java/pl/piomin/services/application/service/RegistrationServiceTest.java` — assertion update
- `docker-compose.yml` — `KC_HOSTNAME`
- `frontend/nginx.conf` — `X-Forwarded-Port`
- `e2e/src/support/mailhog-client.ts` — quoted-printable decoder
- `e2e/features/auth/user-auth.feature` — drop `@wip`
<!-- SECTION:NOTES:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
## Summary
Restores end-to-end OIDC sign-in by fixing four chained bugs: enables PKCE on the Spring OAuth2 client, pins Keycloak's canonical hostname so action tokens validate, populates `lastName` so Keycloak doesn't trigger UPDATE_PROFILE, and forwards `X-Forwarded-Port` from nginx so Tomcat builds correct absolute redirect URLs. Also makes the e2e MailHog helper decode quoted-printable email bodies. All 17 e2e scenarios pass and `make clean build` succeeds.
<!-- SECTION:FINAL_SUMMARY:END -->
