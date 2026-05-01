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
### Work with backlog.md



#### Action 	Example Command
Run every 5 minutes:
1.
- /loop 5m check if the deploy finished
- /loop 5m "Execute [Task A], [Task B], and [Task C]. Keep track of how many times you have done this in this session. Once you have completed all three tasks for the 3rd time, run the command /loop --cancel to stop this recurring task."
- /loop 1h "/review-pr 1234"
- /loop "run tests" --every 1h --for 1d
- /loop --cancel [ID]


2. Parallel Sessions (Native Worktrees)
   This is the most efficient way to keep each task in its own isolated branch and file context.
   
   Terminal 1: claude -w task-1
   Terminal 2: claude -w task-2
   Terminal 3: claude -w task-3
   
   Using the --worktree (or -w) flag automatically creates a new branch and a isolated directory in .claude/worktrees/, ensuring the agents don't overwrite each other's


3. // settings.json
   { "env": { "CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS": "1" } }
   
   "teammateMode": "tmux" in settings.local.json

   Create an agent team.
   Spawn three agents in parallel. Load the TeamCreate tool schema and spawn the agent team

   Agent 1 — docker-expert — implement TASK-6.1:                                                                                                                                                                                                               
   Read backlog task TASK-6.1 via the backlog MCP, move it to In Progress, implement it.                                                                                                                                                                     
   When ready to validate, run:                                                                                                                                                                                                                            
   flock /tmp/make-build.lock make build
   
   Agent 2 — java-architect — implement TASK-6.2:                                                                                                                                                                                                            
   Read backlog task TASK-6.2 via the backlog MCP, move it to In Progress, implement it.                                                                                                                                                                     
   When ready to validate, run:                                                                                                                                                                                                                            
   flock /tmp/make-build.lock make build
   
   Agent 3 — spring-boot-engineer — implement TASK-6.3:                                                                                                                                                                                                        
   Read backlog task TASK-6.3 via the backlog MCP, move it to In Progress, implement it.                                                                                                                                                                   
   When ready to validate, run:                                                                                                                                                                                                                              
   flock /tmp/make-build.lock make build
   
   NOTE: flock serializes make build — if two agents finish at the same time,                                                                                                                                                                                  
   the second waits for the first to complete before running. This avoids docker compose collisions.


4. I'd like to create a feature description for backlog.md.
   Help me to create description by interactivly interviewing me.
   And evenually output me on the screen the description.

   This is draft of the feature description:
   I want to implement a User registration by username and password for my application.
   It should use keycloak up and running locally with docker for handling these.
   After registration, can login to application.
   Also User should have a posibility to edit it's name.
   User should have uqiue name which allows only symbols:[a-zA-Z0-9_] and name not less then 5 symbols not more then 15.
   User can add it's avatar picture (png or jpg).
   User password should be strong, not less then 8 symbols and defined by security rules you propose.
        
   ===

   ===
  
   save this as a backlog task via the Backlog MCP
   
   ===
    
   Use the backlog mcp to decompose this `Task-6` into smaller subtasks. 
   Each task must have clear 'Acceptance Criteria' in its description.

   ===

   which tasks can be run in parallel by different agents?

   ===

   Read backlog task TASK-6.1 via the backlog MCP, move it to In Progress, implement it.

   ===

   I want to implement a [Feature Name, e.g., REST API for User Profiles]. 
   Based on our Java project structure, use the backlog tool to decompose this 
   into 10-15 small, atomic tasks. Each task must have clear 'Acceptance Criteria'  
   in its description.

   ===

   I need to add a new feature.
   We need to implement a JWT authentication filter for our Spring Security setup.
   Create a task for this in the backlog.
      
   ===

   /loop "Check the backlog for the next 'TODO' task. Start it, implement the Java code, run tests to verify, and mark it as 'DONE'. Continue until all tasks for this feature are complete."

   ===

   Create a task for 'Database Migration' and include three subtasks as a checklist:
   1. Create SQL script, 2. Update Entity classes, 3. Run migration test.

   ===

   Create a parent task called 'API Security' and 4 subtasks as individual 
   backlog items. Link them together.
   
   ===

   Read backlog task TASK-7 via the backlog MCP, move it to In Progress, implement it.
   When ready to validate, run: flock /tmp/make-build.lock make clean build
   When validation is successful commit changes (include also this task Markdown file)

   ====

   From backlog.md read sequentially and complete sequentially task-6.4, task-6.5.

   For each task:
   1. Move it to `In Progress`
   2. When ready to validate, run: flock /tmp/make-build.lock `make clean build`
   3. When validation is successful commit changes (include also this task Markdown file)
   4. Do not move to the next task until the current one is fully complete, validation is successful and changes commited.
   
   Start with task-6.4.

5. Any issues with the team:
   $> ps aux | grep 'claude.*--agent-id'
   $> pgrep -la claude
   $> kill the processes
