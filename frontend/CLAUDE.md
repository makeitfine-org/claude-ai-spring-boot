# CLAUDE.md — frontend

## Stack

- React 19.2.5, TypeScript (strict), Vite 8.0.10
- Tailwind CSS 4.2.4 (Vite plugin), shadcn/ui, Radix UI
- TanStack Query 5.100.5 (server state)
- React Hook Form 7.74.0 + Zod 4.3.6 (forms)
- Axios 1.15.2 (HTTP client)
- React Router 7.14.2 (routing)
- Vitest 4.1.5, React Testing Library, MSW 2.13.6, Playwright 1.59.1

## Auth

- Use `useAuth()` from `src/auth/AuthContext.tsx` for token access and login/logout.
- `ProtectedRoute` in `src/auth/ProtectedRoute.tsx` guards routes that require a valid token.
- Tokens are stored in `localStorage` (`accessToken`, `refreshToken`).
- Do NOT introduce Redux or any other global state library for auth.

## Server State

- Use TanStack Query for all API data — no manual `useEffect` fetching.
- Place query/mutation hooks in `src/features/<feature>/use*.ts` alongside the feature.

## API Client

- Single Axios instance: `src/lib/api.ts`.
- Base URL from `VITE_API_BASE_URL` env var (default `http://localhost:8080`); no trailing `/api` — paths include it.
- Interceptors handle: JWT attachment on every request; 401 → refresh token → retry; on refresh failure → redirect to `/login`.
- Never create a second Axios instance; always import `api` from `@/lib/api`.

## Forms

- React Hook Form + Zod for all forms. Put the Zod schema in the same file as the form component.
- Use `@hookform/resolvers/zod` resolver.

## Component Conventions

- shadcn/ui components live in `src/components/ui/` — never edit these files directly; add new ones with the shadcn CLI or manually following the same pattern.
- Path alias `@/` maps to `src/` — use it everywhere instead of relative paths.
- Use **Context7 MCP** before writing any React/Vite/library API code — docs drift.

## File Layout

```
src/
├── auth/               → AuthContext.tsx, ProtectedRoute.tsx
├── features/
│   ├── auth/           → LoginPage.tsx
│   └── persons/        → PersonsPage.tsx, PersonFormDialog.tsx,
│                          ConfirmDeleteDialog.tsx, personsApi.ts, usePersons.ts
├── components/ui/      → shadcn/ui (Button, Card, Dialog, Input, …)
├── lib/                → api.ts (Axios), utils.ts (cn helper)
├── types/              → auth.ts, person.ts
├── App.tsx             → route declarations
└── main.tsx            → provider stack (Query, Router, Auth)
```

## Testing

- Unit tests: Vitest + React Testing Library.
- API mocking in tests: MSW (`msw/node` setup with `http` and `HttpResponse`).
- E2E: Playwright (`npx playwright test`).
- Test files co-locate with the code they test or live in a `__tests__/` sibling.
