SHELL := /bin/bash
COMPOSE := docker compose
GRADLE  := ./gradlew

-include .env
export

ANDROID_HOME     ?= $(if $(wildcard /goinfre/$(USER)),/goinfre/$(USER)/android-sdk,$(HOME)/Android/Sdk)
ANDROID_AVD_HOME ?= $(ANDROID_HOME)/avd
SDKMANAGER        = $(ANDROID_HOME)/cmdline-tools/latest/bin/sdkmanager
AVDMANAGER        = $(ANDROID_HOME)/cmdline-tools/latest/bin/avdmanager
ADB               = $(ANDROID_HOME)/platform-tools/adb
EMULATOR          = $(ANDROID_HOME)/emulator/emulator
ANDROID_IMAGE    := system-images;android-35;google_apis;x86_64
AVD_NAME         := music-room
CMDLINE_TOOLS_URL := https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip

.DEFAULT_GOAL := help

.PHONY: help env up down restart logs ps db dev build test clean fclean re psql tools \
        android android-sdk emulator check-env check-android

help: ## Show this help
	@grep -hE '^[a-zA-Z_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

env: ## Create .env from .env.example with generated secrets
	@if [ -f .env ]; then echo ".env already exists, nothing to do."; else \
		cp .env.example .env && \
		sed -i "s|^JWT_SECRET=.*|JWT_SECRET=$$(openssl rand -hex 32)|" .env && \
		sed -i "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$$(openssl rand -hex 16)|" .env && \
		echo ".env created (JWT_SECRET and POSTGRES_PASSWORD generated)."; fi

check-env:
	@[ -f .env ] || { echo "No .env file: run 'make env' first."; exit 1; }

up: check-env ## Start PostgreSQL + server in Docker
	$(COMPOSE) up -d --build --wait
	@echo "API     : http://localhost:$${SERVER_PORT:-8080}/health"
	@echo "Swagger : http://localhost:$${SERVER_PORT:-8080}/swagger"

down: ## Stop containers
	$(COMPOSE) --profile tools down

restart: down up ## Restart containers

logs: ## Follow server logs
	$(COMPOSE) logs -f server

ps: ## List containers
	$(COMPOSE) ps

db: check-env ## Start PostgreSQL only
	$(COMPOSE) up -d --wait postgres

dev: db ## Start PostgreSQL in Docker and run the server locally with Gradle
	$(GRADLE) :server:run

tools: check-env ## Start pgAdmin (http://localhost:5050)
	$(COMPOSE) --profile tools up -d pgadmin

psql: ## Open a psql shell on the database
	$(COMPOSE) exec postgres psql -U $(POSTGRES_USER) -d $(POSTGRES_DB)

build: ## Compile all modules
	$(GRADLE) build -x test

test: ## Run all tests (Docker required for Testcontainers)
	$(GRADLE) test

android-sdk: ## Install the Android SDK + an emulator (in ANDROID_HOME, no root needed)
	@if [ -x "$(SDKMANAGER)" ]; then echo "cmdline-tools already installed in $(ANDROID_HOME)"; else \
		echo "Downloading cmdline-tools into $(ANDROID_HOME)..." && \
		mkdir -p "$(ANDROID_HOME)/cmdline-tools" && tmp=$$(mktemp -d) && \
		curl -fsSL -o $$tmp/clt.zip $(CMDLINE_TOOLS_URL) && unzip -q $$tmp/clt.zip -d $$tmp && \
		mv $$tmp/cmdline-tools "$(ANDROID_HOME)/cmdline-tools/latest" && rm -rf $$tmp; fi
	@yes | "$(SDKMANAGER)" --licenses > /dev/null
	"$(SDKMANAGER)" "platforms;android-35" "build-tools;35.0.0" "platform-tools" "emulator" "$(ANDROID_IMAGE)"
	@[ -f local.properties ] || echo "sdk.dir=$(ANDROID_HOME)" > local.properties
	@mkdir -p "$(ANDROID_AVD_HOME)"
	@[ -d "$(ANDROID_AVD_HOME)/$(AVD_NAME).avd" ] || \
		echo no | "$(AVDMANAGER)" create avd -n $(AVD_NAME) -k "$(ANDROID_IMAGE)" -d pixel_7
	@echo "SDK ready. Run 'make emulator' then 'make android'."

emulator: ## Start the Android emulator and wait until it has booted
	@[ -x "$(EMULATOR)" ] || { echo "Emulator not found: run 'make android-sdk' first."; exit 1; }
	@nohup "$(EMULATOR)" -avd $(AVD_NAME) -no-snapshot-save > "$(ANDROID_HOME)/emulator.log" 2>&1 &
	@echo "Starting emulator (logs: $(ANDROID_HOME)/emulator.log)..."
	@"$(ADB)" wait-for-device
	@until [ "$$("$(ADB)" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
	@echo "Emulator ready."

check-android:
	@[ -f local.properties ] || [ -d "$(ANDROID_HOME)/platforms" ] || { \
		echo "No Android SDK found. Run 'make android-sdk' (or open the project in Android Studio)."; exit 1; }
	@"$(ADB)" devices 2>/dev/null | grep -qw device$$ || { \
		echo "No device connected. Run 'make emulator' or plug in a phone with USB debugging enabled."; exit 1; }

android: check-android ## Install and launch the app on the connected emulator/device
	$(GRADLE) :composeApp:installDebug
	"$(ADB)" shell am start -n com.musicroom.app/.MainActivity

clean: ## Remove Gradle build outputs
	$(GRADLE) clean

fclean: clean ## Also remove containers, volumes (database data) and images
	$(COMPOSE) --profile tools down -v --rmi local

re: fclean up ## Start over from scratch
