# OAuthWithJavaSample

## 概要

このプロジェクトは、OAuth 2.0/OpenID Connectを使用したGoogleログイン機能を実装するサンプルアプリケーションです。**BFF（Backend for Frontend）パターン**を採用することで、セキュリティを強化したモダンなWebアプリケーションアーキテクチャを実現しています。

### 主な特徴

- **セキュアな認証フロー**: OAuth 2.0の認可コードフロー + PKCE（Proof Key for Code Exchange）を実装
- **BFFパターン**: フロントエンドとバックエンドの間にプロキシ層を配置し、トークンがブラウザに直接露出することを防止
- **HttpOnlyクッキー**: セッション管理にHttpOnlyクッキーを使用してXSS攻撃を防止
- **包括的なセキュリティ対策**: CSRF攻撃、リプレイ攻撃、その他の一般的な脅威に対する対策を実装

## アーキテクチャ

```
[ユーザー] ←→ [フロントエンド(Next.js)] ←→ [BFF(Next.js API)] ←→ [バックエンド(Spring Boot)] ←→ [Google OAuth]
                    ↓
                [HttpOnlyクッキー]
```

### 技術スタック

#### フロントエンド
- **Next.js 15.4.6** - React フレームワーク
- **React 19.1.0** - UIライブラリ
- **TypeScript** - 型安全性
- **Axios** - HTTP クライアント

#### バックエンド
- **Spring Boot 2.7.15** - Javaフレームワーク
- **Spring Security** - セキュリティフレームワーク
- **Spring Data JPA** - データアクセス層
- **Google API Client** - Google OAuth連携
- **JJWT** - JWT トークン管理
- **H2 Database** - 開発・テスト用データベース

## プロジェクト構造

```
OAuthWithJavaSample/
├── docs/                          # ドキュメント
│   ├── 00_OAuth-OIDC技術詳細レポート.md
│   └── 01_google-oauth2-guide.md
├── google-login-project/
│   ├── backend/                   # Spring Boot アプリケーション
│   │   ├── src/main/java/com/example/demo/
│   │   │   ├── controller/        # REST API コントローラー
│   │   │   ├── service/           # ビジネスロジック
│   │   │   ├── entity/            # JPA エンティティ
│   │   │   ├── repository/        # データアクセス層
│   │   │   └── dto/               # データ転送オブジェクト
│   │   └── src/test/              # テストコード
│   └── frontend/                  # Next.js アプリケーション
│       ├── src/pages/            # ページコンポーネント
│       ├── src/pages/api/        # BFF API ルート
│       └── src/styles/           # スタイル
└── README.md
```

## セットアップ手順

### 前提条件

- Java 8以上
- Node.js 18以上
- Google Cloud Platform アカウント

### 1. Google Cloud Platform の設定

1. **GCPプロジェクトの作成**
   - [Google Cloud Console](https://console.cloud.google.com/) にアクセス
   - 新しいプロジェクトを作成

2. **OAuth 2.0 認証情報の設定**
   ```
   認証情報 > 認証情報を作成 > OAuth クライアント ID
   ```
   - アプリケーションの種類: ウェブアプリケーション
   - 承認済みのリダイレクト URI: `http://localhost:3000/api/auth/callback`

3. **OAuth同意画面の設定**
   - 必要なスコープ: `openid`, `email`, `profile`
   - テストユーザーの追加（開発段階）

### 2. バックエンドの設定

1. **設定ファイルの更新**
   ```bash
   cd google-login-project/backend/src/main/resources
   ```
   
   `application.properties` を更新:
   ```properties
   # Google OAuth2 設定
   google.auth.client-id=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
   google.auth.client-secret=YOUR_GOOGLE_CLIENT_SECRET
   google.auth.redirect-uri=http://localhost:3000/api/auth/callback
   
   # JWT署名用シークレット
   app.jwt.secret=YOUR_VERY_STRONG_SECRET_KEY_FOR_JWT_SIGNING
   
   # データベース設定（開発用：H2）
   spring.datasource.url=jdbc:h2:mem:testdb
   spring.datasource.driver-class-name=org.h2database.Driver
   spring.jpa.hibernate.ddl-auto=create-drop
   ```

2. **アプリケーションの起動**
   ```bash
   cd google-login-project/backend
   ./gradlew bootRun
   ```
   
   バックエンドは `http://localhost:8080` で起動します。

### 3. フロントエンドの設定

1. **環境変数の設定**
   ```bash
   cd google-login-project/frontend
   ```
   
   `.env.local` ファイルを作成:
   ```
   NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
   ```

2. **依存関係のインストール**
   ```bash
   npm install
   ```

3. **ログインページの設定更新**
   `src/pages/login.tsx` のクライアントIDを実際の値に更新:
   ```typescript
   const client_id = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com";
   ```

4. **アプリケーションの起動**
   ```bash
   npm run dev
   ```
   
   フロントエンドは `http://localhost:3000` で起動します。

## 使用方法

1. **アプリケーションへのアクセス**
   - ブラウザで `http://localhost:3000` にアクセス
   - 自動的にログインページにリダイレクト

2. **Googleでログイン**
   - 「Sign in with Google」ボタンをクリック
   - Googleの認証ページで認証を完了
   - 成功時、ダッシュボードページにリダイレクト

3. **認証後の機能**
   - ダッシュボードでユーザー情報を確認
   - セッションはHttpOnlyクッキーで管理

## セキュリティ機能

### 実装済みのセキュリティ対策

- **PKCE（Proof Key for Code Exchange）**: 認可コード横取り攻撃を防止
- **State パラメータ**: CSRF攻撃を防止
- **Nonce パラメータ**: リプレイ攻撃を防止
- **HttpOnlyクッキー**: XSS攻撃によるトークン盗取を防止
- **IDトークン検証**: トークンの署名、発行者、有効期限を検証
- **セッション管理**: サーバーサイドでのセキュアなセッション管理

### セキュリティの詳細

詳細なセキュリティ実装については、以下のドキュメントを参照してください：
- [OAuth/OIDC技術詳細レポート](./docs/00_OAuth-OIDC技術詳細レポート.md)
- [Google OAuth2 実装ガイド](./docs/01_google-oauth2-guide.md)

## テスト

### バックエンドのテスト実行

```bash
cd google-login-project/backend
./gradlew test
```

### テストカバレッジの確認

```bash
./gradlew jacocoTestReport
```

テストカバレッジレポートは `build/jacocoHtml/index.html` で確認できます。

## API エンドポイント

### バックエンド API

- `POST /api/auth/google/callback` - Googleログインのコールバック処理
- `GET /api/user/me` - 認証済みユーザー情報の取得（要実装）

### フロントエンド BFF

- `GET /api/auth/callback` - Googleからのリダイレクト処理

## 開発時の注意点

### 本番環境への移行時

1. **セキュリティ設定の強化**
   - JWTシークレットキーの強化
   - HTTPSの必須化
   - セキュアクッキーの有効化

2. **データベースの変更**
   - H2からOracleまたは他の本番用DBへの移行
   - 接続情報の環境変数化

3. **PKCE実装の完全化**
   - サンプルでは簡略化されているcode_verifierの動的生成を実装

### よくある問題と解決方法

- **CORS エラー**: バックエンドでCORS設定を確認
- **認証失敗**: GCPの設定とリダイレクトURIの一致を確認
- **セッション不正**: クッキーの設定とドメインを確認

## ライセンス

このプロジェクトはサンプル実装であり、学習・研究目的での使用を想定しています。

## 参考資料

- [OAuth 2.0 RFC 6749](https://tools.ietf.org/html/rfc6749)
- [OpenID Connect Core 1.0](https://openid.net/specs/openid-connect-core-1_0.html)
- [Google Identity Platform Documentation](https://developers.google.com/identity)
- [PKCE RFC 7636](https://tools.ietf.org/html/rfc7636)