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
WINDOWS_APP      := build/windows/MusicRoom
WINDOWS_JRE_URL  := https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse

IOS_PROJECT      := iosApp/iosApp.xcodeproj
IOS_SCHEME       := iosApp
IOS_BUNDLE_ID    := com.musicroom.app
IOS_APP          := MusicRoom.app
IOS_DERIVED_DATA := build/ios
# Name (or prefix) of the simulator to use, e.g. IOS_SIMULATOR="iPhone 18 Pro".
IOS_SIMULATOR    ?= iPhone

K8S_CLUSTER  := music-room
K8S_SERVICES := identity music library realtime
K8S_IMAGE    := ghcr.io/astarophty/music-room

.DEFAULT_GOAL := help

.PHONY: k8s-cluster k8s-images k8s-secrets k8s-deploy k8s-up k8s-status k8s-down

k8s-cluster: ## Create the local Kubernetes cluster (k3d)
	@k3d cluster list $(K8S_CLUSTER) > /dev/null 2>&1 || k3d cluster create $(K8S_CLUSTER) -p "8081:80@loadbalancer" --wait

k8s-images: ## Build the server image and load it into the local cluster
	docker build -f server/Dockerfile -t $(K8S_IMAGE)-identity:dev .
	@for s in $(filter-out identity,$(K8S_SERVICES)); do docker tag $(K8S_IMAGE)-identity:dev $(K8S_IMAGE)-$$s:dev; done
	k3d image import -c $(K8S_CLUSTER) $(foreach s,$(K8S_SERVICES),$(K8S_IMAGE)-$(s):dev)

k8s-secrets: check-env ## Create the Kubernetes secrets from .env
	./deploy/k8s/create-secrets.sh .env

k8s-deploy: ## Deploy every service to the local cluster and wait for them
	kubectl apply -k deploy/k8s/overlays/dev
	kubectl -n music-room rollout status statefulset/postgres --timeout=180s
	@for s in $(K8S_SERVICES); do kubectl -n music-room rollout status deployment/$$s --timeout=240s; done
	@echo "API: http://music-room.localhost:8081/health"

k8s-up: k8s-cluster k8s-images k8s-secrets k8s-deploy ## Create the cluster, build and deploy everything

k8s-status: ## Show pods, autoscalers and routes
	kubectl -n music-room get pods,svc,hpa,pdb,ingressroute

k8s-down: ## Delete the local cluster and its data
	k3d cluster delete $(K8S_CLUSTER)

.PHONY: help env up down restart logs ps db dev build test clean fclean re psql tools \
        android android-sdk emulator check-env check-sdk check-android check-ios \
        desktop linux windows web ios

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
	"$(SDKMANAGER)" "platforms;android-36" "build-tools;36.0.0" "platform-tools" "emulator" "$(ANDROID_IMAGE)"
	@[ -f local.properties ] || echo "sdk.dir=$(ANDROID_HOME)" > local.properties
	@mkdir -p "$(ANDROID_AVD_HOME)"
	@[ -d "$(ANDROID_AVD_HOME)/$(AVD_NAME).avd" ] || \
		echo no | "$(AVDMANAGER)" create avd -n $(AVD_NAME) -k "$(ANDROID_IMAGE)" -d pixel_7
	@sed -i 's/^hw.keyboard *=.*/hw.keyboard = yes/' "$(ANDROID_AVD_HOME)/$(AVD_NAME).avd/config.ini"
	@echo "SDK ready. Run 'make emulator' then 'make android'."

emulator: ## Start the Android emulator and wait until it has booted
	@[ -x "$(EMULATOR)" ] || { echo "Emulator not found: run 'make android-sdk' first."; exit 1; }
	@nohup "$(EMULATOR)" -avd $(AVD_NAME) -no-snapshot-save > "$(ANDROID_HOME)/emulator.log" 2>&1 &
	@echo "Starting emulator (logs: $(ANDROID_HOME)/emulator.log)..."
	@"$(ADB)" wait-for-device
	@until [ "$$("$(ADB)" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
	@echo "Emulator ready."

check-sdk:
	@[ -f local.properties ] || [ -d "$(ANDROID_HOME)/platforms" ] || { \
		echo "No Android SDK found. Run 'make android-sdk' (or open the project in Android Studio)."; exit 1; }

check-android: check-sdk
	@"$(ADB)" devices 2>/dev/null | grep -qw device$$ || { \
		echo "No device connected. Run 'make emulator' or plug in a phone with USB debugging enabled."; exit 1; }

android: check-android ## Install and launch the app on the connected emulator/device
	$(GRADLE) :androidApp:installDebug
	"$(ADB)" shell am start -n com.musicroom.app/.MainActivity

check-ios: check-sdk
	@[ "$$(uname)" = "Darwin" ] || { echo "The iOS app can only be built on macOS."; exit 1; }
	@xcodebuild -version > /dev/null 2>&1 || { \
		echo "Xcode not ready: install Xcode, then run 'sudo xcode-select -s /Applications/Xcode.app'."; exit 1; }

ios: check-ios ## Build and launch the iOS app on a simulator (macOS + Xcode, Apple Silicon only)
	@set -e; \
	udid=$$(xcrun simctl list devices booted | grep -oE '[0-9A-F-]{36}' | head -1); \
	if [ -z "$$udid" ]; then \
		udid=$$(xcrun simctl list devices available | grep -E '^[[:space:]]+$(IOS_SIMULATOR)' \
			| head -1 | grep -oE '[0-9A-F-]{36}' || true); fi; \
	[ -n "$$udid" ] || { \
		echo "No '$(IOS_SIMULATOR)' simulator found: install one from Xcode > Settings > Components,"; \
		echo "or pick another one with 'make ios IOS_SIMULATOR=\"iPhone 18 Pro\"'."; exit 1; }; \
	echo "Booting simulator $$udid..."; \
	xcrun simctl bootstatus "$$udid" -b; \
	open -a Simulator --args -CurrentDeviceUDID "$$udid" 2>/dev/null \
		|| open -a "$$(xcode-select -p)/../Applications/DeviceHub.app" 2>/dev/null \
		|| echo "No Simulator window opened; the app still runs on the booted device."; \
	xcodebuild -project $(IOS_PROJECT) -scheme $(IOS_SCHEME) -configuration Debug \
		-destination "id=$$udid" -derivedDataPath $(IOS_DERIVED_DATA) build; \
	xcrun simctl install "$$udid" "$(IOS_DERIVED_DATA)/Build/Products/Debug-iphonesimulator/$(IOS_APP)"; \
	xcrun simctl launch "$$udid" $(IOS_BUNDLE_ID)

desktop: ## Run the desktop app on this computer (Linux, macOS, or Windows with Gradle)
	$(GRADLE) :composeApp:run

linux: desktop ## Run the desktop app on Linux (on WSL it opens through WSLg)

windows: ## Build the Windows app (with its own Java, nothing to install) and start it from WSL
	@rm -f composeApp/build/compose/jars/*.jar
	$(GRADLE) :composeApp:packageUberJarForCurrentOS -PdesktopTarget=windows
	@mkdir -p "$(WINDOWS_APP)"
	@cp composeApp/build/compose/jars/*.jar "$(WINDOWS_APP)/music-room.jar"
	@[ -d "$(WINDOWS_APP)/jre" ] || { echo "Downloading a Java runtime for Windows..." && tmp=$$(mktemp -d) && \
		curl -fsSL -o $$tmp/jre.zip "$(WINDOWS_JRE_URL)" && unzip -q $$tmp/jre.zip -d $$tmp && \
		mv $$tmp/jdk-*-jre "$(WINDOWS_APP)/jre" && rm -rf $$tmp; }
	@printf '@echo off\r\nstart "" "%%~dp0jre\\bin\\javaw.exe" -jar "%%~dp0music-room.jar"\r\n' > "$(WINDOWS_APP)/MusicRoom.bat"
	@echo "Windows app ready: $(WINDOWS_APP)/MusicRoom.bat (copy the folder anywhere on Windows)."
	@if command -v cmd.exe > /dev/null; then cd "$(WINDOWS_APP)" && cmd.exe /c MusicRoom.bat; fi

web: ## Run the web app on http://localhost:3000 against the local server
	$(GRADLE) :composeApp:wasmJsBrowserDevelopmentRun

clean: ## Remove Gradle build outputs
	$(GRADLE) clean

fclean: clean ## Also remove containers, volumes (database data) and images
	$(COMPOSE) --profile tools down -v --rmi local

re: fclean up ## Start over from scratch
