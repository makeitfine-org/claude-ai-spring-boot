# Plan: Column Sorting + Live Multi-Field Search on Persons Page

## Context

Today the Persons table has two UX gaps:

1. **No column sorting.** The list is hardcoded to `lastName,asc` server-side; users can't reorder by First name, Email, Phone, City, or Active.
2. **Search is email-exact-match only.** `GET /api/persons/search?email=...` returns a single record via `findByEmail` (unique). Users want substring search across First name, Last name, Email, Phone, City — triggered as they type.

This plan adds clickable sort headers and replaces the email search with an incremental "contains" search across multiple fields, while reusing the existing `Page<PersonResponse>` pagination pipeline.

---

## Approach

### 1. Backend — extend `GET /api/persons` with a `q` filter

- **Repository** (`backend/src/main/java/pl/piomin/services/domain/repository/PersonRepository.java`):
  Add a `JpaSpecificationExecutor<Person>` extension OR a custom `@Query` method:
  ```java
  @Query("""
      SELECT p FROM Person p
      WHERE :q IS NULL OR :q = ''
         OR LOWER(p.firstName)   LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(p.lastName)    LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(p.email)       LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(p.phoneNumber) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(p.city)        LIKE LOWER(CONCAT('%', :q, '%'))
      """)
  Page<Person> search(@Param("q") String q, Pageable pageable);
  ```
  Prefer this single-method approach (simpler, no Specification plumbing).

- **Service** (`PersonService.getAllPersons`): accept optional `String q`, delegate to `repository.search(q, pageable)` (when `q == null/blank`, the query degrades to `findAll` semantics via the `:q IS NULL OR :q = ''` short-circuit).

- **Controller** (`PersonController.java:37-43`): add `@RequestParam(required = false) String q`:
  ```java
  @GetMapping
  public ResponseEntity<Page<PersonResponse>> getAllPersons(
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC) Pageable pageable) {
      return ResponseEntity.ok(personService.getAllPersons(q, pageable));
  }
  ```
- **Keep** `findByEmail` repository method (still used by validation paths via `existsByEmail`); remove the now-unused `/api/persons/search` controller endpoint OR leave it for now and delete in a follow-up. **Recommendation: delete it** to avoid dead code (also delete `PersonService.findByEmail` if nothing else uses it — verify with grep).

- **Sort whitelist:** Spring's `Pageable` will accept any property name from the URL. Add a guard in the service to whitelist `firstName, lastName, email, phoneNumber, city, active` (silently drop unknown sort fields) to prevent ORDER-BY on non-existent columns.

- **Tests** (per `backend/CLAUDE.md` requirements):
  - `PersonRepositoryTest`: positive (substring matches across each field, case-insensitive) + negative (no match returns empty page; null/blank `q` returns all).
  - `PersonServiceTest`: mocked repo; verify whitelist drops unknown sort fields.
  - `PersonIntegrationTest`: `GET /api/persons?q=abc&sort=firstName,desc` returns expected results, paginated.
  - Maintain ≥85% JaCoCo line coverage per package.

### 2. Frontend — clickable sort headers + unified search

- **`personsApi.ts`**: Replace `searchByEmail` with the unified list call accepting `q` and `sort`:
  ```ts
  list: (page: number, size: number, sort: string, q?: string) =>
    api.get<Page<PersonResponse>>('/api/persons', {
      params: { page, size, sort, ...(q ? { q } : {}) },
    }).then(r => r.data),
  ```
  Drop `searchByEmail`.

- **`usePersons.ts`**: Drop `usePersonSearch`. Update `usePersonsList` to accept `(page, size, sort, q)`; include all four in the queryKey so TanStack Query caches per-combination.

- **`PersonsPage.tsx`** (`frontend/src/features/persons/PersonsPage.tsx`):
  - State: add `sortField: 'firstName' | 'lastName' | 'email' | 'phoneNumber' | 'city' | 'active'` (default `'lastName'`) and `sortDir: 'asc' | 'desc'` (default `'asc'`). Build `sort = ${sortField},${sortDir}`.
  - Replace `searchEmail` / `debouncedEmail` / `usePersonSearch` with a single `q` + `debouncedQ` (keep 400 ms debounce — already adequate). Remove the `isSearching` branching: search now flows through the same paginated list query.
  - Search input: change placeholder to `"Search name, email, phone, city…"` and keep the existing live-as-you-type debounce.
  - **Fix existing bug:** `emailTimer` is declared inside the component body (line 69), so a new timer is created on every render — `clearTimeout` never sees the previous handle. Move it into a `useRef<ReturnType<typeof setTimeout> | null>(null)` so debouncing actually works.
  - Sortable headers: wrap `TableHead` content in a button. Clicking the active column toggles `asc`/`desc`; clicking another column sets it as active with `asc`. Show an arrow indicator (`ChevronUpIcon`/`ChevronDownIcon` from `lucide-react`, already used elsewhere) next to the active column. The "Actions" column stays non-sortable.
  - Header → sort field map:
    - First name → `firstName`
    - Last name → `lastName`
    - Email → `email`
    - Phone → `phoneNumber`
    - City → `city`
    - Active → `active`
  - When `q` or `sort` changes, reset `page` to 0.

- **Tests:**
  - Vitest + RTL on `PersonsPage`: clicking each header issues a list call with the right `sort` param (mock via MSW); typing in the search input triggers a debounced list call with `q`.
  - Optional: a Playwright happy-path test confirming sort toggle + search end-to-end.

### 3. Out of scope (intentionally deferred)

- Multi-column sort (Spring supports it via repeated `sort` params, but the UX is awkward and the user didn't ask).
- Server-side full-text search / Postgres trigram indexes — `LIKE '%x%'` is fine at this dataset size; revisit if it gets slow.
- Highlighting matched substrings in cells.

---

## Critical Files

**Backend**
- `backend/src/main/java/pl/piomin/services/domain/repository/PersonRepository.java` — add `search(q, pageable)`
- `backend/src/main/java/pl/piomin/services/application/service/PersonService.java` — add `q` param + sort whitelist
- `backend/src/main/java/pl/piomin/services/presentation/rest/PersonController.java:37-64` — add `q` param to GET; remove `/search` endpoint
- `backend/src/test/java/.../PersonRepositoryTest.java`, `PersonServiceTest.java`, `PersonIntegrationTest.java` — extend

**Frontend**
- `frontend/src/features/persons/personsApi.ts` — unify list, drop `searchByEmail`
- `frontend/src/features/persons/usePersons.ts` — drop `usePersonSearch`, extend `usePersonsList`
- `frontend/src/features/persons/PersonsPage.tsx` — sort state, clickable headers, live search, fix debounce ref bug

---

## Verification

**Backend**
- `cd backend && mvn verify` — all tests + JaCoCo ≥85% pass
- Manual: `curl 'http://localhost:8080/api/persons?q=an&sort=firstName,desc&page=0&size=10'` (with JWT) returns paginated, filtered, sorted results
- Manual: `curl '...?sort=bogusField,asc'` returns 200 with default sort applied (whitelist guard)

**Frontend**
- `cd frontend && npm test` (Vitest) — sort + search tests pass
- `npm run dev`, log in, open Persons page:
  - Type `a` → table updates after ~400 ms with rows containing `a` in any of the 5 fields
  - Type `abc` → narrows further
  - Clear search → full list returns
  - Click "First name" header → ascending by first name; click again → descending; arrow indicator flips
  - Repeat for Last name, Email, Phone, City, Active columns
  - Sorting interacts correctly with pagination (page resets to 0; page count updates)

**Cross-cutting (per root CLAUDE.md delivery checklist)**
- No new env vars / services → `docker-compose.yml` unchanged
- No new ports / quick-start changes → root `README.md` unchanged
- No CI step changes → `.github/workflows/ci.yml` unchanged
- Bump `pom.xml` PATCH version + `backend/README.md` version line; follow `.claude/rules/version-bump-procedure.md` (Dockerfile COPY line, rebuild)