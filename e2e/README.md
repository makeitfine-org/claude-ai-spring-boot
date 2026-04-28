# e2e — Acceptance Test Suite

Black-box end-to-end acceptance tests for the full stack (backend + frontend + database).
Built with **Cucumber-JS** (BDD / Gherkin) + **Playwright** (browser automation) + **TypeScript**.

---

## Contents

- [Prerequisites](#prerequisites)
- [Setup](#setup)
- [Running the tests](#running-the-tests)
- [Test structure](#test-structure)
- [Writing new tests](#writing-new-tests)
- [Configuration](#configuration)
- [CI integration](#ci-integration)
- [Debugging failures](#debugging-failures)

---

## Prerequisites

| Tool | Minimum version | Notes |
|---|---|---|
| Node.js | 20 | Tested on 22 |
| npm | 10 | bundled with Node 20+ |
| Docker + Compose | v2 | required to start the full stack |

---

## Setup

```bash
# From the repo root — build the backend JAR (required before docker compose up)
mvn -f backend/pom.xml package -DskipTests -q

# Start the full stack
docker compose up -d --wait

# From the e2e/ directory:
cp .env.example .env          # defaults work for docker-compose
npm install
npx playwright install --with-deps chromium
```

> `.env` is gitignored. Never commit it — it may contain real credentials in shared environments.

---

## Running the tests

The full stack must be running before you execute the suite. Start it from the repo root:

```bash
docker compose up -d --wait   # starts postgres, backend, frontend with health-checks
```

Then run the tests:

```bash
# All scenarios
npm test

# By scope tag
npm run test:ui     # @ui   — browser-driven SPA scenarios
npm run test:api    # @api  — REST contract scenarios (no browser)
npm run test:auth   # @auth — login / JWT lifecycle scenarios
npm run test:db     # @db   — DB integrity assertions
```

After a run, open the HTML report:

```bash
# macOS
open reports/cucumber.html
# Linux
xdg-open reports/cucumber.html
```

### Automatic stack management

Set `E2E_MANAGE_STACK=1` in `.env` and the suite will run `docker compose up` and `docker compose down` itself:

```bash
E2E_MANAGE_STACK=1 npm test
```

Set `E2E_KEEP_STACK=1` as well if you want the stack to stay up after the run (useful locally).

---

## Test structure

```
e2e/
├── cucumber.js               # Cucumber runner config (paths, formatters, ts-node)
├── tsconfig.json
├── .env.example              # all config vars
├── features/                 # Gherkin feature files — one folder per scope
│   ├── auth/
│   │   ├── login.feature           @auth @ui
│   │   └── jwt-refresh.feature     @auth @api
│   ├── persons-ui/
│   │   ├── create-person.feature   @ui @db
│   │   ├── edit-person.feature     @ui @db
│   │   ├── delete-person.feature   @ui @db
│   │   └── list-search-sort.feature @ui
│   ├── persons-api/
│   │   ├── crud.feature            @api
│   │   └── pagination.feature      @api
│   └── data-integrity/
│       └── db-state.feature        @db @api
└── src/
    ├── world.ts              # CustomWorld — shared state per scenario
    ├── hooks.ts              # Before/After/BeforeAll/AfterAll lifecycle
    ├── steps/                # Step definitions (one file per feature scope)
    │   ├── auth.steps.ts
    │   ├── persons-ui.steps.ts
    │   ├── persons-api.steps.ts
    │   └── db.steps.ts
    └── support/              # Utilities (not step definitions)
        ├── api-client.ts     # Axios wrapper + JWT helpers
        ├── db-client.ts      # pg client — query / cleanup helpers
        ├── seed.ts           # test-data lifecycle
        └── stack.ts          # docker compose up/down/wait-for-health
```

### CustomWorld (`src/world.ts`)

The `CustomWorld` instance is fresh per scenario and carries:

| Field | Type | Description |
|---|---|---|
| `page` | `Page` | Playwright page — opened in `Before`, closed in `After` |
| `browser` | `Browser` | Chromium instance |
| `apiClient` | Axios instance | Configured with `API_BASE_URL` |
| `accessToken` | `string` | Stored after login steps |
| `refreshToken` | `string` | Stored after login steps |
| `lastResponse` | `any` | Last Axios response (for API assertion steps) |
| `lastPersonId` | `number` | Person ID returned by last create-via-API step |
| `dbClient` | `DbClient` | Connected pg pool for DB assertions |

### Tags

| Tag | Meaning |
|---|---|
| `@ui` | Requires a running browser (Playwright) |
| `@api` | Pure HTTP — no browser needed |
| `@auth` | Exercises login / JWT lifecycle |
| `@db` | Asserts database state via direct SQL |

---

## Writing new tests

### 1. Add a feature file

Create a `.feature` file in the appropriate `features/` subfolder.
Tag it with `@ui`, `@api`, `@auth`, and/or `@db`.

```gherkin
@ui @db
Feature: My new feature
  Scenario: Something happens end-to-end
    Given I am logged in as "test@example.com"
    When I open the persons page
    Then the persons list shows at least one row
```

Use existing step phrases where possible — see the full list below.

### 2. Add step definitions (if needed)

If your scenario needs a step that doesn't exist yet, add it to the relevant file in `src/steps/`.

```typescript
// src/steps/persons-ui.steps.ts
When('I click the export button', async function (this: CustomWorld) {
  await this.page.getByRole('button', { name: /export/i }).click()
})
```

- Access the Playwright page via `this.page`.
- Access the Axios client via `this.apiClient` and the token via `this.accessToken`.
- Access the DB via `this.dbClient.queryPersonByEmail(email)`.
- Store cross-step state on `this` (e.g. `this.lastPersonId`).

### 3. Isolate test data

- Name test persons with email pattern `e2e-test-*@example.com` or `firstName = 'E2ETest'`.
- The `AfterAll` hook calls `cleanupE2EPersons()` to delete these rows automatically.
- For scenarios that need a specific pre-existing row, create it in a `Before` step and delete it in `After`.

### 4. Existing step reference

**Auth**
```
Given I am logged in as {string}
Given I am on the login page
When I enter email {string} and password {string}
When I submit the login form
Then I should be redirected to the persons page
Then I should see an error message
Given I have a valid JWT token
When I call the refresh endpoint with my refresh token
Then I should receive a new access token
```

**Persons UI**
```
When I open the persons page
When I click the add person button
When I fill in the person form with first name {string} and last name {string} and email {string}
When I submit the person form
Then the persons list shows {string}
Then the persons list does not show {string}
Then the persons list shows at least one row
When I search for {string}
When I click edit for {string}
When I update the last name to {string}
When I click delete for {string}
When I confirm the deletion
```

**Persons API**
```
Given I have a valid auth token
When I create a person via API with first name {string} last name {string} email {string}
Then the API response status is {int}
Then the API response contains first name {string}
When I retrieve the person by id
When I update the person last name to {string} via API
When I delete the person via API
When I list persons via API
Then the API response contains at least {int} persons
```

**Database**
```
Then the database has a person with email {string}
Then the database does not have a person with email {string}
Then the database person with email {string} has last name {string}
```

---

## Configuration

All values are read from environment variables (loaded via `dotenv` from `.env`).

| Variable | Default | Description |
|---|---|---|
| `FRONTEND_URL` | `http://localhost:3000` | SPA base URL for Playwright |
| `API_BASE_URL` | `http://localhost:8080` | Backend base URL for Axios |
| `DB_HOST` | `localhost` | Postgres host |
| `DB_PORT` | `5432` | Postgres port |
| `DB_NAME` | `persondb` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `TEST_USER_EMAIL` | `test@example.com` | Login credentials used by auth steps |
| `TEST_USER_PASSWORD` | `password` | Login credentials used by auth steps |
| `E2E_MANAGE_STACK` | `0` | Set to `1` to let the suite start/stop docker compose |
| `E2E_KEEP_STACK` | `0` | Set to `1` to leave the stack running after the suite |

---

## CI integration

The suite runs as an `e2e` job in `.github/workflows/ci.yml` after `build-and-test`.

```
build-and-test  →  e2e
```

The job:
1. Starts the full stack with `docker compose up -d --wait`.
2. Runs `npm test` (all scenarios, `E2E_MANAGE_STACK=0`).
3. Uploads the Cucumber HTML report as an artifact (`cucumber-report`) — always.
4. Uploads Playwright failure screenshots to `playwright-traces` — on failure only.

To download the report after a run:

> GitHub → your repo → Actions → select a workflow run → Artifacts → `cucumber-report`

---

## Debugging failures

### HTML report

```bash
open reports/cucumber.html
```

Shows each scenario with step-level pass/fail and embedded failure screenshots.

### Playwright headed mode

Run a single scenario in a visible browser:

```bash
PWDEBUG=1 npx cucumber-js --name "Authenticated user creates a person"
```

### Run one feature file

```bash
npx cucumber-js features/persons-ui/create-person.feature
```

### Run one scenario by name

```bash
npx cucumber-js --name "Successful login with valid credentials"
```

### DB state inspection

```bash
psql -h localhost -U postgres -d persondb -c "SELECT id, first_name, last_name, email FROM persons ORDER BY created_at DESC LIMIT 10;"
```

### Common issues

| Symptom | Likely cause | Fix |
|---|---|---|
| `ECONNREFUSED localhost:8080` | Backend not running | `docker compose up -d --wait` from repo root |
| `net::ERR_CONNECTION_REFUSED` in browser | Frontend not running | Same as above |
| Scenario fails with `TimeoutError` | Slow startup in CI | Increase `waitForStack` timeout in `src/support/stack.ts` |
| DB assertion fails immediately | Async lag after write | The DB steps retry for up to 10 s — if still failing, check the API response |
| `undefined` step | New Gherkin phrase has no matching step | Add the step to `src/steps/` and re-run |