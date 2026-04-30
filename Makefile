# Makefile for claude-ai-spring-boot

.PHONY: clean updateFrontend buildBackend buildFrontend acceptanceTest build dockerAll dockerDown \
        ghList ghView defaultMessage message help ciCheck

# Function to execute commands sequentially with success and failure messages
# Usage: $(call execute_commands,command1 && command2 && .. && commandN, success_msg, fail_msg)
define execute_commands
@( $(1) ) && $(MAKE) message $(2) || { $(MAKE) message $(3); exit 1; }
endef

clean:
	@echo "### Cleaning (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		docker compose down ; \
		docker rmi -f claude-ai-spring-boot-backend:latest claude-ai-spring-boot-frontend:latest 2>/dev/null || true && \
		cd backend && mvn clean && \
		cd ../frontend && rm -rf dist,\
		"✅ CLEAN SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ CLEAN FAILED (claude-ai-spring-boot) ❌")

updateFrontend:
	@echo "### Updating frontend (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		cd frontend && rm -rf dist node_modules package-lock.json && npx npm-check-updates -u && npm install,\
		"✅ UPDATE FRONTEND SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ UPDATE FRONTEND FAILED (claude-ai-spring-boot) ❌")

buildBackend:
	@echo "### Building backend (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		cd backend && mvn clean verify,\
		"✅ BUILD BACKEND SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ BUILD BACKEND FAILED (claude-ai-spring-boot) ❌")

buildFrontend:
	@echo "### Building frontend (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		cd frontend && npm install && npm run build,\
		"✅ BUILD FRONTEND SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ BUILD FRONTEND FAILED (claude-ai-spring-boot) ❌")

acceptanceTest:
	@echo "### Running acceptance tests (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		cd e2e && npm ci && npm test,\
		"✅ ACCEPTANCE TESTS SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ ACCEPTANCE TESTS FAILED (claude-ai-spring-boot) ❌")

build:
	@echo "### Full build (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		$(MAKE) buildBackend && \
		$(MAKE) buildFrontend && \
		docker compose build && \
		docker compose up -d --wait && \
		$(MAKE) acceptanceTest && \
		docker compose down,\
		"✅ BUILD SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ BUILD FAILED (claude-ai-spring-boot) ❌")

ciCheck: # if it's not working try `make updateFrontend`
	@echo "### CI simulation (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		cd backend && mvn clean verify && \
		cd .. && cd frontend && npm ci && npm run build && \
		cd .. && docker compose build --no-cache && \
		docker compose up -d --wait && \
		cd e2e && npm ci && npm test && \
		cd .. && docker compose down,\
		"✅ CI CHECK SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ CI CHECK FAILED (claude-ai-spring-boot) ❌")

dockerAll:
	@echo "### Building and docker up locally (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		$(MAKE) build && \
		$(MAKE) message "✅ DOCKER COMPOSE RUNNING (claude-ai-spring-boot) ... ⏩⏩⏩" && \
		docker compose up,\
		"✅ DOCKER COMPOSE ALL SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ DOCKER COMPOSE ALL FAILED (claude-ai-spring-boot) ❌")

dockerDown:
	docker compose down

ghList:
	gh run list --limit 5

ghView:
	gh run view --web

###
# Common commands
defaultMessage:
	make message "🔔 Execution finished (claude-ai-spring-boot) 🔔"

message:
	@echo "=============================================="
	@echo "============= MESSAGE (TELEGRAM) ============="
	@echo "=============================================="
	@if [ -z "$(filter-out $@,$(MAKECMDGOALS))" ]; then \
		echo "Usage: make message \"Your message here\""; \
		exit 1; \
	fi
	@echo "Sending message:"
	@echo "$(filter-out $@,$(MAKECMDGOALS))"
	@echo
	bash -c ' \
		tn() { \
			local msg="$${*:-🔔 Job finished (claude-ai-spring-boot) 🔔}"; \
			local api="https://api.telegram.org/bot$${NOTIFICATION_TELEGRAM_BOT_TOKEN}/sendMessage"; \
			curl -sS -X POST "$$api" \
				--data "chat_id=$${NOTIFICATION_TELEGRAM_CHAT_ID}" \
				--data-urlencode "text=$$msg"; \
		}; \
		tn "$(filter-out $@,$(MAKECMDGOALS))" \
	'
	@echo
%:
	@:

help:
	@echo ""
	@echo "╔═══════════════════════════════════════════════════════════════════╗"
	@echo "║          claude-ai-spring-boot - Makefile Commands                ║"
	@echo "╚═══════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "🔨 Build Targets:"
	@echo "  clean             - Docker down, remove images, mvn clean, reinstall frontend deps"
	@echo "  updateFrontend    - Update frontend deps (npm-check-updates -u && npm install)"
	@echo "  buildBackend      - Build backend with Maven (mvn clean verify, enforces JaCoCo)"
	@echo "  buildFrontend     - Build frontend (npm install && npm run build)"
	@echo "  build             - Full build: backend + frontend + docker rebuild + e2e (--wait)"
	@echo "  ciCheck           - Strict CI simulation: no-cache docker build, npm ci, mvn verify"
	@echo ""
	@echo "🧪 Test Targets:"
	@echo "  acceptanceTest    - Run Cucumber+Playwright e2e tests (stack must be running)"
	@echo ""
	@echo "🐳 Docker Targets:"
	@echo "  dockerAll        - Build backend + frontend, then docker compose up (foreground)"
	@echo "  dockerDown       - Stop and remove all Docker services"
	@echo ""
	@echo "🐙 GitHub Actions:"
	@echo "  ghList            - List last 5 GitHub Actions runs"
	@echo "  ghView            - Open latest GitHub Actions run in browser"
	@echo ""
	@echo "📬 Notification Targets:"
	@echo "  defaultMessage    - Send default notification to Telegram"
	@echo "  message <text>    - Send custom notification to Telegram"
	@echo "                      Example: make message \"Build completed\""
	@echo ""
	@echo "  Requires env vars: NOTIFICATION_TELEGRAM_BOT_TOKEN, NOTIFICATION_TELEGRAM_CHAT_ID"
	@echo ""
	@echo "❓ Other:"
	@echo "  help              - Show this help message"
	@echo ""
