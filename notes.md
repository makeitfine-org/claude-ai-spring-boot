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

### Todo
Open playwright in active chrome browser:
https://playwright.dev/mcp/configuration/browser-extension
* Add e2e tests for up backend and frontend and check:
* Create acceptance criteria tests
* * login
* * person (get,post,edit,delete)
* * navigate through pages (1-2-3-...)
* * search person by email, city, first name, ... etc.
* * logout
---
Create dark/light/auto themas for front-end
--
After claude code completed task it writes resume what it did and show it on the terminal.
Create hook that saves it to the file `audit.md` file also.
---
Create Makefile take as a template renovation Makefile
gh run list --limit 5
gh run view --log