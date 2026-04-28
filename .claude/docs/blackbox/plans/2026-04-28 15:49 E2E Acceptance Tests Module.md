# E2E Acceptance Tests Module

## Context

The repo (`claude-ai-spring-boot`) currently has only backend integration tests
(`SecurityIntegrationTest`, `PersonIntegrationTest`) and frontend Vitest unit
tests. There is no full-stack acceptance suite that drives the SPA, exercises
the REST API, and verifies database state end-to-end.

We will introduce a new top-level **`e2e/`** module — a dedicated black-box
test project independent of `backend/` and `frontend/`. It will use **Cucumber
(BDD) + Playwright + TypeScript**, bring the whole stack up via the existing
`docker-compose.yml`, and cover four scopes: UI happy paths, REST contracts,
auth/security flows, DB integrity.

**Why a separate module (not inside backend/ or frontend/):**
- Tests are black-box — they don't belong to either side.
- Avoids polluting the SPA build (Playwright/Cucumber deps) or the Maven
  reactor.
- Independent CI job, independent dependency lifecycle.
- Matches project convention (each module already has its own `CLAUDE.md`).

## Target Layout

```
e2e/
├── package.json              # cucumber-js, @playwright/test, pg, ts-node
├── tsconfig.json
├── cucumber.js               # cucumber-js config (paths, formatters)
├── playwright.config.ts      # baseURL=http://localhost:3000, traces on failure
├── .env.example              # API_BASE_URL, FRONTEND_URL, DB_URL, TEST_USER
├── docker-compose.test.yml   # (optional) overlay pinning healthchecks for CI
├── README.md
├── CLAUDE.md                 # module rules per monorepo convention
├── features/
│   ├── auth/
│   │   ├── login.feature
│   │   └── jwt-refresh.feature
│   ├── persons-ui/
│   │   ├── create-person.feature
│   │   ├── list-search-sort.feature
│   │   ├── edit-person.feature
│   │   └── delete-person.feature
│   ├── persons-api/
│   │   ├── crud.feature
│   │   └── pagination.feature
│   └── data-integrity/
│       └── db-state.feature
└── src/
    ├── world.ts              # CustomWorld: page, apiClient, dbClient, token
    ├── hooks.ts              # Before/After: stack readiness, page, cleanup
    ├── steps/
    │   ├── auth.steps.ts
    │   ├── persons-ui.steps.ts
    │   ├── persons-api.steps.ts
    │   └── db.steps.ts
    └── support/
        ├── api-client.ts     # axios wrapper, JWT helpers, hits :8080/api
        ├── db-client.ts      # pg client → persondb (verify rows)
        ├── stack.ts          # docker compose up --wait + health polling
        └── seed.ts           # SQL fixtures / cleanup between scenarios
```

## Stack & Versions

- **`@cucumber/cucumber`** ^11 (BDD runner, Gherkin)
- **`playwright`** + **`@playwright/test`** ^1.50 (browser automation)
- **`typescript`** ^5.6, **`ts-node`** for step compilation
- **`pg`** ^8 (direct postgres queries for DB-integrity assertions)
- **`axios`** ^1 (REST contract checks; same client patterns as frontend)
- **`dotenv`** for env config

## How the Stack Boots

Tests assume the existing `docker-compose.yml` (postgres + app + frontend) is
the system under test. `src/support/stack.ts` will:

1. Run `docker compose up -d --wait` (relies on existing healthchecks:
   `pg_isready`, `/actuator/health`, frontend depends on app healthy).
2. Poll `http://localhost:3000` and `http://localhost:8080/actuator/health`
   until ready (timeout 90s).
3. On suite teardown: `docker compose down -v` (CI) or leave running (local
   dev — opt-in via `E2E_KEEP_STACK=1`).

A test user is seeded via a dedicated SQL fixture executed by `seed.ts`
against `persondb` before the suite — independent of Flyway production data.

## Scope Coverage (Initial Suite)

| Scope | Feature files | Mechanism |
|---|---|---|
| UI happy paths | `persons-ui/*.feature` | Playwright drives `http://localhost:3000` |
| REST contracts | `persons-api/*.feature`, `auth/*.feature` | Axios → `http://localhost:8080/api` |
| Auth/security | `auth/login.feature`, `auth/jwt-refresh.feature` | Login UI + JWT lifecycle (valid/invalid/expired/refresh) |
| DB integrity | `data-integrity/db-state.feature` + assertions in UI/API scenarios | `pg` client queries `persons` table after writes |

Sample Gherkin shape:

```gherkin
Feature: Create person via UI
  Scenario: Authenticated user creates a person and the row lands in DB
    Given I am logged in as "admin"
    When I open the persons page
    And I create a person with first name "Ada" and last name "Lovelace"
    Then the persons list shows "Ada Lovelace"
    And the database has a person with email "ada@example.com"
```

## Key Files to Create / Modify

**Create:**
- `e2e/` module (full layout above) — primary deliverable
- `e2e/CLAUDE.md` — module-specific rules (per monorepo convention)

**Modify:**
- `README.md` (root) — add "Running e2e tests" section + new module to layout
- `docker-compose.yml` — likely no change; reuse as-is. If healthchecks need
  tuning for CI, add an optional `e2e/docker-compose.test.yml` overlay
  instead of touching the root file.
- `.github/workflows/ci.yml` — add a new `e2e` job that runs after
  `build-and-test`: build images, `docker compose up --wait`, run
  `npm --prefix e2e run test`, upload Cucumber HTML report + Playwright
  traces as artifacts.
- `.gitignore` — add `e2e/node_modules/`, `e2e/test-results/`,
  `e2e/playwright-report/`, `e2e/reports/`.

## Reused Existing Assets

- `docker-compose.yml` services (postgres `persondb`, app `:8080`,
  frontend `:3000`) — no duplication.
- Spring Security endpoints already documented:
  `POST /api/auth/login`, `POST /api/auth/refresh`,
  `/api/persons/**` (CRUD + paginated search).
- Flyway seed data (V3 — 27 person rows) usable as read-only fixtures for
  list/search/sort/pagination scenarios; mutating scenarios use isolated
  test rows with cleanup.
- Existing healthchecks in `docker-compose.yml` for readiness gating.

## Verification

End-to-end check that the new module works:

1. **Local run**
   - `docker compose up -d --wait`
   - `cd e2e && npm install && npx playwright install --with-deps`
   - `npm test` → all features pass; HTML report at
     `e2e/reports/cucumber.html`; Playwright traces on any failure.
2. **Failure-path sanity**
   - Temporarily break a frontend selector → corresponding UI scenario fails
     with a Playwright trace artifact (proves the suite actually exercises
     the SPA, not just the API).
3. **DB-integrity sanity**
   - Run create-person scenario, then `psql -h localhost -U postgres -d
     persondb -c "select email from persons where email='ada@example.com'"`
     → row present (proves DB assertions are real, not mocked).
4. **CI**
   - Push branch; new `e2e` job in `.github/workflows/ci.yml` goes green
     and uploads Cucumber report + Playwright traces as artifacts.
5. **Coverage of declared scopes** — each of the four scopes
   (UI / REST / auth / DB) has at least one passing scenario in the initial
   suite.