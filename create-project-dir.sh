#!/bin/bash

# --- メインプロジェクトディレクトリの作成 ---
echo "Creating main project directory: google-login-project"
mkdir -p google-login-project
cd google-login-project

# --- バックエンド (Spring Boot + Gradle) のディレクトリ構成を作成 ---
echo "Creating backend directory structure..."
mkdir -p backend/src/main/java/com/example/demo/{config,controller,dto,entity,repository,service}
mkdir -p backend/src/main/resources
mkdir -p backend/src/test/java/com/example/demo

# バックエンドの空ファイルを作成
echo "Creating backend placeholder files..."
touch backend/build.gradle
touch backend/settings.gradle
touch backend/src/main/java/com/example/demo/DemoApplication.java
touch backend/src/main/java/com/example/demo/config/SecurityConfig.java
touch backend/src/main/java/com/example/demo/controller/AuthController.java
touch backend/src/main/java/com/example/demo/dto/CallbackRequestDto.java
touch backend/src/main/java/com/example/demo/entity/User.java
touch backend/src/main/java/com/example/demo/entity/UserIdentity.java
touch backend/src/main/java/com/example/demo/repository/UserRepository.java
touch backend/src/main/java/com/example/demo/repository/UserIdentityRepository.java
touch backend/src/main/java/com/example/demo/service/GoogleAuthService.java
touch backend/src/main/java/com/example/demo/service/UserService.java
touch backend/src/main/java/com/example/demo/service/JwtService.java
touch backend/src/main/resources/application.properties

# --- フロントエンド (Next.js) のディレクトリ構成を作成 ---
echo "Creating frontend directory structure..."
mkdir -p frontend/pages/api/auth
mkdir -p frontend/public
mkdir -p frontend/styles

# フロントエンドの空ファイルを作成
echo "Creating frontend placeholder files..."
touch frontend/pages/api/auth/callback.ts
touch frontend/pages/_app.tsx
touch frontend/pages/dashboard.tsx
touch frontend/pages/login.tsx
touch frontend/public/.gitkeep # publicディレクトリが空でもGitで管理されるように
touch frontend/styles/globals.css
touch frontend/.env.local
touch frontend/next.config.js
touch frontend/package.json
touch frontend/tsconfig.json

echo "--------------------------------------------------"
echo "Project structure for 'google-login-project' created successfully."
echo "Next steps:"
echo "1. For the backend, you might want to run 'gradle init' or use Spring Initializr (https://start.spring.io/) to get a complete Gradle project with wrappers."
echo "2. For the frontend, navigate to 'frontend/' and run 'npm install' after filling in your package.json."
echo "--------------------------------------------------"

