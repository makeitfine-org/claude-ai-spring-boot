# Plan: Deduplicate Saved Plans in save-plan.sh

## Context

The `save-plan.sh` hook fires on every `Write` tool call to `~/.claude/plans/`. Each firing creates a new file with a full `YYYY-MM-DD HH:MM` timestamp prefix plus the plan title. When Claude edits a plan multiple times during a session, the result is several files like:

```
2026-04-26 14:07 My Feature Plan.md
2026-04-26 14:31 My Feature Plan.md
2026-04-26 15:02 My Feature Plan.md
```

The user wants only the **latest version** kept — one file per plan title, overwritten on each update.

## Approach

Before writing the destination file, find and delete any existing files in `dest_dir` whose suffix matches ` ${title}.md`. Then write the new file using only the date portion (`YYYY-MM-DD`, no time) to keep filenames stable and readable.

### Why date-only prefix (no time)?
- A plan title is the stable identity; the date shows when it was last worked on.
- Dropping the time component means the filename won't change intra-day, making the "delete old, write new" logic simpler and safer.
- `YYYY-MM-DD` prefix still gives chronological sort order in directory listings.

## Change — `.claude/hooks/save-plan.sh`

**Line 34** (timestamp): change from `date +'%Y-%m-%d %H:%M'` → `date +'%Y-%m-%d'`

**Line 36** (dest_file): keep as `"$dest_dir/${timestamp} ${title}.md"` — now produces `YYYY-MM-DD title.md`

**Add dedup block** between line 36 and the `printf` write (around line 38):

```bash
# Remove any prior version of this plan (any date prefix, same title suffix)
find "$dest_dir" -maxdepth 1 -name "* ${title}.md" -delete 2>/dev/null || true
```

This `find` call:
- Scans only the immediate directory (`-maxdepth 1`)
- Matches any file whose name ends with ` ${title}.md` (the space before title prevents false prefix matches)
- Deletes all matches before the new write lands
- Errors are suppressed and never fatal (`|| true`)

## Critical File

- `.claude/hooks/save-plan.sh` — only file to modify

## Verification

1. Manually trigger two sequential plan writes for the same plan title within a single minute.
2. List `.claude/docs/blackbox/plans/` — confirm only one file exists per title.
3. Confirm the surviving file contains the content of the **latest** write.
4. Confirm the hook still creates a new file correctly for a brand-new plan title.