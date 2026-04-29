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
- Write me a detailed plan (step by step) how to deploy it to aws
- Propose the cheapest aws technologies (lambda, rds)
- I'd like to deploy it with code no manula clicking on  UI.
  So write terraform or propose something else.


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

---
Improve CLAUDE.md files by running `makefile build` after adding new features 

---
Write multi-user support for they can register with login, password 
and get confirmation on email.
Save it in cognito or propose something else

---

Create e2e tests with `skaffold` to check that backend and frontend works fine on K8s 
the same as they work on `docker compose` 

---

1) When run `skaffold dev` on backend I get long execution of such command:

1396908a28e9: Pull complete
Digest: sha256:8e7dc4215c70f922e798c9f8aafa0a3734ca386342427b3dbb17cecc4a429c8e
Status: Downloaded newer image for maven:3.9-eclipse-temurin-21
---> 8a14c71c9922
Step 2/11 : WORKDIR /app
---> Running in 489cb2460db0
---> b194a847f0ba
Step 3/11 : COPY pom.xml .
---> 4550958f128e
Step 4/11 : RUN mvn -B -ntp dependency:go-offline -q
---> Running in 9f2d74d6bd02

2) When run `skaffold dev` on backend I get long execution of such command:
   to select an already authenticated account to use.                                                                             
   Sending build context to Docker daemon  354.5MB                                                                                
   Step 1/11 : FROM node:22-alpine AS builder                                                                                     
   ---> 04a4709b55a8                                             
   Step 2/11 : WORKDIR /app                                       
   ---> Using cache                                              
   ---> d1158aeef2b2                                             
   Step 3/11 : COPY package.json package-lock.json ./                                                                             
   ---> Using cache                                              
   ---> e36b20d2db12                                             
   Step 4/11 : RUN npm install                                    
   ---> Using cache                                              
   ---> 8a21003f7cb5                                             
   Step 5/11 : COPY . .                                           
   ---> 71c8b96f076b
   Step 6/11 : RUN npm run build
   ---> Running in 661991431d10

3) When run `skaffold dev` on backend and frontend I get in log:

   to select an already authenticated account to use.                                                                                                                                                                                                            
   ERROR: (gcloud.auth.docker-helper) There was a problem refreshing your current auth tokens: ('invalid_grant: Bad Request', {'error': 'invalid_grant', 'error_description': 'Bad Request'})                                                                    
   Please run:

$ gcloud auth login

to obtain new credentials.

If you have already logged in with a different account, run:

$ gcloud config set account ACCOUNT

to select an already authenticated account to use.                                                                                                                                                                                                            
ERROR: (gcloud.auth.docker-helper) There was a problem refreshing your current auth tokens: ('invalid_grant: Bad Request', {'error': 'invalid_grant', 'error_description': 'Bad Request'})                                                                    
Please run:

$ gcloud auth login

to obtain new credentials.

If you have already logged in with a different account, run:

$ gcloud config set account ACCOUNT

to select an already authenticated account to use.                                                                                                                                                                                                            
ERROR: (gcloud.auth.docker-helper) There was a problem refreshing your current auth tokens: ('invalid_grant: Bad Request', {'error': 'invalid_grant', 'error_description': 'Bad Request'})                                                                    
Please run:

$ gcloud auth login

to obtain new credentials.

If you have already logged in with a different account, run:

$ gcloud config set account ACCOUNT

to select an already authenticated account to use.

---
Create a task in the backlog for described problems solution

---

After task completion save in the end of task file the summary which Claude code provided.

For example after completion task `task-5` claude code give summary:

● Here's a summary of what was done for task-5:

Files changed:

┌────────────────────────┬───────────────────────────────────────────────────────────────────────────────────────────────────────┐                                                                                                                          
│          File          │                                                Change                                                 │
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤                                                                                                                          
│ backend/Dockerfile     │ Added BuildKit cache mount for /root/.m2 — Maven deps persist across rebuilds                         │
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤                                                                                                                        
│ frontend/Dockerfile    │ Added BuildKit cache mount for /root/.npm, switched npm install → npm ci --prefer-offline             │                                                                                                                          
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤                                                                                                                          
│ frontend/.dockerignore │ Created — excludes node_modules/, dist/, .git/, etc. (shrinks build context from ~354 MB to a few MB) │                                                                                                                          
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤                                                                                                                          
│ backend/skaffold.yaml  │ Added useBuildkit: true                                                                               │                                                                                                                        
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤                                                                                                                          
│ frontend/skaffold.yaml │ Added useBuildkit: true                                                                               │
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤                                                                                                                          
│ ~/.docker/config.json  │ Removed europe-docker.pkg.dev: gcloud credHelper — eliminates the invalid_grant noise                 │
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤                                                                                                                          
│ backend/CLAUDE.md      │ Documented the Skaffold dev build optimizations                                                       │
├────────────────────────┼───────────────────────────────────────────────────────────────────────────────────────────────────────┤                                                                                                                          
│ frontend/CLAUDE.md     │ Documented the Skaffold dev build optimizations                                                       │
└────────────────────────┴───────────────────────────────────────────────────────────────────────────────────────────────────────┘

AC #4 and #5 (before/after timing numbers) can't be checked without running skaffold dev live — the task is marked complete for all the structural criteria. The key outcome: after the first cold build populates the BuildKit cache, subsequent skaffold  
dev iterations will skip Maven dependency resolution entirely on the backend, and the frontend will transfer a tiny build context instead of 354 MB.

So save it must have been saved in the end of file `task-5-....md`

