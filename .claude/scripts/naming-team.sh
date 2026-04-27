#!/usr/bin/env bash
# Launches two Claude naming-reviewer agents in separate tmux panes.
# Usage: ./.claude/scripts/naming-team.sh
set -euo pipefail

ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
SESSION="naming-review"

if tmux has-session -t "$SESSION" 2>/dev/null; then
  echo "Session '$SESSION' already exists. Attaching..."
  tmux attach -t "$SESSION"
  exit 0
fi

tmux new-session -d -s "$SESSION" -x 220 -y 50

# Pane 0: backend naming reviewer
tmux send-keys -t "$SESSION:0" \
  "cd '$ROOT' && claude --agent backend-naming-reviewer --print 'Review and fix all variable naming in backend/src/main/java/ per Java conventions. Work file by file, list old→new renames, apply edits, then run mvn compile.'" Enter

# Pane 1: frontend naming reviewer
tmux split-window -h -t "$SESSION:0"
tmux send-keys -t "$SESSION:0.1" \
  "cd '$ROOT' && claude --agent frontend-naming-reviewer --print 'Review and fix all variable naming in frontend/src/ per TypeScript/React conventions. Work file by file, list old→new renames, apply edits, then run npm run build.'" Enter

tmux select-pane -t "$SESSION:0.0"
tmux attach -t "$SESSION"
