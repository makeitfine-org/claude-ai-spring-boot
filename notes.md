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

### Todo (tasks)
✅ Open playwright in active chrome browser:
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

---
After claude code completed task it writes resume what it did and show it on the terminal.
Create hook that saves it to the file `audit.md` file also.
---
✅ Create Makefile take as a template renovation Makefile
gh run list --limit 5
gh run view --log
---
Play around switching from plan -> edition in Claude Code after plan created.

---
Add Makefile to README.md, CLAUDE.md?

---
Increase speed of creation of frontend/Dockerfile (cause backend/Docker is fast)

---
✅ Think of not to exclude:
`rm -rf dist node_modules package-lock.json` from `Make clean`

---
Make update in settins.json instead of settins.local.json

---
✅ x Rename in Makefile (for simple typing):
cleanShallow -> clean.shallow

---
Rename
<artifactId>claude-ai-spring-boot</artifactId>
to
<artifactId>backend</artifactId>

---
✅ Add alias to ~/.bashrc of 'make' as 'ma'

---
Deploy to AWS (as lambda or talk with AI how's better)

---
backlog.md work with

---
Make changes for `make build` to execute without stop Skaffold of (back and front)

---
In docker
COPY target/backend-1.0.1.jar app.jar
change `1.0.1` to `*`

---
While `make build` docker are being created with some port exposing in docker compose.
And this port can overlap with skaffold(s) backend and frontend instances in minikube ports.
Fix these for `make build` works normally while minikube running 

---

While `skaffold dev` in frontend and backend there are multiple errors, fix it.

to select an already authenticated account to use.                                                                                                                                                                                                            
ERROR: (gcloud.auth.docker-helper) There was a problem refreshing your current auth tokens: ('invalid_grant: Bad Request', {'error': 'invalid_grant', 'error_description': 'Bad Request'})                                                                    
Please run:

$ gcloud auth login

to obtain new credentials.

If you have already logged in with a different account, run:

$ gcloud config set account ACCOUNT

to select an already authenticated account to use.                                                                                                                                                                                                          
                                                  
---
Seems skaffold on `backend` doesn't reload docker on changes in code
fix it