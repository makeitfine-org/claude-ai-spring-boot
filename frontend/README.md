# Claude AI Spring Boot — Frontend

React 19 + TypeScript + Vite SPA for the Person Management API. Authenticates via JWT and communicates with the Spring Boot backend.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | React 19.2.5, TypeScript (strict) |
| Build | Vite 8.0.10 with HMR |
| Styling | Tailwind CSS 4.2.4 (Vite plugin), shadcn/ui, Radix UI |
| Server state | TanStack React Query 5.100.5 |
| Auth state | React Context (`AuthContext`) |
| Forms | React Hook Form 7.74.0 + Zod 4.3.6 |
| HTTP | Axios 1.15.2 with JWT interceptors |
| Routing | React Router 7.14.2 |
| Unit testing | Vitest 4.1.5 + React Testing Library |
| API mocking | MSW 2.13.6 (Mock Service Worker) |
| E2E testing | Playwright 1.59.1 |

---

## Prerequisites

- Node.js 20+
- npm 10+
- Spring Boot backend running on `http://localhost:8080` (see `../backend/README.md`)

---

## Quick Start

```bash
npm install
npm run dev
```

App opens at **http://localhost:5173**.

---

## Available Scripts

| Script | Command | Description |
|---|---|---|
| `dev` | `vite` | Dev server with HMR on port 5173 |
| `build` | `tsc -b && vite build` | Type-check then bundle to `dist/` |
| `lint` | `eslint .` | Check code quality |
| `preview` | `vite preview` | Preview production build on port 4173 |

---

## Project Structure

```
frontend/
├── src/
│   ├── main.tsx                  # Provider stack: Query → Router → Auth
│   ├── App.tsx                   # Route declarations
│   ├── index.css                 # Tailwind + CSS theme variables
│   ├── auth/
│   │   ├── AuthContext.tsx       # Token state, login/logout, useAuth()
│   │   └── ProtectedRoute.tsx    # Guards routes requiring a valid token
│   ├── features/
│   │   ├── auth/
│   │   │   └── LoginPage.tsx     # Login form (RHF + Zod)
│   │   └── persons/
│   │       ├── PersonsPage.tsx       # CRUD table with pagination
│   │       ├── PersonFormDialog.tsx  # Create / edit dialog (RHF + Zod)
│   │       ├── ConfirmDeleteDialog.tsx
│   │       ├── personsApi.ts         # Axios calls for /api/persons
│   │       └── usePersons.ts         # TanStack Query hooks
│   ├── components/
│   │   └── ui/                   # shadcn/ui components (do not edit)
│   ├── lib/
│   │   ├── api.ts                # Axios instance with JWT interceptors
│   │   └── utils.ts              # cn() helper
│   └── types/
│       ├── auth.ts               # AuthRequest, AuthResponse
│       └── person.ts             # PersonRequest, PersonResponse, Page<T>
├── index.html
├── vite.config.ts                # Tailwind + React plugins, @ alias
├── nginx.conf                    # SPA routing + /api/* proxy
├── Dockerfile                    # Multi-stage: node → nginx
└── package.json
```

---

## Routing

| Path | Component | Auth required |
|---|---|---|
| `/login` | `LoginPage` | No |
| `/persons` | `PersonsPage` | Yes (via `ProtectedRoute`) |
| `*` | Redirects to `/persons` | — |

---

## Auth Flow

1. User submits credentials on `LoginPage` → `POST /api/auth/login`.
2. `AuthContext.login()` stores `accessToken` and `refreshToken` in `localStorage` and updates in-memory state.
3. Every Axios request gets `Authorization: Bearer <accessToken>` via a request interceptor in `src/lib/api.ts`.
4. On a 401 response the interceptor automatically calls `POST /api/auth/refresh` with the refresh token and retries the original request.
5. If the refresh also fails the user is redirected to `/login`.
6. `ProtectedRoute` reads `isAuthenticated` from `useAuth()` and redirects to `/login` when false.

---

## Environment Variables

Create `.env.local` in this directory (git-ignored):

```env
VITE_API_BASE_URL=http://localhost:8080
```

The app defaults to `http://localhost:8080` if the variable is absent. Note: no `/api` suffix — API paths include it (e.g. `/api/persons`).

---

## UI Component Library

All components in `src/components/ui/` are built on **Radix UI** primitives styled with **Tailwind CSS**. Import directly:

```typescript
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Dialog, DialogTrigger, DialogContent } from '@/components/ui/dialog'
import { Table, TableHeader, TableBody, TableRow, TableCell } from '@/components/ui/table'
```

Do not edit files in `src/components/ui/` directly.

---

## TypeScript

- Strict mode enabled — no implicit any, no unused vars/params
- Path alias `@/` maps to `src/` — use it everywhere instead of relative paths
- Target ES2023

---

## Styling

Tailwind CSS 4 is configured as a Vite plugin — no `tailwind.config.js` needed.

Use `cn()` from `@/lib/utils` to merge Tailwind classes safely:

```typescript
import { cn } from '@/lib/utils'

<div className={cn('p-4 rounded', isActive && 'bg-primary text-white')} />
```

---

## Docker

The frontend ships as an Nginx container:

```
Dockerfile — multi-stage build
  stage 1: node:22-alpine  → npm run build → dist/
  stage 2: nginx:1.27-alpine → serves dist/ on port 80
```

`nginx.conf` handles SPA routing (`try_files $uri /index.html`) and proxies all `/api/*` requests to `app:8080`, so the browser never needs to know the backend's address.

```bash
# Build and run via docker-compose (recommended)
docker-compose up frontend

# Build the image standalone
docker build -t claude-ai-spring-boot-frontend .
```

---

## Testing

### Unit tests — Vitest + React Testing Library

```bash
npx vitest run    # run once
npx vitest        # watch mode
```

### API mocking in tests — MSW

```typescript
import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

const server = setupServer(
  http.get('http://localhost:8080/api/persons', () =>
    HttpResponse.json({ content: [], totalElements: 0 })
  )
)

beforeAll(() => server.listen())
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
```

### E2E tests — Playwright

```bash
npx playwright test
```
