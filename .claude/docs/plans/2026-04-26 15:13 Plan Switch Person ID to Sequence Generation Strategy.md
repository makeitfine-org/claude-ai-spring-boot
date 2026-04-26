# Plan: Switch Person ID to Sequence Generation Strategy

## Context

The Person entity currently uses `GenerationType.IDENTITY` (PostgreSQL BIGSERIAL), which prevents Hibernate from batching inserts — each INSERT requires a separate round-trip to fetch the generated ID. Switching to `GenerationType.SEQUENCE` with `allocationSize = 20` lets Hibernate pre-allocate 20 IDs per sequence call, enabling JDBC batch inserts and reducing database round-trips. The sequence `persons_id_seq` already exists (created implicitly by BIGSERIAL), so we only need to reconfigure it and update the entity annotation.

---

## Files to Modify

| File | Change |
|---|---|
| `src/main/java/pl/piomin/services/domain/entity/Person.java` | Replace `@GeneratedValue(strategy = GenerationType.IDENTITY)` with SEQUENCE + `@SequenceGenerator` |
| `src/main/resources/db/migration/V4__update_persons_id_sequence.sql` | ALTER SEQUENCE to INCREMENT BY 20 (must match `allocationSize`) |
| `pom.xml` | Bump version 1.0.0 → 1.0.1 |
| `README.md` | Update version reference |

---

## Step-by-Step Implementation

### Step 1 — Person entity (`Person.java`)

Replace the existing annotation block on the `id` field:

**Before:**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

**After:**
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_seq")
@SequenceGenerator(
    name = "person_seq",
    sequenceName = "persons_id_seq",
    allocationSize = 20
)
private Long id;
```

Add import `jakarta.persistence.SequenceGenerator` if not already present.

### Step 2 — Flyway migration `V4__update_persons_id_sequence.sql`

BIGSERIAL creates `persons_id_seq` with `INCREMENT BY 1`. Hibernate's pooled optimizer requires the DB sequence increment to match `allocationSize`:

```sql
ALTER SEQUENCE persons_id_seq INCREMENT BY 20;
```

> **Why this matters:** If the DB increments by 1 but Hibernate thinks it increments by 20, IDs will collide. The sequences must be in sync.

### Step 3 — Version bump

- `pom.xml`: `1.0.0` → `1.0.1`
- `README.md`: update any version badge/reference to `1.0.1`

---

## What Does NOT Need to Change

- `PersonRepository.java` — `JpaRepository<Person, Long>` is unaffected
- All DTOs, mappers, services, controllers — they use `Long id`, which is unchanged
- Test files — they set/assert Long IDs directly; sequence strategy is transparent at the test level
- `docker-compose.yml`, k8s manifests, CI — no structural changes

---

## Verification

```bash
# 1. Run full build + tests + JaCoCo gate
mvn verify

# 2. Check Flyway applied the migration
# (or inspect via psql: \d persons_id_seq)

# 3. Confirm sequence increment
# psql -c "SELECT increment_by FROM pg_sequences WHERE sequencename = 'persons_id_seq';"
# Expected: 20
```

All existing tests pass without modification because they use the entity/repository API, not the generation strategy directly.