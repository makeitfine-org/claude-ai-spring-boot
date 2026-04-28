# Makefile for claude-ai-spring-boot

.PHONY: clean cleanShallow updateFrontend buildBackend buildFrontend acceptanceTest build docker_all docker_down \
        ghList ghView default_message message help

# Function to execute commands sequentially with success and failure messages
# Usage: $(call execute_commands,command1 && command2 && .. && commandN, success_msg, fail_msg)
define execute_commands
@( $(1) ) && $(MAKE) message $(2) || { $(MAKE) message $(3); exit 1; }
endef

clean:
	@echo "### Cleaning (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		docker compose down ; \
		docker rmi -f claude-ai-spring-boot-app:latest claude-ai-spring-boot-frontend:latest 2>/dev/null || true && \
		cd backend && mvn clean && \
		cd ../frontend && rm -rf dist node_modules package-lock.json,\
		"✅ CLEAN SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ CLEAN FAILED (claude-ai-spring-boot) ❌")

cleanShallow:
	@echo "### Cleaning (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		docker compose down ; \
		docker rmi -f claude-ai-spring-boot-app:latest claude-ai-spring-boot-frontend:latest 2>/dev/null || true && \
		cd backend && mvn clean && \
		cd ../frontend && rm -rf dist,\
		"✅ CLEAN SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ CLEAN FAILED (claude-ai-spring-boot) ❌")

updateFrontend:
	cd frontend && rm -rf dist node_modules package-lock.json && npx npm-check-updates -u && npm install

buildBackend:
	@echo "### Building backend (claude-ai-spring-boot) ..."
	cd backend && mvn install

buildFrontend:
	@echo "### Building frontend (claude-ai-spring-boot) ..."
	cd frontend && npm install && npm run build

acceptanceTest:
	@echo "### Running acceptance tests (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		cd e2e && npm install && npm test,\
		"✅ ACCEPTANCE TESTS PASSED (claude-ai-spring-boot) ✅",\
		"❌ ACCEPTANCE TESTS FAILED (claude-ai-spring-boot) ❌")

build:
	@echo "### Full build (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		$(MAKE) buildBackend && \
		$(MAKE) buildFrontend && \
		docker compose up -d && \
		cd e2e && npm install && npm test && \
		cd .. && docker compose down,\
		"✅ BUILD SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ BUILD FAILED (claude-ai-spring-boot) ❌")

docker_all:
	@echo "### Building and docker up locally (claude-ai-spring-boot) ..."
	$(call execute_commands,\
		$(MAKE) buildBackend && \
		$(MAKE) buildFrontend && \
		docker compose down && \
		$(MAKE) message "✅ DOCKER COMPOSE RUNNING (claude-ai-spring-boot) ... ⏩⏩⏩" && \
		docker compose up,\
		"✅ DOCKER COMPOSE ALL SUCCESSFUL (claude-ai-spring-boot) ✅",\
		"❌ DOCKER COMPOSE ALL FAILED (claude-ai-spring-boot) ❌")

docker_down:
	docker compose down

ghList:
	gh run list --limit 5

ghView:
	gh run view --web

###
# Common commands
default_message:
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
	@echo "  cleanShallow      - Same as clean but skip reinstalling frontend node_modules"
	@echo "  updateFrontend    - Update frontend deps (npm-check-updates -u && npm install)"
	@echo "  buildBackend      - Build backend with Maven (cd backend && mvn install)"
	@echo "  buildFrontend     - Build frontend (npm install && npm run build)"
	@echo "  build             - Full build: backend + frontend + e2e tests (spins docker up/down)"
	@echo ""
	@echo "🧪 Test Targets:"
	@echo "  acceptanceTest    - Run Cucumber+Playwright e2e tests (stack must be running)"
	@echo ""
	@echo "🐳 Docker Targets:"
	@echo "  docker_all        - Build backend + frontend, then docker compose up (foreground)"
	@echo "  docker_down       - Stop and remove all Docker services"
	@echo ""
	@echo "🐙 GitHub Actions:"
	@echo "  ghList            - List last 5 GitHub Actions runs"
	@echo "  ghView            - Open latest GitHub Actions run in browser"
	@echo ""
	@echo "📬 Notification Targets:"
	@echo "  default_message   - Send default notification to Telegram"
	@echo "  message <text>    - Send custom notification to Telegram"
	@echo "                      Example: make message \"Build completed\""
	@echo ""
	@echo "  Requires env vars: NOTIFICATION_TELEGRAM_BOT_TOKEN, NOTIFICATION_TELEGRAM_CHAT_ID"
	@echo ""
	@echo "❓ Other:"
	@echo "  help              - Show this help message"
	@echo ""
