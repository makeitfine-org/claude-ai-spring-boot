# Improve root CLAUDE.md for e2e module

## Context

The `e2e/` Cucumber+Playwright acceptance-test module was added in a prior session (all 11
scenarios now pass). The root `CLAUDE.md` still has no mention of the module — it's missing
from the layout map, the agent guide, and the delivery checklist. Two non-obvious bugs were
also discovered and fixed during e2e bring-up that are worth capturing as permanent rules.

## Changes

### 1. Monorepo Layout section
Add `e2e/` entry so the map is complete:
```
e2e/        → Cucumber + Playwright acceptance tests (see e2e/CLAUDE.md)
```

### 2. Agent Selection table
Add a row for e2e work:
```
| New/fix Cucumber+Playwright e2e scenario | `test-automator` |
```

### 3. Delivery Checklist
Add a 4th item:
```
4. If stack-level changes were made (Dockerfile, docker-compose, env vars), re-run `cd e2e && npm test` to confirm all scenarios still pass
```

### 4. New "E2E Gotchas" section
Two hard-won lessons from e2e bring-up that would waste time to rediscover:

**Cucumber step timeout** — `timeout:` in `cucumber.js` profile config is silently ignored by
`@cucumber/cucumber` v11. The only effective mechanism is `setDefaultTimeout(ms)` called from
`e2e/src/hooks.ts` at module level.

**Axios 401 interceptor scope** — The refresh-and-redirect logic in `frontend/src/lib/api.ts`
must guard against auth endpoints with `!original.url?.startsWith('/api/auth/')`. Without this,
a failed login triggers a token-refresh attempt → "no refresh token" → `window.location.href =
'/login'` (full page reload), destroying React state before any error Alert can render.

## File to Change

| File | Change |
|---|---|
| `CLAUDE.md` (root) | All four changes above |