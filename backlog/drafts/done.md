### Done (tasks)

✅ Create dark/light/auto themas for front-end

---
✅ Create Makefile take as a template renovation Makefile
gh run list --limit 5
gh run view --log

---
✅ Play around switching from plan -> edition in Claude Code after plan created.

---
✅ Add Makefile to README.md, CLAUDE.md?

---
✅ Increase speed of creation of frontend/Dockerfile (cause backend/Docker is fast)

---
✅ Think of not to exclude:
`rm -rf dist node_modules package-lock.json` from `Make clean`

---
✅ x Rename in Makefile (for simple typing):
cleanShallow -> clean.shallow

---
✅ Rename
<artifactId>claude-ai-spring-boot</artifactId>
to
<artifactId>backend</artifactId>

---
✅ Add alias to ~/.bashrc of 'make' as 'ma'

---
✅ backlog.md work with

---
✅ In docker
COPY target/backend-1.0.1.jar app.jar
change `1.0.1` to `*`

---
✅ While `skaffold dev` in frontend and backend there are multiple errors, fix it.

to select an already authenticated account to use.                                                                                                                                                                                                            
ERROR: (gcloud.auth.docker-helper) There was a problem refreshing your current auth tokens: ('invalid_grant: Bad Request', {'error': 'invalid_grant', 'error_description': 'Bad Request'})                                                                    
Please run:

$ gcloud auth login

to obtain new credentials.

If you have already logged in with a different account, run:

$ gcloud config set account ACCOUNT

to select an already authenticated account to use.                                                                                                                                                                                                          
                                                  
---
✅ Seems skaffold on `backend` doesn't reload docker on changes in code
fix it

---
✅ 1) When run `skaffold dev` on backend I get long execution of such command:

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
