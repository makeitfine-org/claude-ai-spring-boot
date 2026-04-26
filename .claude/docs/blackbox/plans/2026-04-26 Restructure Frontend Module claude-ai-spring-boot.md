# Restructure + Frontend Module — claude-ai-spring-boot

## Context
The project currently has the Spring Boot backend living directly at the repo root (`src/`, `pom.xml`, `Dockerfile`). The user wants a clean mono-repo layout with `backend/` and `frontend/` as sibling modules. The `backend/` skeleton already exists (empty dirs, no pom, no `.mvn`). The `frontend/` dir exists with empty placeholder directories.

This plan has **two phases**:
1. Move backend from root into `backend/`
2. Scaffold the React/TypeScript frontend in `frontend/`

---

## Phase 1 — Backend Restructure

### Target layout after restructure
```
claude-ai-spring-boot/          ← repo root
├── .git/
├── .gitignore
├── docker-compose.yml           ← updated
├── README.md                    ← updated
├── CLAUDE.md
├── .github/workflows/ci.yml    ← updated
├── backend/
│   ├── pom.xml                  ← moved from root
│   ├── Dockerfile               ← moved from root
│   ├── skaffold.yaml            ← moved from root
│   ├── .mvn/                    ← moved from root
│   ├── mvnw / mvnw.cmd          ← moved from root
│   ├── src/                     ← moved from root
│   └── k8s/                     ← moved from root
└── frontend/                    ← Phase 2
```

### Steps

**1.1 — Move Maven wrapper**
```bash
cp -r .mvn backend/.mvn
cp mvnw mvnw.cmd backend/
```

**1.2 — Move pom.xml**
```bash
cp pom.xml backend/pom.xml
```

**1.3 — Move all Java source**
```bash
cp -r src backend/src
```

**1.4 — Move Dockerfile**
```bash
cp Dockerfile backend/Dockerfile
```
Update `COPY` line inside to remain `target/claude-ai-spring-boot-1.0.1.jar app.jar` (path is relative to build context).

**1.5 — Move k8s manifests**
```bash
cp -r k8s/. backend/k8s/
```

**1.6 — Move skaffold.yaml**
```bash
cp skaffold.yaml backend/skaffold.yaml
```
Update `context:` and `dockerfile:` paths inside to be relative (already relative to `./backend` if run from there).

**1.7 — Update docker-compose.yml**
Change backend build context from `.` to `./backend`:
```yaml
app:
  build:
    context: ./backend
    dockerfile: Dockerfile
```

**1.8 — Update .github/workflows/ci.yml**
Change `mvn clean verify` steps to run from `backend/`:
```yaml
- run: mvn -f backend/pom.xml clean verify
# OR
- run: cd backend && mvn clean verify
```
Update JaCoCo report artifact path: `backend/target/site/jacoco/`
Update JAR artifact path: `backend/target/claude-ai-spring-boot-*.jar`

**1.9 — Update CLAUDE.md**
- Update any paths that reference root `src/` → `backend/src/`
- Update version-bump-procedure rule: Dockerfile is now at `backend/Dockerfile`

**1.10 — Delete old root files** (after verifying copies are good)
```bash
rm -rf src/ pom.xml Dockerfile skaffold.yaml .mvn mvnw mvnw.cmd
rm -rf k8s/   # root-level k8s (now in backend/k8s/)
```

**1.11 — Smoke test**
```bash
cd backend && mvn clean verify
docker compose build
```

---

## Phase 2 — Frontend Module

### Tech Stack

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

### Directory Layout

```
frontend/
├── src/
│   ├── api/
│   │   ├── axiosClient.ts       # Axios instance, JWT interceptors, auto-refresh
│   │   └── endpoints.ts         # Typed API functions for all routes
│   ├── types/
│   │   └── api.ts               # AuthRequest/Response, PersonRequest/Response, Page<T>, ProblemDetail
│   ├── store/
│   │   ├── store.ts
│   │   └── authSlice.ts         # accessToken, refreshToken, isAuthenticated
│   ├── hooks/
│   │   └── usePersons.ts        # TanStack Query hooks
│   ├── components/
│   │   ├── ProtectedRoute.tsx
│   │   ├── Layout.tsx
│   │   ├── PersonForm.tsx       # React Hook Form + Zod
│   │   ├── PersonTable.tsx      # shadcn/ui Table, paginated
│   │   └── ErrorBoundary.tsx
│   ├── pages/
│   │   ├── LoginPage.tsx
│   │   ├── PersonsPage.tsx
│   │   ├── PersonDetailPage.tsx
│   │   └── NotFoundPage.tsx
│   ├── utils/
│   │   └── tokenStorage.ts
│   └── main.tsx
├── tests/
│   ├── unit/
│   │   ├── authSlice.test.ts
│   │   └── tokenStorage.test.ts
│   ├── components/
│   │   ├── LoginPage.test.tsx
│   │   ├── PersonForm.test.tsx
│   │   └── PersonTable.test.tsx
│   ├── hooks/
│   │   └── usePersons.test.tsx
│   ├── mocks/
│   │   ├── handlers.ts          # MSW handlers for all /api/* routes
│   │   └── server.ts
│   └── e2e/
│       ├── auth.spec.ts         # Login, logout, token refresh
│       └── persons.spec.ts      # CRUD, pagination, search
├── package.json
├── vite.config.ts
├── vitest.config.ts
├── playwright.config.ts
├── tsconfig.json
└── tailwind.config.ts
```

### API Surface (from backend)

| Method | Path | Auth |
|---|---|---|
| POST | `/api/auth/login` | public |
| POST | `/api/auth/refresh` | Bearer refresh token |
| GET | `/api/persons?page&size&sort` | Bearer |
| POST | `/api/persons` | Bearer |
| GET | `/api/persons/{id}` | Bearer |
| PUT | `/api/persons/{id}` | Bearer |
| DELETE | `/api/persons/{id}` | Bearer |
| GET | `/api/persons/search?email=` | Bearer |

### Implementation Steps

**2.1 — Scaffold**
```bash
cd frontend
npm create vite@latest . -- --template react-ts
```

**2.2 — Install dependencies**
```bash
npm i react-router-dom @tanstack/react-query @reduxjs/toolkit react-redux axios
npm i react-hook-form @hookform/resolvers zod
npm i -D tailwindcss @tailwindcss/vite
npx shadcn@latest init
npx shadcn@latest add table button input form card badge dialog alert
npm i -D vitest @vitest/coverage-v8 @testing-library/react @testing-library/user-event @testing-library/jest-dom msw @playwright/test
```

**2.3 — TypeScript types** (`src/types/api.ts`)
Mirror all backend DTO fields exactly (PersonRequest, PersonResponse, AuthRequest, AuthResponse, Page<T>, ProblemDetail).

**2.4 — Axios client** (`src/api/axiosClient.ts`)
- Base URL from `VITE_API_URL` (default `http://localhost:8080`)
- Request interceptor: attach `Authorization: Bearer <accessToken>`
- Response interceptor: on 401, call `/api/auth/refresh` once, retry original; if refresh fails → clear tokens → redirect `/login`

**2.5 — Redux auth slice** (`src/store/authSlice.ts`)
State: `{ accessToken, refreshToken, isAuthenticated }`
Actions: `setCredentials`, `clearCredentials`
Persist to `localStorage` via middleware.

**2.6 — TanStack Query hooks** (`src/hooks/usePersons.ts`)
- `usePersons(page, size, sort)` — paginated list
- `usePerson(id)` — single person
- `useSearchPerson(email)`
- `useCreatePerson()`, `useUpdatePerson()`, `useDeletePerson()` — mutations + `queryClient.invalidateQueries`

**2.7 — Pages**
- **LoginPage**: email/password form → POST `/api/auth/login` → dispatch `setCredentials` → redirect `/persons`
- **PersonsPage**: table with URL-synced pagination, email search bar, Add Person button
- **PersonDetailPage**: show all fields, Edit (opens form), Delete (confirms then redirects)
- **PersonForm**: modal/dialog used for create and edit, Zod schema mirrors backend validation

**2.8 — Routing** (`src/main.tsx`)
```
/login         → LoginPage (public)
/              → redirect → /persons
/persons       → PersonsPage (protected)
/persons/:id   → PersonDetailPage (protected)
*              → NotFoundPage
```

**2.9 — Vitest tests**
- `authSlice.test.ts` — set/clear, localStorage sync
- `LoginPage.test.tsx` — renders, submits, MSW returns 401 → error shown, 200 → redirects
- `PersonForm.test.tsx` — validation messages (required fields, email format, max lengths), submit triggers mutation
- `PersonTable.test.tsx` — rows render, pagination controls work, delete opens confirm dialog
- `usePersons.test.tsx` — data fetching happy path + 404 error via MSW

**2.10 — Playwright e2e**
- `auth.spec.ts`: successful login, failed login shows error, logout clears session
- `persons.spec.ts`: create → appears in list; edit → changes persist; delete → removed; pagination; email search

**2.11 — Docker**
Add `frontend/Dockerfile`:
```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```
`nginx.conf`: proxy `/api` → `http://app:8080`, serve SPA with `try_files $uri /index.html`.

Add frontend service to `docker-compose.yml`:
```yaml
frontend:
  build: ./frontend
  ports: ["5173:80"]
  depends_on: [app]
```

**2.12 — CI update**
Add `frontend-ci` job to `.github/workflows/ci.yml`:
```yaml
- npm ci
- npm run build
- npm run test (Vitest)
- npx playwright install --with-deps
- npx playwright test
```

---

## Critical Files

| File | Action |
|---|---|
| `pom.xml` | Move to `backend/pom.xml` |
| `src/` | Move to `backend/src/` |
| `Dockerfile` | Move to `backend/Dockerfile` |
| `k8s/` | Move to `backend/k8s/` |
| `skaffold.yaml` | Move to `backend/skaffold.yaml` |
| `.mvn/`, `mvnw`, `mvnw.cmd` | Move to `backend/` |
| `docker-compose.yml` | Update backend build context |
| `.github/workflows/ci.yml` | Update Maven paths, add frontend job |
| `frontend/` | Scaffold entirely new |

---

## Verification

1. `cd backend && mvn clean verify` — all tests pass, JaCoCo ≥ 85%
2. `docker compose up --build` — backend on `:8080`, frontend on `:5173`
3. `http://localhost:5173` — login page loads; login with `test@example.com / password`
4. CRUD operations via UI hit real backend (via Docker)
5. `cd frontend && npm run test` — Vitest passes
6. `cd frontend && npx playwright test` — e2e specs pass

---

## Mutations Log
| Date | Type | Task | Reason |
|---|---|---|---|
| 2026-04-26 | Insert | Phase 1 (restructure) | User required backend move to backend/ before frontend work |