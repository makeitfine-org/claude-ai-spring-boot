### Todo (tasks)
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
Make update in settins.json instead of settins.local.json

---
Deploy to AWS (as lambda or talk with AI how's better)
- Write me a detailed plan (step by step) how to deploy it to aws
- Propose the cheapest aws technologies (lambda, rds)
- I'd like to deploy it with code no manula clicking on  UI.
  So write terraform or propose something else.

---
[check] While `make build` docker are being created with some port exposing in docker compose.
And this port can overlap with skaffold(s) backend and frontend instances in minikube ports.
Fix these for `make build` works normally while minikube running 

---
[check] Improve CLAUDE.md files by running `makefile build` after adding new features 

---
Write multi-user support for they can register with login, password 
and get confirmation on email.
Save it in cognito or propose something else

---
Create a task in the backlog for described problems solution

---
I'd like to introduce Docker-based AWS LocalStack for my application to emulate 
a realistic environment for your production application on safe, local infrastructure.

---
I want to implement a User registration by username and password for my application.
- It should use keycloak up and running locally with docker for handling these.
- After registration, can login to application. 
- Also User should have a posibility to edit it's name.
  User should have uqiue name which allows only symbols:[a-zA-Z0-9_] and name not less then 5 symbols not more then 15.
  User can add it's avatar picture (png or jpg).
  User password should be strong, not less then 8 symbols and defined by security rules you propose.

---
Set timeout for build in `github actions` CI/CD

---
connect to PC Claude code from phone and handle by approving
(share termina with claude/session in web?)

---
Makefile ciCheck