 ### Notes 

Prompt which created 2 pane 
 1) Create new `tmux new-session -s claude-team` session
 2) Create and run a team with 2 agent to refactor the frontend and backend.                                                                                                                                                                                   
 Use agents: @"backend-naming-reviewer (agent)" @"frontend-naming-reviewer (agent)"
 (but all time it was idled)

---
1) Create new `tmux new-session -s claude` session
2) Create and run a team with 1 agent to refactor frontend.                                                                                                                               
   Use agents: @"frontend-naming-reviewer (agent)"
   (but all time it was idled, not responded)

---
Use:
1) /loop "Check the backlog for the next 'TODO' task. Start it, implement the Java code, run tests to verify, and mark it as 'DONE'. Continue until all tasks for this feature are complete."
2) Create a parent task called 'API Security' and 4 subtasks as individual backlog items. Link them together.

---
/loop 5m "Execute [Task A], [Task B], and [Task C]. Keep track of how many times you have done this in this session. Once you have completed all three tasks for the 3rd time, run the command /loop --cancel to stop this recurring task."

### Action 	Example Command
Run every 5 minutes:
/loop 5m check if the deploy finished

Run every hour:
/loop 1h "/review-pr 1234"

Set a duration:
/loop "run tests" --every 1h --for 1d

Cancel a loop:
/loop --cancel [ID]

---
1. Parallel Sessions (Native Worktrees)
   This is the most efficient way to keep each task in its own isolated branch and file context.
   Medium
   Medium
   +1
   Terminal 1: claude -w task-1
   Terminal 2: claude -w task-2
   Terminal 3: claude -w task-3
   Terminal 4: claude -w task-4
   Terminal 5: claude -w task-5
   Using the --worktree (or -w) flag automatically creates a new branch and a isolated directory in .claude/worktrees/, ensuring the agents don't overwrite each other's

2. // settings.json
   { "env": { "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1" } }

3. "I have 5 separate tasks: [list your tasks]. Create an agent team with 5 teammates. Assign one task to each teammate and have them report progress back to you as the lead."

----
I'd like to create a feature description for backlog.md.                                                                                                                                                                                                    
Help me to create description by interviewing me.                                                                                                                                                                                                           
And evenually write the description.

This is draft of the feature description:  

---
Any issues with the team:
$> ps aux | grep 'claude.*--agent-id'
$> pgrep -la claude
$> kill the processes
