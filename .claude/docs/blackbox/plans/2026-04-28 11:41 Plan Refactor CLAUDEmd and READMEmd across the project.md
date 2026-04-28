# Plan — Refactor CLAUDE.md and README.md across the project

## Context

Full-stack monorepo: Spring Boot 3.4.1 backend + React 19/Vite frontend + PostgreSQL.
Containerised via `docker-compose.yml` (postgres:5432, app:8080, frontend:3000).

Problems driving this change:
- Root `CLAUDE.md` had no awareness of the frontend module; mixed backend-only rules (Maven/Flyway/JaCoCo) with general guidance.
- `frontend/README.md` was stale: claimed routing, auth, API hooks "still need implementation" — all are already built. Referenced Redux/Redux slice; actual implementation uses `AuthContext.tsx` (React Context, no Redux).
- No root `README.md` — newcomers had no project entry point.
- No per-module CLAUDE.md files.

## Status

| File | Status |
|---|---|
| `/CLAUDE.md` | **DONE** — already rewritten (slim, cross-cutting only) |
| `/README.md` | To create |
| `/backend/CLAUDE.md` | To create |
| `/frontend/CLAUDE.md` | To create |
| `/frontend/README.md` | To rewrite |
| `/backend/README.md` | To update (minor) |

---

## File 1 — `/README.md` (create)

Root entry point for the project. Sections:

- **What it is** — one-paragraph description of the full-stack app
- **Architecture** — ASCII/text diagram: Browser → Nginx (3000) → React SPA; Nginx proxies `/api/*` → Spring Boot (8080) → PostgreSQL (5432)
- **Quick Start** — `docker-compose up` brings up all three services; verify with `curl http://localhost:8080/actuator/health`
- **Services** — table: Service | Port | Description for postgres, app, frontend
- **Default credentials** — email/password for the seeded user
- **Repository layout** — `backend/`, `frontend/`, `docker-compose.yml`, `.github/workflows/ci.yml`
- **Links** — `backend/README.md` (API reference), `frontend/README.md` (frontend dev guide)

---

## File 2 — `/backend/CLAUDE.md` (create)

Backend-specific rules extracted from the old root CLAUDE.md + version-bump procedure pointer.

Sections:
- **Stack** — Java 21, Spring Boot 3.4.1, PostgreSQL 16, MapStruct 1.6.3, JJWT 0.12.6, Testcontainers 1.21.4, JaCoCo 0.8.12
- **Architecture Layers** — domain → application → presentation → infrastructure → config (no upward imports); DTO location; MapStruct-only mapping
- **Coding Rules** — group ID `pl.piomin.services`, no Lombok, Context7 MCP before any Spring/library API code
- **Flyway** — file pattern, never modify applied migrations, sequential numbering (currently V1–V4)
- **Testing** — JaCoCo 85% minimum per package, Testcontainers (never H2 for integration), required test file patterns (`*ServiceTest`, `*RepositoryTest`, `*IntegrationTest`), positive + negative cases
- **Version & Delivery Checklist** — `mvn verify`, Flyway numbering, `pom.xml` PATCH bump + `backend/README.md` update, `docker-compose.yml`, CI, k8s manifests; pointer to `.claude/rules/version-bump-procedure.md`

---

## File 3 — `/frontend/CLAUDE.md` (create)

Frontend-specific rules. Sections:

- **Stack** — React 19.2.5, Vite 8.0.10, TypeScript (strict), Tailwind CSS 4.2.4, shadcn/ui + Radix UI, TanStack Query 5.100.5, React Hook Form 7.74 + Zod 4.3.6, Axios 1.15.2, React Router 7.14.2, Vitest 4.1.5, MSW 2.13.6, Playwright 1.59.1
- **Auth** — use `AuthContext` / `useAuth()` from `src/auth/AuthContext.tsx`; do NOT introduce Redux for auth; tokens in `localStorage`
- **Server state** — TanStack Query for all API data; hooks in `src/features/<feature>/use*.ts`
- **API client** — `src/lib/api.ts` (Axios instance); base URL from `VITE_API_BASE_URL` env var; interceptors handle JWT attach + 401 refresh
- **Forms** — React Hook Form + Zod; schema in same file as form component
- **Component conventions** — shadcn/ui components in `src/components/ui/` (do not edit); path alias `@/` maps to `src/`; Context7 MCP before any library API code
- **File layout** — `src/auth/`, `src/features/{auth,persons}/`, `src/components/ui/`, `src/lib/`, `src/types/`
- **Testing** — Vitest + React Testing Library for units; MSW for API mocking in tests; Playwright for E2E

---

## File 4 — `/frontend/README.md` (rewrite)

Fix all stale content. Key changes vs current file:

| Current (wrong) | Corrected |
|---|---|
| State — global: Redux Toolkit 2 + React Redux 9 | Auth state via React Context (`AuthContext.tsx`); Redux not used |
| "Routing not yet wired" | Wired: `/login` → LoginPage, `/persons` (protected) → PersonsPage |
| "Still needs implementation": routing, pages, API hooks, auth slice, person form | All implemented — remove that section |
| `.env` example shows `http://localhost:8080/api` | Actual value: `http://localhost:8080` (no `/api` suffix) |
| No Docker section | Add: multi-stage Dockerfile (node:22-alpine → nginx:1.27-alpine), nginx.conf proxies `/api/*` to `app:8080` |
| Project structure doesn't match reality | Document real layout: `src/auth/`, `src/features/{auth,persons}/`, `src/lib/api.ts`, `src/types/` |
| Versions: React 19, Vite 8, TanStack 5, etc. shown generically | Pin to actual: React 19.2.5, Vite 8.0.10, TanStack Query 5.100.5, Tailwind 4.2.4, RHF 7.74.0, Zod 4.3.6, axios 1.15.2, react-router 7.14.2, Vitest 4.1.5, MSW 2.13.6, Playwright 1.59.1 |

Sections to keep (accurate): Quick Start, Available Scripts, Environment Variables description, UI Component Library reference, TypeScript rules, Styling.

Add new sections: **Auth Flow** (describes `AuthContext`, `ProtectedRoute`, token refresh), **Docker** (Dockerfile + nginx), **What's implemented** (accurate current state).

---

## File 5 — `/backend/README.md` (minor update)

Two additions only — no existing content changed:

1. After the intro paragraph, add a short **Full-Stack** note: `docker-compose up` also starts the React frontend on **port 3000**; link to `../frontend/README.md`.
2. After the "Docker Deployment" section, add a **Frontend** section: one sentence + link.

---

## Verification

After writing all files:

```bash
# All files present
ls README.md backend/CLAUDE.md frontend/CLAUDE.md backend/README.md frontend/README.md CLAUDE.md

# No backend-only rules remain in root CLAUDE.md
grep -i "jacoco\|flyway\|mvn\|mapstruct" CLAUDE.md  # should return nothing

# Frontend README does not mention Redux
grep -i "redux" frontend/README.md  # should return nothing

# Frontend README does not claim things are unimplemented
grep -i "still needs\|not yet wired" frontend/README.md  # should return nothing
```