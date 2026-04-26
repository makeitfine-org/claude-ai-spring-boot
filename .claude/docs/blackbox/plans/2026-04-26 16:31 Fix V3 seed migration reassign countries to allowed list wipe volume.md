# Fix: V3 seed migration — reassign countries to allowed list + wipe volume

## Context
`postgres_data` named Docker volume persists between `docker compose down/up`, so Flyway
never re-applies V3. Fix: rewrite V3 (all 27 records kept, country values reassigned
to the 8 allowed), then wipe the volume so migrations run fresh.

Allowed countries: Poland, USA, Spain, Germany, Ukraine, Japan, China, Egypt.

## Step 1 — Rewrite V3__seed_persons.sql

File: `src/main/resources/db/migration/V3__seed_persons.sql`

Keep all 27 persons. Change only the `country` column for each row that currently
has a non-allowed country. Distribute evenly across the 8 countries (~3-4 per country).

Country reassignment (name → new country):
| Current country | Person | New country |
|----------------|--------|-------------|
| Poland | Anna Kowalska, Nadia Kowalczyk | Poland (keep) |
| USA | John Smith | USA (keep) |
| Spain | Maria Garcia | Spain (keep) |
| Italy | Luca Rossi | Poland |
| Germany | Sophie Müller | Germany (keep) |
| Japan | Kenji Tanaka, Yuki Sato | Japan (keep) |
| UAE | Fatima Al-Hassan | Egypt |
| Mexico | Carlos Mendez | USA |
| Russia | Elena Petrova | Ukraine |
| China | Wei Zhang | China (keep) |
| Senegal | Amara Diallo | Egypt |
| Ireland | Patrick O'Brien | USA |
| India | Priya Sharma | China |
| France | Lucas Dubois | Spain |
| Sweden | Sara Lindqvist | Germany |
| Egypt | Ahmed Hassan | Egypt (keep) |
| Brazil | Isabella Fernandez | Spain |
| Switzerland | Oliver Schneider | Germany |
| UK | James Robinson | USA |
| Czech Republic | Hana Novak | Ukraine |
| Saudi Arabia | Mohammed Al-Rashid | Egypt |
| Belgium | Chloe Martin | Germany |
| Argentina | Diego Torres | Spain |
| Taiwan | Mei Chen | China |
| Finland | Elsa Virtanen | Ukraine |

Final distribution: Poland×3, USA×4, Spain×4, Germany×4, Ukraine×3, Japan×2, China×3, Egypt×4 = 27 total.

## Step 2 — Wipe volume and restart

Since V3 was already applied (old checksum), the volume must be destroyed so Flyway
starts from scratch with the new V3 content.

```
docker compose down -v
docker compose up
```

## Critical files
- `src/main/resources/db/migration/V3__seed_persons.sql` — update country values only

## Verification
```sql
SELECT country, COUNT(*) FROM persons GROUP BY country ORDER BY country;
-- expected: 8 rows (Poland 3, USA 4, Spain 4, Germany 4, Ukraine 3, Japan 2, China 3, Egypt 4)
SELECT COUNT(*) FROM persons;  -- expected: 27
```