As I observe Claude Code works in such way:
1) I asked Claude Code to do something
2) Then Claude Code has been completing this
3) And in the end Claude Code outputs something like final summary

I'd like you to create a hook to append this summary into the end of `.claude/docs/blackbox/summary-audit.md` file.
Write the summary in such format:

## <YYYY-MM-DDTHH:mm:ssZ>
Prompt: <prompt>

Summary:
 <summary>


## <YYYY-MM-DDTHH:mm:ssZ>
● Prompt:
<prompt>

● Summary:
<summary>


Give the below examples as a base:

EXAMPLE 1:
after completion `Complete task-5` implementation Claude code has given a resume:
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

✻ Brewed for 39s

#### So you have to append to the file `summary-audit.md` such content:

## <YYYY-MM-DDTHH:mm:ssZ>
● Prompt:
Complete task-5

● Summary:
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


EXAMPLE 2:
after completion `Create plan to improve project` implementation Claude code has given a summary:
● All done. Here's what changed:

backend/Dockerfile — replaced with a multi-stage build:
- Stage 1 (maven:3.9-eclipse-temurin-21): caches dependencies, then builds the jar inside Docker — no host-side mvn needed
- Stage 2: copies via wildcard backend-*.jar — no hard-coded version string

backend/.dockerignore (new) — excludes target/, .git/, .idea/, *.md:
- Keeps the build context clean
- Skaffold's docker watcher now only fires on src/** and pom.xml changes, not on target/ writes

.claude/rules/version-bump-procedure.md — removed the obsolete "update Dockerfile" step (now 4 steps → 3)

Root README.md — removed the pre-build mvn package step from the E2E quick-start

backend/README.md — added dev-loop iteration time note (~2–5 min per change) under Kubernetes Deployment

No changes to skaffold.yaml, docker-compose.yml, or k8s manifests — they all continue to work as-is. TASK-2, 2.1, and 2.2 are complete and archived.

✻ Cooked for 29s

#### So you have to append to the file `summary-audit.md` such content:

## <YYYY-MM-DDTHH:mm:ssZ>
● Prompt:
Create plan to improve project

● Summary:
backend/Dockerfile — replaced with a multi-stage build:
- Stage 1 (maven:3.9-eclipse-temurin-21): caches dependencies, then builds the jar inside Docker — no host-side mvn needed
- Stage 2: copies via wildcard backend-*.jar — no hard-coded version string

backend/.dockerignore (new) — excludes target/, .git/, .idea/, *.md:
- Keeps the build context clean
- Skaffold's docker watcher now only fires on src/** and pom.xml changes, not on target/ writes

.claude/rules/version-bump-procedure.md — removed the obsolete "update Dockerfile" step (now 4 steps → 3)

Root README.md — removed the pre-build mvn package step from the E2E quick-start

backend/README.md — added dev-loop iteration time note (~2–5 min per change) under Kubernetes Deployment

No changes to skaffold.yaml, docker-compose.yml, or k8s manifests — they all continue to work as-is. TASK-2, 2.1, and 2.2 are complete and archived.


EXAMPLE 3:
after completion `Create plan to add improvements` plan creation Claude code has given a summary:
● Here are concrete improvements I'd propose for README.md:

1. Fix the make dockerAll description (line 25)                                                                                                                                                                                                             
   README says it "runs acceptance tests, then starts all three services." But per CLAUDE.md, make dockerAll is "Full build then docker compose up" — it's make build that runs e2e tests. Either the README is wrong or CLAUDE.md is. Reconcile to one truth.

2. Reconcile the K8s/repo-layout discrepancy with CLAUDE.md                                                                                                                                                                                                 
   README (lines 69, 76) correctly shows k8s/ lives inside both backend/ and frontend/. CLAUDE.md says "k8s/ → Kubernetes manifests (inside backend/)" which is half-right (also exists in frontend/). Fix CLAUDE.md to match the README — README is the more  
   accurate one here.

3. Add a "Stopping" note to Quick Start (after line 32)                                                                                                                                                                                                     
   make dockerAll runs in the foreground — a first-time user has no signal for how to stop or clean up. One line:                                                                                                                                              
   Stop with Ctrl+C; tear down with `make dockerDown`.

4. Drop or annotate "Default Credentials" (lines 55–60)                                                                                                                                                                                                     
   Putting test@example.com / password in a public README without context implies these are real account credentials. Either move to backend/README.md with a note like "Seeded by data.sql for local/dev only — not present in production" or remove from the
   root.

5. Collapse the duplicated E2E steps (lines 99–116)                                                                                                                                                                                                         
   The Quick Start already uses make dockerAll (which transitively runs e2e). The "Running Acceptance Tests" section then teaches a manual 3-step path. State the simple path first:                                                                           
   make acceptanceTest    # stack must already be running                                                                                                                                                                                                      
   …and keep the manual breakdown as a fallback for first-time setup.

6. Reverse the K8s access guidance (lines 195–207)                                                                                                                                                                                                          
   The current text leads with minikube tunnel (extra terminal, extra step) and treats portForward as a footnote. Skaffold's built-in port-forward already gives you localhost:3000/localhost:8080 for free — that should be the recommended path; minikube    
   tunnel is the fallback for skaffold run mode.

7. Add a "Prerequisites" line at the top of Quick Start                                                                                                                                                                                                     
   The Quick Start says "run make dockerAll" but Docker isn't listed as required at the root level (only buried in the K8s section). One line above the code block:                                                                                          
   Requires Docker (Compose v2). No local Java/Node needed — the build runs in containers.

8. Expand the Troubleshooting table (line 222)                                                                                                                                                                                                              
   One useful row to add given common failures here:                        
   | make build fails on mvn install | Stale Maven cache | make clean && make build |

Want me to apply any subset? 1, 2, 4, and 6 are the highest-value ones (correctness + safety). 3, 5, 7 are quality-of-life. 8 is optional.

✻ Prepared for 53s

#### So you have to append to the file `summary-audit.md` such content:
## <YYYY-MM-DDTHH:mm:ssZ>
● Prompt:
Create plan to add improvements

● Summary:
1. Fix the make dockerAll description (line 25)                                                                                                                                                                                                             
   README says it "runs acceptance tests, then starts all three services." But per CLAUDE.md, make dockerAll is "Full build then docker compose up" — it's make build that runs e2e tests. Either the README is wrong or CLAUDE.md is. Reconcile to one truth.

2. Reconcile the K8s/repo-layout discrepancy with CLAUDE.md                                                                                                                                                                                                 
   README (lines 69, 76) correctly shows k8s/ lives inside both backend/ and frontend/. CLAUDE.md says "k8s/ → Kubernetes manifests (inside backend/)" which is half-right (also exists in frontend/). Fix CLAUDE.md to match the README — README is the more  
   accurate one here.

3. Add a "Stopping" note to Quick Start (after line 32)                                                                                                                                                                                                     
   make dockerAll runs in the foreground — a first-time user has no signal for how to stop or clean up. One line:                                                                                                                                              
   Stop with Ctrl+C; tear down with `make dockerDown`.

4. Drop or annotate "Default Credentials" (lines 55–60)                                                                                                                                                                                                     
   Putting test@example.com / password in a public README without context implies these are real account credentials. Either move to backend/README.md with a note like "Seeded by data.sql for local/dev only — not present in production" or remove from the
   root.

5. Collapse the duplicated E2E steps (lines 99–116)                                                                                                                                                                                                         
   The Quick Start already uses make dockerAll (which transitively runs e2e). The "Running Acceptance Tests" section then teaches a manual 3-step path. State the simple path first:                                                                           
   make acceptanceTest    # stack must already be running                                                                                                                                                                                                      
   …and keep the manual breakdown as a fallback for first-time setup.

6. Reverse the K8s access guidance (lines 195–207)                                                                                                                                                                                                          
   The current text leads with minikube tunnel (extra terminal, extra step) and treats portForward as a footnote. Skaffold's built-in port-forward already gives you localhost:3000/localhost:8080 for free — that should be the recommended path; minikube    
   tunnel is the fallback for skaffold run mode.

7. Add a "Prerequisites" line at the top of Quick Start                                                                                                                                                                                                     
   The Quick Start says "run make dockerAll" but Docker isn't listed as required at the root level (only buried in the K8s section). One line above the code block:                                                                                          
   Requires Docker (Compose v2). No local Java/Node needed — the build runs in containers.

8. Expand the Troubleshooting table (line 222)                                                                                                                                                                                                              
   One useful row to add given common failures here:                        
   | make build fails on mvn install | Stale Maven cache | make clean && make build |
---