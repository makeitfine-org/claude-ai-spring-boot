# Claude AI Spring Boot — Frontend

React 19 + TypeScript + Vite frontend for the Person Management API. Connects to the Spring Boot backend running on `http://localhost:8080`.

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | React 19, TypeScript 6 |
| Build | Vite 8 with HMR |
| Styling | Tailwind CSS v4 (Vite plugin), shadcn/ui, Radix UI |
| State — server | TanStack React Query 5 |
| State — global | Redux Toolkit 2 + React Redux 9 |
| State — forms | React Hook Form 7 + Zod 4 |
| HTTP | Axios 1.15 |
| Routing | React Router v7 (installed, routing not yet wired) |
| Testing | Vitest 4 + React Testing Library 16 + Playwright 1.59 |
| API mocking | MSW 2 (Mock Service Worker) |

---

## Prerequisites

- Node.js 20+
- npm 10+
- Spring Boot backend running on `http://localhost:8080` (see `../backend/README.md`)

---

## Quick Start

```bash
# Install dependencies
npm install

# Start dev server with HMR
npm run dev
```

App opens at **http://localhost:5173**.

---

## Available Scripts

| Script | Command | Description |
|---|---|---|
| `dev` | `vite` | Dev server with HMR on port 5173 |
| `build` | `tsc -b && vite build` | Type-check then bundle to `dist/` |
| `lint` | `eslint .` | Check code quality (ESLint + TypeScript rules) |
| `preview` | `vite preview` | Preview production build on port 4173 |

---

## Project Structure

```
frontend/
├── src/
│   ├── main.tsx              # Entry point — React 19 strict mode
│   ├── App.tsx               # Root component
│   ├── index.css             # Global styles (Tailwind + CSS theme variables)
│   ├── assets/               # Static images and SVGs
│   ├── components/
│   │   └── ui/               # shadcn/ui component library (pre-built)
│   │       ├── alert.tsx
│   │       ├── badge.tsx
│   │       ├── button.tsx
│   │       ├── card.tsx
│   │       ├── dialog.tsx
│   │       ├── input.tsx
│   │       ├── label.tsx
│   │       ├── select.tsx
│   │       └── table.tsx
│   └── lib/
│       └── utils.ts          # cn() helper for merging Tailwind classes
├── index.html
├── vite.config.ts            # Vite — Tailwind + React plugins, @ alias
├── tsconfig.app.json         # Strict TypeScript config for src/
├── components.json           # shadcn/ui config
└── package.json
```

### Recommended structure when adding features

```
src/
├── pages/                    # One component per route
│   ├── LoginPage.tsx
│   ├── PersonListPage.tsx
│   └── PersonDetailPage.tsx
├── components/
│   ├── ui/                  # shadcn/ui (already here — do not edit)
│   ├── forms/               # Form components built with RHF + Zod
│   └── features/            # Feature-specific display components
├── hooks/                    # Custom hooks (usePersons, useAuth, …)
├── api/                      # Axios client + per-resource API functions
│   ├── client.ts            # Axios instance with interceptors
│   ├── authApi.ts
│   └── personApi.ts
├── store/                    # Redux store
│   ├── store.ts
│   └── authSlice.ts
└── types/                    # TypeScript interfaces matching backend DTOs
    └── person.ts
```

---

## Environment Variables

Create a `.env.local` file in this directory (git-ignored):

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

Access in code:

```typescript
const baseUrl = import.meta.env.VITE_API_BASE_URL;
```

If `VITE_API_BASE_URL` is not set the app defaults to `http://localhost:8080/api`.

---

## UI Component Library

All components live in `src/components/ui/` and are built on **Radix UI primitives** styled with **Tailwind CSS**. Import directly:

```typescript
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Dialog, DialogTrigger, DialogContent } from '@/components/ui/dialog'
import { Select, SelectTrigger, SelectContent, SelectItem } from '@/components/ui/select'
import { Alert, AlertTitle, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableCell } from '@/components/ui/table'
```

### Button variants

```tsx
<Button variant="default">Primary</Button>
<Button variant="outline">Outline</Button>
<Button variant="destructive">Delete</Button>
<Button variant="ghost">Ghost</Button>
<Button variant="secondary">Secondary</Button>
<Button size="sm">Small</Button>
<Button size="lg">Large</Button>
```

---

## Backend Integration

### Auth flow

1. Call `POST /api/auth/login` → receive `accessToken` + `refreshToken`
2. Store tokens (Redux slice recommended)
3. Attach `Authorization: Bearer <accessToken>` to every protected request via an Axios interceptor
4. On 401 response, call `POST /api/auth/refresh` with the refresh token, retry the original request

### Suggested Axios client (`src/api/client.ts`)

```typescript
import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api'

export const api = axios.create({ baseURL: BASE_URL })

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
```

### Suggested React Query hook (`src/hooks/usePersons.ts`)

```typescript
import { useQuery } from '@tanstack/react-query'
import { api } from '@/api/client'

export function usePersons(page = 0, size = 20) {
  return useQuery({
    queryKey: ['persons', page, size],
    queryFn: () =>
      api.get('/persons', { params: { page, size, sort: 'lastName,asc' } })
         .then(res => res.data),
  })
}
```

### Backend endpoints summary

| Purpose | Method | Path |
|---|---|---|
| Login | POST | `/api/auth/login` |
| Refresh token | POST | `/api/auth/refresh` |
| List persons | GET | `/api/persons?page=0&size=20&sort=lastName,asc` |
| Get person | GET | `/api/persons/{id}` |
| Create person | POST | `/api/persons` |
| Update person | PUT | `/api/persons/{id}` |
| Delete person | DELETE | `/api/persons/{id}` |
| Search by email | GET | `/api/persons/search?email=` |
| Health check | GET | `/actuator/health` |

See `../backend/README.md` for full request/response shapes and curl examples.

---

## State Management

| Type | Tool | When to use |
|---|---|---|
| Server / API data | React Query | Persons list, person detail, mutations |
| Auth tokens + user | Redux Toolkit | Persisted across routes |
| Form inputs | React Hook Form | Create / edit person forms |
| Local UI | `useState` | Modals, toggles, transient state |

---

## TypeScript

- **Strict mode** enabled — no implicit any, no unused vars/params
- **Path alias** `@/` maps to `src/` — use it everywhere instead of relative paths
- **Target** ES2023 — modern browsers only

---

## Styling

### Tailwind CSS v4

Configured as a Vite plugin — no `tailwind.config.js` needed.

### Theme

CSS custom properties are defined in `src/index.css` under `:root` (light) and `.dark` (dark mode). Change colors there:

```css
:root {
  --primary: oklch(0.205 0 0);
  --radius: 0.625rem;
}
```

### `cn()` helper

Use `cn()` from `@/lib/utils` to merge Tailwind classes safely:

```typescript
import { cn } from '@/lib/utils'

<div className={cn('p-4 rounded', isActive && 'bg-primary text-white')} />
```

---

## Testing

### Unit tests — Vitest + React Testing Library

```bash
npx vitest run        # run once
npx vitest            # watch mode
```

```typescript
import { render, screen } from '@testing-library/react'
import { Button } from '@/components/ui/button'

it('renders button text', () => {
  render(<Button>Save</Button>)
  expect(screen.getByText('Save')).toBeInTheDocument()
})
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

---

## What's Ready vs What Needs Building

### Ready to use
- UI component library (Button, Card, Input, Label, Select, Dialog, Alert, Badge, Table)
- Tailwind CSS v4 theming with dark mode
- Redux Toolkit + React Redux wiring
- React Query client
- React Hook Form + Zod validation
- Axios HTTP client
- React Router v7 (installed)
- Vitest + RTL + Playwright + MSW test infrastructure

### Still needs implementation
- Routing — add `BrowserRouter` + `Routes` to `App.tsx`
- Pages — `LoginPage`, `PersonListPage`, `PersonDetailPage`, `PersonFormPage`
- Axios instance with JWT interceptors (`src/api/client.ts`)
- Auth Redux slice + token persistence (`src/store/authSlice.ts`)
- React Query hooks for person CRUD (`src/hooks/usePersons.ts`)
- Person form with Zod schema matching backend validation rules
- Global error boundary and API error handling
- Loading states and empty states
- Unit and E2E tests for implemented pages
