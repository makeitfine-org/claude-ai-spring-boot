# e2e Module

Cucumber (BDD) + Playwright + TypeScript black-box acceptance tests.

## Running

1. Ensure the full stack is running: `docker compose up -d --wait` (from repo root)
2. Install: `npm install && npx playwright install --with-deps`
3. Run all: `npm test`
4. Run by tag: `npm run test:ui` / `npm run test:api` / `npm run test:auth` / `npm run test:db`

## Conventions
- Test-generated persons use email pattern `e2e-test-*@example.com` — cleaned up by AfterAll
- Steps are in `src/steps/`, support utilities in `src/support/`
- Reports land in `reports/` (gitignored)
- Playwright traces on failure go to `test-results/` (gitignored)
- Never put real credentials in code; use `.env` (copy `.env.example`)
