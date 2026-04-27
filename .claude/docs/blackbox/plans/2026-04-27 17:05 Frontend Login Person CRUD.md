# Frontend Login + Person CRUD

## Context

The frontend (`frontend/`) is a fresh Vite + React 19 + TS scaffold with the right libraries installed (react-router, axios, @tanstack/react-query, react-hook-form, zod, shadcn/ui, Tailwind v4) but no actual app code — `App.tsx` is still the default counter demo. The Spring Boot backend already exposes JWT auth (`/api/auth/login`, `/api/auth/refresh`) and a full Person REST API (`/api/persons` CRUD + paginated listing + `/search?email=`), with CORS open to `*` and a hardcoded test user `test@example.com` / `password`.

We will build the missing frontend: a login page that obtains JWT tokens, a protected app shell, and a Persons screen offering list/create/edit/delete against the backend. Verification will be a manual browser run against the backend.

## Approach

Use the libraries already in `package.json`. Skip Redux Toolkit — auth state is small enough for a `AuthContext` + `localStorage`; server state goes through React Query. Forms use react-hook-form + zod. UI uses shadcn/ui components already present (`button`, `input`, `label`, `card`, `dialog`, `table`, `alert`, `select`, `badge`).

### Architecture

```
frontend/src/
  main.tsx                  # wrap App with QueryClientProvider, BrowserRouter, AuthProvider
  App.tsx                   # routes only
  lib/
    api.ts                  # axios instance + interceptors (attach token, refresh on 401)
    utils.ts                # (existing)
  auth/
    AuthContext.tsx         # { user, accessToken, refreshToken, login, logout } — persists to localStorage
    ProtectedRoute.tsx      # redirects to /login if no token
  features/
    auth/
      LoginPage.tsx         # email + password form (zod), calls api.login
    persons/
      personsApi.ts         # list/get/create/update/delete/searchByEmail (axios calls)
      usePersons.ts         # React Query hooks wrapping personsApi
      PersonsPage.tsx       # paginated table, search-by-email, New/Edit/Delete actions
      PersonFormDialog.tsx  # shadcn Dialog with PersonRequest form (create + edit)
      ConfirmDeleteDialog.tsx
  types/
    auth.ts                 # AuthRequest, AuthResponse
    person.ts               # PersonRequest, PersonResponse, Page<T>
```

### Auth flow

- `POST /api/auth/login` → store `accessToken`, `refreshToken` in `localStorage` and AuthContext.
- Axios request interceptor adds `Authorization: Bearer <accessToken>`.
- Axios response interceptor: on 401, call `POST /api/auth/refresh` with refresh token, retry original request once; on second failure, clear tokens and redirect to `/login`.
- Logout: clear context + storage, navigate to `/login`.

### Routes

| Path | Component | Guard |
|---|---|---|
| `/login` | `LoginPage` | redirect to `/persons` if already authed |
| `/persons` | `PersonsPage` | `ProtectedRoute` |
| `*` | redirect → `/persons` | — |

### PersonsPage UX

- Table columns: First name, Last name, Email, Phone, City, Active (Badge), Actions (Edit / Delete).
- Top bar: "New person" button, search input (filters by email via `/api/persons/search?email=`), pagination controls (page/size, server-side via `/api/persons?page=&size=&sort=lastName,asc`).
- Create / Edit open `PersonFormDialog` with fields matching `PersonRequest` (firstName, lastName, email, phoneNumber, street, city, postalCode, country, dateOfBirth, active). Zod enforces required + max-length rules from the backend DTO.
- Delete opens `ConfirmDeleteDialog` → `DELETE /api/persons/{id}`.
- React Query mutations invalidate the `['persons', page, size]` key on success; show inline error alert on failure.

### Config

- Add `frontend/.env`:
  ```
  VITE_API_BASE_URL=http://localhost:8080
  ```
- `lib/api.ts` reads `import.meta.env.VITE_API_BASE_URL`.
- Alternative considered: Vite proxy in `vite.config.ts`. Skipped — backend CORS is already wide-open, and an explicit base URL is clearer for production builds.

## Files to create

- `frontend/.env`
- `frontend/src/lib/api.ts`
- `frontend/src/auth/AuthContext.tsx`
- `frontend/src/auth/ProtectedRoute.tsx`
- `frontend/src/types/auth.ts`
- `frontend/src/types/person.ts`
- `frontend/src/features/auth/LoginPage.tsx`
- `frontend/src/features/persons/personsApi.ts`
- `frontend/src/features/persons/usePersons.ts`
- `frontend/src/features/persons/PersonsPage.tsx`
- `frontend/src/features/persons/PersonFormDialog.tsx`
- `frontend/src/features/persons/ConfirmDeleteDialog.tsx`

## Files to modify

- `frontend/src/App.tsx` — replace the counter demo with `<Routes>`.
- `frontend/src/main.tsx` — wrap `<App />` in `QueryClientProvider`, `BrowserRouter`, `AuthProvider`.

## Reuse

- All shadcn/ui primitives in `frontend/src/components/ui/` (button, input, label, card, dialog, alert, table, select, badge).
- `cn()` from `frontend/src/lib/utils.ts`.
- Already-installed deps: `axios`, `@tanstack/react-query`, `react-router-dom`, `react-hook-form`, `zod`, `@hookform/resolvers`, `lucide-react`.

## Out of scope

- Registration / signup (no backend endpoint exists).
- Role/permission UI (backend only has `ROLE_USER`).
- Redux store wiring — Context + React Query is sufficient; we can introduce RTK later if global client state grows.
- Unit/E2E tests for the frontend — task is "check it in browser". Tests can be added in a follow-up.

## Verification

1. **Backend up:** `docker compose up -d postgres` then `cd backend && mvn spring-boot:run` (port 8080). Or use the existing `docker compose up`.
2. **Frontend dev server:** `cd frontend && npm install && npm run dev` → opens `http://localhost:5173`.
3. **Login golden path:** navigate to `/`, expect redirect to `/login`. Enter `test@example.com` / `password` → redirected to `/persons`, table populated with the 27 seeded persons.
4. **Login negative:** wrong password → red alert "Invalid email or password", no navigation.
5. **List & paginate:** change page size, navigate pages — verify network calls hit `/api/persons?page=...`.
6. **Search:** type a seeded email (e.g. `anna.kowalska@example.com`) → row filters to that single person.
7. **Create:** "New person" → fill form → submit → row appears in table; reload page → still there.
8. **Edit:** click Edit on a row → change firstName → save → table updates.
9. **Delete:** click Delete → confirm → row disappears; backend returns 204.
10. **Token refresh:** in DevTools, edit `localStorage.accessToken` to a junk value, perform any action → interceptor calls `/api/auth/refresh`, retries, succeeds without user-visible error.
11. **Logout:** logout button in header → tokens cleared, redirected to `/login`, accessing `/persons` URL bounces back to `/login`.
12. **Build:** `npm run build` passes (TS strict + Vite build).