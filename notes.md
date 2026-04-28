 #### Prompt which created 2 pane
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

todo:
- Open playwright in active chrome browser:
https://playwright.dev/mcp/configuration/browser-extension
* Add e2e tests for up backend and frontend and check:  
* * login
* * person (get,post,edit,delete)
* * navigate through pages (1-2-3-...)
* * search person by email
* * logout
