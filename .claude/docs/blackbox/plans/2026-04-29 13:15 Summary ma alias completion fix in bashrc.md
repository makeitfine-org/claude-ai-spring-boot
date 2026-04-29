# Summary: `ma` alias completion fix in `~/.bashrc`

## Context
Adding `alias ma="make"` broke Tab completion: `make <Tab>` worked, `ma <Tab>` did not.

Root cause: bash-completion's `_make` function (in `/usr/share/bash-completion/completions/make`, line 156) uses `$1` as the binary to invoke for harvesting Makefile targets:

```
$1 -npq __BASH_MAKE_COMPLETION__=1 ...
```

When completion is registered as `complete -F _make ma`, `$1` becomes `ma`. Aliases do **not** expand inside shell functions, so the call fails silently and `COMPREPLY` ends up empty. The generic `alias_completion` helper already in the file can't fix this because it copies the `complete` definition verbatim — it doesn't rewrite the `$1` the inner function sees.

## Change applied
**File:** `/home/eug/.bashrc`

**Location:** immediately after the `alias_completion` call (around line 369), before the NVM block.

**What was added:** an installer that loads `_make` if needed (handling both `_completion_loader` and the newer `_comp_load` names, plus a direct-source fallback) and then registers a thin wrapper `_ma` for the `ma` alias. The wrapper passes literal `make` as `$1` to `_make`, sidestepping the alias-in-function problem:

```bash
_install_ma_completion() {
    if ! complete -p make &>/dev/null; then
        if declare -F _completion_loader &>/dev/null; then
            _completion_loader make 2>/dev/null
        elif declare -F _comp_load &>/dev/null; then
            _comp_load make 2>/dev/null
        fi
    fi
    if ! declare -F _make &>/dev/null; then
        for f in /usr/share/bash-completion/completions/make \
                 /etc/bash_completion.d/make; do
            [ -r "$f" ] && . "$f" && break
        done
    fi
    if declare -F _make &>/dev/null; then
        _ma() { _make make "$2" "$3"; }
        complete -F _ma ma
    fi
}
_install_ma_completion
unset -f _install_ma_completion
```

The `alias ma="make"` line itself is at line 292 (in the Maven section), unchanged.

## Why this design
- Reuses the existing `_make` function rather than duplicating target-harvesting logic.
- Idempotent: skips registration if `_make` can't be loaded.
- Works across bash-completion versions (pre/post `_completion_loader` → `_comp_load` rename).
- No invasive changes to the existing `alias_completion` machinery — it still handles every other alias the same way.

## Verification (already done)
- `complete -p ma` → `complete -F _ma ma`
- Simulated `_ma` invocation against the real Makefile in `~/dev/projects/my/renovation` returned all 12 targets (build, clean, compile, …) — same set as `make <Tab>`.
- User confirmed `ma <Tab>` now works in a fresh shell.