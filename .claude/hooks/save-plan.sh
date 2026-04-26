#!/usr/bin/env bash
# PostToolUse (Write): Copy new plan files to .claude/docs/blackbox/plans/ with a
# dated, human-readable name. Fires whenever Claude writes to ~/.claude/plans/.
set -uo pipefail

input=$(cat)
tool_name=$(echo "$input" | jq -r '.tool_name // ""'  2>/dev/null) || tool_name=""
file_path=$(echo "$input" | jq -r '.tool_input.file_path // ""' 2>/dev/null) || file_path=""
content=$(echo  "$input" | jq -r '.tool_input.content // ""'    2>/dev/null) || content=""

[ "$tool_name" != "Write" ] && exit 0

PLANS_SRC="$HOME/.claude/plans"
[[ "$file_path" != "$PLANS_SRC/"* ]] && exit 0
[ -z "$content" ] && exit 0

# --- title extraction ---
# 1. First "# " heading in content
title=$(printf '%s' "$content" | grep -m1 "^# " | sed 's/^# //' | tr -d '\r' || true)

# 2. Fallback: filename minus the last two hyphenated words (poetic suffix)
if [ -z "$title" ]; then
    stem=$(basename "$file_path" .md)
    title=$(echo "$stem" | rev | cut -d'-' -f3- | rev | tr '-' ' ')
fi

# 3. Last resort
[ -z "$title" ] && title="untitled-plan"

# Sanitise: keep alphanumerics, spaces, hyphens, underscores; cap at 80 chars
title=$(echo "$title" | sed 's/[^a-zA-Z0-9 _-]//g' | tr -s ' ' | cut -c1-80)

# --- write dated copy ---
timestamp=$(date +'%Y-%m-%d %H:%M')
dest_dir="$CLAUDE_PROJECT_DIR/.claude/docs/blackbox/plans"
dest_file="$dest_dir/${timestamp} ${title}.md"

mkdir -p "$dest_dir"
# Remove any prior version of this plan (any date prefix, same title suffix)
find "$dest_dir" -maxdepth 1 -name "* ${title}.md" -delete 2>/dev/null || true
printf '%s' "$content" > "$dest_file" 2>/dev/null || true

exit 0
