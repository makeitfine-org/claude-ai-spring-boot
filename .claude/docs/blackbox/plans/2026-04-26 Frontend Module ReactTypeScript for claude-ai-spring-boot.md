# Frontend Module — React/TypeScript for claude-ai-spring-boot

## Context
The project is a Spring Boot 3.4.1 Person Management REST API with JWT auth, located at project root `/src`. An empty `/frontend` directory exists and is ready. The goal is to build a full-featured React/TypeScript UI that consumes the existing backend and is integrated into the project's Docker, CI, and Kubernetes infrastructure.

**Backend base URL:** `http://localhost:8080`  
**API prefix:** `/api`  
**Auth:** JWT Bearer — access token (15 min) + refresh token (7 days)

---

## Tech Stack

| Concern | Library |
|---|---|
| Build | Vite 6 |
| Language | TypeScript 5 |
| UI framework | React 19 |
| Routing | React Router v7 |
| Server state | TanStack Query v5 |
| Auth state | Redux Toolkit v2 |
| Styling | Tailwind CSS v4 + shadcn/ui |
| Forms | React Hook Form v7 + Zod v3 |
| HTTP | Axios v1 |
| Unit/component tests | Vitest + React Testing Library + MSW v2 |
| E2E tests | Playwright v1 |

---

## Directory Layout

```
frontend/
├── src/
│   ├── api/
│   │   ├── axiosClient.ts       # Axios instance, JWT interceptors, auto-refresh
│   │   └── endpoints.ts         # Typed API functions for all routes
│   ├── types/
│   │   └── api.ts               # AuthRequest, AuthResponse, PersonRequest, PersonResponse, Page<T>
│   ├── store/
│   │   ├── store.ts             # Redux store
│   │   └── authSlice.ts         # accessToken, refreshToken, isAuthenticated
│   ├── hooks/
│   │   └── usePersons.ts        # TanStack Query hooks (usePersons, usePerson, useMutatePerson, …)
│   ├── components/
│   │   ├── ProtectedRoute.tsx   # Redirects to /login if not authenticated
│   │   ├── Layout.tsx           # App shell with nav
│   │   ├── PersonForm.tsx       # Create / edit form (React Hook Form + Zod)
│   │   ├── PersonTable.tsx      # Paginated data table (shadcn/ui Table)
│   │   └── ErrorBoundary.tsx
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── PersonsPage.tsx      # List + search
│   │   ├── PersonDetailPage.tsx
│   │   └── NotFoundPage.tsx
│   ├── utils/
│   │   └── tokenStorage.ts      # localStorage helpers (get/set/clear tokens)
│   └── main.tsx                 # Vite entry — mounts React, Router, QueryClient, Redux
├── tests/
│   ├── unit/                    # Vitest unit tests
│   │   ├── authSlice.test.ts
│   │   └── tokenStorage.test.ts
│   ├── components/              # RTL component tests
│   │   ├── LoginPage.test.tsx
│   │   ├── PersonForm.test.tsx
│   │   └── PersonTable.test.tsx
│   ├── hooks/
│   │   └── usePersons.test.tsx  # TanStack Query hooks via MSW
│   ├── mocks/
│   │   ├── handlers.ts          # MSW request handlers matching all /api/* routes
│   │   └── server.ts            # MSW server setup
│   └── e2e/
│       ├── auth.spec.ts         # Login, logout, token refresh flow
│       └── persons.spec.ts      # CRUD flows, pagination, search
├── package.json
├── vite.config.ts
├── vitest.config.ts
├── playwright.config.ts
├── tsconfig.json
└── tailwind.config.ts
```

---

## Implementation Steps

### 1 — Scaffold project
```
cd frontend
npm create vite@latest . -- --template react-ts
npm install
```

### 2 — Install dependencies
```bash
# Core
npm i react-router-dom @tanstack/react-query @reduxjs/toolkit react-redux axios
# Forms + validation
npm i react-hook-form @hookform/resolvers zod
# UI
npm i -D tailwindcss @tailwindcss/vite
npx shadcn@latest init   # pick New York style, zinc base, CSS vars
npx shadcn@latest add table button input form card badge dialog alert
# Testing
npm i -D vitest @vitest/coverage-v8 @testing-library/react @testing-library/user-event \
        @testing-library/jest-dom msw @playwright/test
```

### 3 — TypeScript types (`src/types/api.ts`)
Mirror all backend DTOs exactly:
- `AuthRequest`, `AuthResponse`
- `PersonRequest`, `PersonResponse`
- `Page<T>` (Spring pageable shape)
- `ProblemDetail` (RFC 7807 error shape)

### 4 — Axios client (`src/api/axiosClient.ts`)
- Base URL from `VITE_API_URL` env var (default `http://localhost:8080`)
- Request interceptor: attach `Authorization: Bearer <accessToken>`
- Response interceptor: on 401, call `/api/auth/refresh` once, retry original; if refresh fails → clear tokens → redirect to `/login`

### 5 — Redux auth slice (`src/store/authSlice.ts`)
State: `{ accessToken, refreshToken, isAuthenticated }`  
Actions: `setCredentials(AuthResponse)`, `clearCredentials()`  
Persistence: sync to `localStorage` via middleware

### 6 — TanStack Query hooks (`src/hooks/usePersons.ts`)
- `usePersons(page, size, sort)` — paginated list
- `usePerson(id)` — single person
- `useSearchPerson(email)` — by email
- `useCreatePerson()`, `useUpdatePerson()`, `useDeletePerson()` — mutations with query invalidation

### 7 — Pages
- **LoginPage** — email/password form, dispatches `setCredentials` on success
- **PersonsPage** — table with URL-synced pagination (`?page=0&size=20`), email search bar, Add button
- **PersonDetailPage** — shows `PersonResponse` fields, Edit/Delete actions
- **PersonForm** (modal) — used for both create and edit, validates with Zod schema matching backend constraints

### 8 — Routing (`src/main.tsx`)
```
/login              → LoginPage (public)
/                   → redirect to /persons
/persons            → PersonsPage (protected)
/persons/:id        → PersonDetailPage (protected)
*                   → NotFoundPage
```
`ProtectedRoute` checks `isAuthenticated` from Redux store.

### 9 — Vitest unit + component tests
- `authSlice.test.ts` — set/clear credentials, localStorage sync
- `LoginPage.test.tsx` — renders, submits, shows errors (MSW handler returns 401/200)
- `PersonForm.test.tsx` — validation messages, submit triggers correct mutation
- `PersonTable.test.tsx` — renders rows, pagination controls, delete confirm dialog
- `usePersons.test.tsx` — hook data-fetching with MSW happy/error paths

MSW `handlers.ts` mocks all backend endpoints with realistic fixture data.

### 10 — Playwright e2e tests
- `auth.spec.ts`: successful login, failed login error message, logout clears session, token refresh transparent to user
- `persons.spec.ts`: create person → appears in list; edit person → changes persist; delete person → removed from list; pagination; email search

Playwright config: `baseURL: http://localhost:5173`, starts dev server via `webServer`.

### 11 — Docker integration
Add `frontend/Dockerfile`:
- Stage 1: `node:22-alpine` — `npm ci && npm run build`
- Stage 2: `nginx:alpine` — serve `/dist`, proxy `/api` to backend service

Add `frontend` service to `docker-compose.yml`:
```yaml
frontend:
  build: ./frontend
  ports: ["5173:80"]
  environment:
    - VITE_API_URL=http://app:8080
  depends_on: [app]
```

### 12 — CI/CD update (`.github/workflows/ci.yml`)
Add `frontend-ci` job:
- `npm ci` → `npm run build` → `npm run test` (Vitest) → `npx playwright install` → `npx playwright test`

### 13 — Kubernetes (optional stretch)
Add `k8s/frontend-deployment.yaml` + `k8s/frontend-service.yaml` mirroring backend pattern.

---

## Critical Files

| File | Action |
|---|---|
| `frontend/` | Create entirely new |
| `docker-compose.yml` | Add `frontend` service |
| `.github/workflows/ci.yml` | Add `frontend-ci` job |
| `k8s/frontend-*.yaml` | New (stretch) |

---

## Verification

1. `cd frontend && npm run dev` → `http://localhost:5173` loads login page
2. Login with `test@example.com / password` → redirected to persons list
3. Create / edit / delete a person → backend persists changes (requires `docker compose up`)
4. `npm run test` → all Vitest tests pass
5. `npx playwright test` → all e2e specs pass
6. `docker compose up --build` → frontend served at `http://localhost:5173`, API proxied to backend

## Mutations Log
| Date | Type | Task | Reason |
|---|---|---|---|
| — | — | — | — |