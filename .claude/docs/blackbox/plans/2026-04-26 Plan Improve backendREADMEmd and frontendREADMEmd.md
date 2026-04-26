# Plan: Improve backend/README.md and frontend/README.md

## Context
The backend README is functional but lacks complete curl command examples and response details for every endpoint. The frontend README is still the generic Vite template — it has zero project-specific content. The user wants both replaced with thorough developer guides.

---

## Task 1 — Rewrite `backend/README.md`

### What changes
- Keep existing structure but expand every API section with exact curl commands
- Add a **Quick Auth Flow** section showing how to capture the token in a shell variable
- Add complete request/response JSON for every endpoint
- Add **Error Responses** for 400/401/404 per endpoint
- Fix the Environment Variables table (add `SPRING_PROFILES_ACTIVE`)
- Update Project Structure to match actual layer names
- Add a **Validation Rules** section for PersonRequest fields

### Curl examples to include
| Endpoint | Method | Auth required |
|---|---|---|
| `/api/auth/login` | POST | No |
| `/api/auth/refresh` | POST | Refresh token |
| `/api/persons` | POST | Yes |
| `/api/persons?page=0&size=20&sort=lastName,asc` | GET | Yes |
| `/api/persons/{id}` | GET | Yes |
| `/api/persons/{id}` | PUT | Yes |
| `/api/persons/{id}` | DELETE | Yes |
| `/api/persons/search?email=...` | GET | Yes |
| `/actuator/health` | GET | No |

Key pattern — capture token once and reuse:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}' \
  | jq -r '.accessToken')
```

---

## Task 2 — Rewrite `frontend/README.md`

### What changes
Replace the Vite template boilerplate entirely with a project-specific guide covering:
1. **Overview** — what the app is, tech stack summary
2. **Prerequisites** — Node.js ≥20, npm
3. **Quick Start** — `npm install && npm run dev`
4. **Project Structure** — actual `src/` tree with descriptions
5. **Available Scripts** — dev, build, lint, preview
6. **UI Component Library** — list of shadcn/ui components and how to use them
7. **Backend Integration** — base URL, how auth tokens flow, Axios + React Query pattern
8. **Environment Variables** — `VITE_API_BASE_URL`, how to create `.env.local`
9. **State Management** — Redux for auth/UI, React Query for server data, RHF for forms
10. **Testing** — Vitest + RTL for unit, Playwright for E2E, MSW for mocking
11. **TypeScript** — strict mode, path alias `@/` → `src/`
12. **Styling** — Tailwind v4 via Vite plugin, CSS custom properties for theming
13. **What's Implemented vs What Needs Building** — honest status of the starter

---

## Files Modified
- `backend/README.md` — full rewrite with complete curl guide
- `frontend/README.md` — full rewrite replacing Vite template

## Files Read (no change)
- `backend/src/main/resources/application.yml` — for port/env defaults
- `backend/src/main/java/pl/piomin/services/application/dto/` — for DTO field constraints
- `frontend/package.json` — for scripts and versions
- `frontend/vite.config.ts` — for alias and plugins

---

## Verification
1. Read both READMEs after writing to confirm correctness
2. Manually verify every curl command matches the actual controller paths from the explore output
3. Check token capture pattern works end-to-end (login → store → use)