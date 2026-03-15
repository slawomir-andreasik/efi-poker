.DEFAULT_GOAL := help
SHELL := /bin/bash
APP_VERSION := $(shell grep -oP '(?<=version=).+' version.properties)
IMAGE_REPO := ghcr.io/slawomir-andreasik/efi-poker

##@ Development
.PHONY: dev dev-stop docker-dev docker-dev-stop status

dev:  ## Start dev environment (DB + backend + frontend)
	@pkill -9 -f "EfiPokerApplication|bootRun" 2>/dev/null || true
	@sleep 1
	docker compose -f docker-compose.yml up -d postgres
	@echo "Waiting for PostgreSQL..."
	@until docker compose -f docker-compose.yml exec -T postgres pg_isready -U efipoker > /dev/null 2>&1; do sleep 1; done
	@echo "PostgreSQL ready."
	@set -a && source .env && set +a && \
		nohup ./gradlew :backend:bootRun > /tmp/efi-server.log 2>&1 & echo $$! > .dev-server.pid
	@echo "Waiting for backend on :8080..."
	@ready=0; for i in $$(seq 1 45); do \
		if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then \
			echo "Backend ready."; ready=1; break; \
		fi; \
		sleep 2; \
	done; \
	if [ $$ready -eq 0 ]; then echo "WARNING: Backend not ready after 90s. Check: tail -f /tmp/efi-server.log"; fi
	@cd frontend && nohup bun run dev --port 5173 > /tmp/efi-client.log 2>&1 & echo $$! > .dev-client.pid
	@echo ""
	@echo "Dev environment started"
	@echo ""
	@echo "  Frontend:  http://localhost:5173"
	@echo "  Backend:   http://localhost:8080"
	@echo "  DB:        localhost:${POSTGRES_PORT:-5432}"
	@echo ""
	@echo "Logs:"
	@echo "  tail -f /tmp/efi-server.log"
	@echo "  tail -f /tmp/efi-client.log"
	@echo ""
	@echo "Stop:  make dev-stop"

dev-stop:  ## Stop dev environment
	@for f in .dev-server.pid .dev-client.pid; do \
		if [ -f $$f ]; then kill $$(cat $$f) 2>/dev/null; rm -f $$f; fi; \
	done
	@pkill -9 -f "EfiPokerApplication|bootRun" 2>/dev/null || true
	docker compose -f docker-compose.yml down
	@echo "Dev environment stopped."

docker-dev:  ## Start dev in Docker (build images + run all)
	docker compose -f compose.dev.yaml up --build -d
	@echo ""
	@echo "Dev environment started (Docker)"
	@echo ""
	@echo "  App:   http://localhost:5173"
	@echo "  DB:    localhost:${POSTGRES_PORT:-5432}"
	@echo "  Login: admin / changeme"
	@echo ""
	@echo "Stop: make docker-dev-stop"

docker-dev-stop:  ## Stop Docker dev environment
	docker compose -f compose.dev.yaml down

status:  ## Show running services
	@docker compose -f docker-compose.yml ps 2>/dev/null; docker compose -f compose.dev.yaml ps 2>/dev/null; true

##@ Build
.PHONY: build api-generate

build: server-build client-build  ## Full build (backend + frontend)

api-generate:  ## Regenerate API clients from OpenAPI specs
	./gradlew :api:openApiGenerate :api:generateTypescriptClient :api:copyFlattenedSpec

##@ Backend
.PHONY: server-build server-test server-test-unit server-test-integration server-run spotless

server-build:  ## Gradle build (tests + formatting + coverage)
	./gradlew :backend:check

server-test:  ## Backend tests only
	./gradlew :backend:test

server-test-unit:  ## Backend unit tests only (fast, no Spring)
	./gradlew :backend:unitTest

server-test-integration:  ## Backend integration tests only (Spring + DB)
	./gradlew :backend:integrationTest

server-run:  ## Start Spring Boot dev server (loads .env)
	@set -a && source .env && set +a && ./gradlew :backend:bootRun

spotless:  ## Auto-fix Java formatting
	./gradlew spotlessApply

##@ Frontend
.PHONY: client-dev client-build client-test client-lint

client-dev:  ## Start Vite dev server
	cd frontend && bun run dev

client-build: api-generate  ## Production build (generates TS client first)
	cd frontend && VITE_APP_VERSION=$(APP_VERSION) bun run build

client-test:  ## Frontend tests
	cd frontend && bun run test

client-lint:  ## ESLint check
	cd frontend && bun run lint

##@ Docker
.PHONY: up-db db-reset

up-db:  ## Start PostgreSQL only
	docker compose -f docker-compose.yml up -d postgres

db-reset:  ## Reset database (drop volume + restart)
	docker compose -f docker-compose.yml down -v
	docker compose -f docker-compose.yml up -d postgres
	@until docker compose -f docker-compose.yml exec -T postgres pg_isready -U efipoker > /dev/null 2>&1; do sleep 1; done
	@echo "Database reset. Liquibase will re-run on next server start."

##@ Images
.PHONY: image-build image-push

image-build:  ## Build Docker images locally
	./gradlew :backend:bootBuildImage
	docker build --build-arg APP_VERSION=$(APP_VERSION) -t $(IMAGE_REPO)/frontend:$(APP_VERSION) -t $(IMAGE_REPO)/frontend:latest frontend/

image-push: image-build  ## Build and push images to GHCR
	docker push $(IMAGE_REPO)/backend:$(APP_VERSION)
	docker push $(IMAGE_REPO)/backend:latest
	docker push $(IMAGE_REPO)/frontend:$(APP_VERSION)
	docker push $(IMAGE_REPO)/frontend:latest

##@ Verify
.PHONY: pre-push verify

pre-push: spotless server-build client-lint client-test  ## Auto-fix formatting + run all checks

verify:  ## Full verification (health + tests)
	@echo "Running backend tests..."
	./gradlew :backend:test
	@echo "Running frontend tests..."
	cd frontend && bun run test
	@echo "Running frontend lint..."
	cd frontend && bun run lint
	@echo "All checks passed."

##@ Maintenance
.PHONY: lint clean

lint: spotless client-lint  ## All linters

clean:  ## Clean build artifacts
	./gradlew clean
	rm -rf frontend/dist frontend/node_modules

##@ Help
.PHONY: help
help:  ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-22s\033[0m %s\n", $$1, $$2}'
