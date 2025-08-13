このサンプルは、レポートで強く推奨されている以下の現代的なセキュリティベストプラクティスを反映した構成になっています。

  * [cite\_start]**プロトコル:** OpenID Connect (OIDC) を利用した認証 [cite: 20, 21]。
  * [cite\_start]**認可フロー:** すべてのクライアントで推奨される「PKCE (Proof Key for Code Exchange) を伴う認可コードグラントフロー」 [cite: 107, 113]。
  * [cite\_start]**アーキテクチャ:** フロントエンド(Next.js)とバックエンド(Spring Boot)の間にBFF (Backend for Frontend) 層を設け、トークンがブラウザに漏洩することを防ぐ「BFFパターン」 [cite: 118, 122]。
  * [cite\_start]**トークン管理:** トークンはすべてサーバーサイドで管理し、フロントエンドとはHttpOnly属性のセキュアなセッションCookieで通信 [cite: 121]。
  * [cite\_start]**セキュリティ対策:** CSRF対策のための`state`パラメータ [cite: 99] [cite\_start]と、リプレイ攻撃対策のための`nonce`パラメータ [cite: 31] の必須利用。
  * [cite\_start]**データベース設計:** メールアドレスの変更に対応できるよう、不変である`sub`クレームをユーザーの主キーとして扱う [cite: 141, 142]。

-----

### 概要アーキテクチャ

1.  **フロントエンド (React/Next.js):** ユーザーインターフェースを提供。「Googleでログイン」ボタンを表示します。
2.  **BFF (Next.js API Routes):** フロントエンドとバックエンドの間に位置するプロキシ層。OAuthのコールバックを処理し、トークンを直接ブラウザに晒すことなく、バックエンドとの通信を仲介します。フロントエンドとはセキュアなセッションCookieを介して通信します。
3.  **バックエンド (Java/Spring Boot):** ビジネスロジックとデータベースとのやり取りを担当。GoogleとのOAuth/OIDCフロー（トークン交換、IDトークン検証など）を実際に実行し、ユーザー情報を管理します。
4.  **データベース (Oracle):** ユーザー情報を永続化します。

### ステップ1: データベース (Oracle) の準備

[cite\_start]レポートで推奨されている、プロバイダー情報とユーザー情報を分離するスキーマを設計します [cite: 144]。

```sql
-- ユーザーの基本情報を格納するテーブル
CREATE TABLE USERS (
    ID VARCHAR2(36) PRIMARY KEY, -- UUIDなどを想定
    DISPLAY_NAME VARCHAR2(255),
    PROFILE_IMAGE_URL VARCHAR2(2048),
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 外部認証プロバイダーとの紐付けを管理するテーブル
CREATE TABLE USER_IDENTITIES (
    ID VARCHAR2(36) PRIMARY KEY,
    USER_ID VARCHAR2(36) NOT NULL,
    PROVIDER_NAME VARCHAR2(50) NOT NULL, -- 'google' など
    PROVIDER_USER_ID VARCHAR2(255) NOT NULL, -- Googleの'sub'クレーム
    ENCRYPTED_REFRESH_TOKEN VARCHAR2(2048), -- 暗号化して保存
    -- アクセストークンは有効期間が短いため、必要に応じてキャッシュなどに保存する方が望ましい場合もある
    -- ここでは例としてDBに含める
    ENCRYPTED_ACCESS_TOKEN VARCHAR2(2048),
    ACCESS_TOKEN_EXPIRES_AT TIMESTAMP,
    CONSTRAINT FK_USER_IDENTITIES_USER_ID FOREIGN KEY (USER_ID) REFERENCES USERS(ID),
    CONSTRAINT UK_USER_IDENTITIES_PROVIDER UNIQUE (PROVIDER_NAME, PROVIDER_USER_ID)
);
```

### ステップ2: バックエンド (Java + Spring Boot) の実装

#### 1\. 依存関係の追加 (`pom.xml`)

Web、Security、JPA、Oracleドライバ、JWTライブラリなどを追加します。

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.oracle.database.jdbc</groupId>
        <artifactId>ojdbc8</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>com.google.api-client</groupId>
        <artifactId>google-api-client</artifactId>
        <version>2.2.0</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### 2\. 設定ファイル (`application.properties`)

[cite\_start]GCPで取得した情報とDB接続情報を設定します [cite: 72]。

```properties
# Google OAuth2/OIDC Settings
google.auth.client-id=YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com
google.auth.client-secret=YOUR_GOOGLE_CLIENT_SECRET
google.auth.redirect-uri=http://localhost:3000/api/auth/callback # Next.js BFFのコールバックURL

# Oracle DB Settings
spring.datasource.url=jdbc:oracle:thin:@//localhost:1521/ORCL
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.datasource.driver-class-name=oracle.jdbc.OracleDriver
spring.jpa.database-platform=org.hibernate.dialect.Oracle12cDialect

# JWT Secret for Session
app.jwt.secret=YOUR_VERY_STRONG_SECRET_KEY_FOR_JWT_SIGNING
```

#### 3\. JPAエンティティとリポジトリ

```java
// User.java
@Entity @Table(name = "USERS")
@Data @NoArgsConstructor
public class User {
    @Id private String id;
    private String displayName;
    private String profileImageUrl;
    @CreationTimestamp private Instant createdAt;
}

// UserIdentity.java
@Entity @Table(name = "USER_IDENTITIES")
@Data @NoArgsConstructor
public class UserIdentity {
    @Id private String id;
    @ManyToOne @JoinColumn(name = "USER_ID", nullable = false) private User user;
    private String providerName;
    private String providerUserId; // Google 'sub' claim
    private String encryptedRefreshToken;
    private String encryptedAccessToken;
    private Instant accessTokenExpiresAt;
}

// UserRepository.java
public interface UserRepository extends JpaRepository<User, String> {}

// UserIdentityRepository.java
public interface UserIdentityRepository extends JpaRepository<UserIdentity, String> {
    Optional<UserIdentity> findByProviderNameAndProviderUserId(String providerName, String providerUserId);
}
```

#### 4\. 認証ロジックのコア (`GoogleAuthService.java`)

Googleとの通信、トークン交換、IDトークン検証を担当します。

```java
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${google.auth.client-id}")
    private String clientId;
    @Value("${google.auth.client-secret}")
    private String clientSecret;
    @Value("${google.auth.redirect-uri}")
    private String redirectUri;

    private static final String GOOGLE_TOKEN_SERVER_URL = "https://oauth2.googleapis.com/token";
    private static final String ISSUER = "https://accounts.google.com";

    // トークン交換処理
    public GoogleTokenResponse exchangeCodeForTokens(String code, String codeVerifier) throws IOException {
        // ... GoogleのトークンエンドポイントへのPOSTリクエストを実装 ...
        // パラメータ: client_id, client_secret, code, redirect_uri, grant_type='authorization_code', code_verifier
        // レスポンスをパースして返す
    }

    // IDトークン検証
    public GoogleIdToken.Payload verifyIdToken(String idTokenString, String expectedNonce) throws GeneralSecurityException, IOException {
        [cite_start]// Googleのライブラリを使って署名、iss, aud, exp, nonceを検証 [cite: 191]
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList(clientId))
            .setIssuer(ISSUER)
            .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new GeneralSecurityException("ID Token is invalid.");
        }
        
        [cite_start]// nonceを検証してリプレイ攻撃を防ぐ [cite: 186]
        String nonce = (String) idToken.getPayload().get("nonce");
        if (!expectedNonce.equals(nonce)) {
            throw new GeneralSecurityException("Invalid nonce.");
        }

        return idToken.getPayload();
    }
}
```

#### 5\. ユーザー処理 (`UserService.java`)

[cite\_start]IDトークンの`sub`を元にユーザーを検索または新規作成（プロビジョニング）します [cite: 158, 160]。

```java
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    // TODO: トークンを暗号化/復号化するためのEncryptorをDIする

    public User processGoogleUser(GoogleIdToken.Payload payload, String accessToken, String refreshToken, Instant expiresAt) {
        String providerUserId = payload.getSubject(); [cite_start]// 'sub'クレームを永続的なキーとして使用 [cite: 189]

        Optional<UserIdentity> identityOpt = userIdentityRepository.findByProviderNameAndProviderUserId("google", providerUserId);

        if (identityOpt.isPresent()) {
            // 既存ユーザー
            UserIdentity identity = identityOpt.get();
            // TODO: トークン情報を更新
            return identity.getUser();
        } else {
            [cite_start]// 新規ユーザー [cite: 160]
            User newUser = new User();
            newUser.setId(UUID.randomUUID().toString());
            newUser.setDisplayName((String) payload.get("name"));
            newUser.setProfileImageUrl((String) payload.get("picture"));
            userRepository.save(newUser);

            UserIdentity newIdentity = new UserIdentity();
            newIdentity.setId(UUID.randomUUID().toString());
            newIdentity.setUser(newUser);
            newIdentity.setProviderName("google");
            newIdentity.setProviderUserId(providerUserId);
            // TODO: refreshTokenとaccessTokenを暗号化してセット
            newIdentity.setAccessTokenExpiresAt(expiresAt);
            userIdentityRepository.save(newIdentity);

            return newUser;
        }
    }
}
```

#### 6\. コントローラ (`AuthController.java`)

BFF (Next.js) からのAPIリクエストを処理するエンドポイントを提供します。

```java
@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class AuthController {
    
    // ... GoogleAuthService, UserServiceなどをDI ...

    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestBody CallbackRequestDto callbackRequest, HttpServletRequest request) {
        try {
            // セッションからstate, nonce, code_verifierを取得
            HttpSession session = request.getSession(false);
            String savedState = (String) session.getAttribute("state");
            String savedNonce = (String) session.getAttribute("nonce");
            String codeVerifier = (String) session.getAttribute("code_verifier");

            [cite_start]// stateを検証してCSRFを防ぐ [cite: 185]
            if (savedState == null || !savedState.equals(callbackRequest.getState())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid state");
            }

            // トークンを交換
            GoogleTokenResponse tokenResponse = googleAuthService.exchangeCodeForTokens(callbackRequest.getCode(), codeVerifier);
            
            // IDトークンを検証
            GoogleIdToken.Payload payload = googleAuthService.verifyIdToken(tokenResponse.getIdToken(), savedNonce);

            // ユーザーを処理
            User user = userService.processGoogleUser(payload, ...);
            
            // アプリケーション独自のセッションJWTを生成
            String jwt = jwtService.generateToken(user);
            
            return ResponseEntity.ok(Map.of("sessionToken", jwt));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication failed.");
        }
    }
}
```

-----

### ステップ3: フロントエンド (React + Next.js) の実装

#### 1\. 環境変数 (`.env.local`)

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

#### 2\. ログインページ (`pages/login.tsx`)

「Googleでログイン」ボタンを設置します。クリックすると、Googleの認可ページへリダイレクトします。

```tsx
import React from 'react';

const LoginPage = () => {
  const handleGoogleLogin = () => {
    [cite_start]// 認可コードフローを開始 [cite: 45, 46]
    // 本来はバックエンドにstate, nonce, code_challengeを生成させ、
    // それらを含む認可URLを取得するのがより安全。
    // ここでは簡略化のためにフロントでURLを構築。
    const client_id = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com";
    const redirect_uri = "http://localhost:3000/api/auth/callback"; // BFFのURL
    const scope = "openid email profile"; [cite_start]// OIDCのための必須スコープ [cite: 82]
    const response_type = "code";
    
    [cite_start]// CSRF対策 [cite: 99]
    const state = "aF0W8qV2z_SOME_RANDOM_STRING";
    [cite_start]// リプレイ攻撃対策 [cite: 31]
    const nonce = "n-0S6_WzA2Mj_SOME_RANDOM_STRING";

    [cite_start]// PKCE用パラメータ [cite: 110]
    // 本番実装では、code_verifierを生成し、ハッシュ化してcode_challengeを作成するライブラリを使用する
    const code_challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
    const code_challenge_method = "S256";

    // 本番では、code_verifierをHttpOnly Cookieなどに保存してBFFに渡す必要がある

    const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${client_id}&redirect_uri=${redirect_uri}&response_type=${response_type}&scope=${scope}&state=${state}&nonce=${nonce}&code_challenge=${code_challenge}&code_challenge_method=${code_challenge_method}`;
    
    window.location.href = authUrl;
  };

  return (
    <div>
      <h1>Login Page</h1>
      <button onClick={handleGoogleLogin}>Sign in with Google</button>
    </div>
  );
};

export default LoginPage;
```

#### 3\. BFFコールバック (`pages/api/auth/callback.ts`)

このAPIルートがBFFとして機能します。Googleからのリダイレクトを受け、トークンを直接扱わずバックエンドに中継します。

```ts
import type { NextApiRequest, NextApiResponse } from 'next';
import axios from 'axios';
import { serialize } from 'cookie';

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  const { code, state } = req.query;

  if (!code || !state) {
    return res.status(400).json({ error: 'Invalid request' });
  }

  try {
    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
    
    [cite_start]// BFFがバックエンドに認可コードを送信 [cite: 49]
    const response = await axios.post(`${apiBaseUrl}/api/auth/google/callback`, {
      code: code as string,
      state: state as string,
      // 本来はここでCookieからcode_verifierを読み取り、バックエンドに渡す
    });

    const { sessionToken } = response.data;

    [cite_start]// バックエンドから受け取ったセッショントークン(JWT)をHttpOnlyクッキーに設定 [cite: 121]
    [cite_start]// これにより、フロントエンドのJavaScriptからトークンにアクセスできなくなる(XSS対策) [cite: 117]
    const cookie = serialize('sessionToken', sessionToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV !== 'development',
      maxAge: 60 * 60 * 24 * 7, // 1 week
      sameSite: 'lax',
      path: '/',
    });
    res.setHeader('Set-Cookie', cookie);
    
    // ログイン後のページにリダイレクト
    res.redirect('/dashboard');

  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Authentication failed' });
  }
}
```

#### 4\. 保護されたページ (`pages/dashboard.tsx`)

サーバーサイドでCookieを検証し、ログイン済みユーザーのみがアクセスできるページの例です。

```tsx
import type { GetServerSideProps, NextPage } from 'next';

interface DashboardProps {
  user: {
    name: string;
    email: string;
  };
}

const Dashboard: NextPage<DashboardProps> = ({ user }) => {
  return (
    <div>
      <h1>Welcome, {user.name}</h1>
      <p>Your email is: {user.email}</p>
    </div>
  );
};

export const getServerSideProps: GetServerSideProps = async (context) => {
  const { req } = context;
  const token = req.cookies.sessionToken;

  if (!token) {
    return {
      redirect: {
        destination: '/login',
        permanent: false,
      },
    };
  }

  try {
    // クッキーのトークンを使い、バックエンドの保護されたAPIからユーザー情報を取得
    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
    const response = await fetch(`${apiBaseUrl}/api/user/me`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    if (!response.ok) throw new Error('Failed to fetch user');

    const user = await response.json();

    return {
      props: { user },
    };
  } catch (error) {
    return {
      redirect: {
        destination: '/login',
        permanent: false,
      },
    };
  }
};

export default Dashboard;
```

-----

### 結論と注意点

このサンプルコードは、提供されたレポート「GoogleログインOAuth認証詳細調査」で解説されている重要なセキュリティ概念を実装したものです。

  * [cite\_start]**BFFパターンの重要性:** このアーキテクチャの核心は、Next.jsのAPIルートをBFFとして利用し、OAuth/OIDCの複雑なフローと機密情報（トークン）をバックエンドにカプセル化している点です。これにより、フロントエンドはセキュアなセッションCookieのみを扱い、XSS攻撃によるトークン窃取のリスクを大幅に低減します [cite: 122]。
  * [cite\_start]**PKCEとStateの必須性:** 認可コード横取り攻撃 [cite: 106] [cite\_start]とCSRF攻撃 [cite: 98] を防ぐため、`code_challenge`と`state`の利用は現代のOAuth実装において必須です。
  * **本番実装に向けて:** このサンプルは概念を説明するためのものであり、本番環境で使用するには、以下のような追加の実装が必要です。
      * **トークンの暗号化:** DBに保存するリフレッシュトークンとアクセストークンは、必ず強力な暗号化を施してください。
      * **厳密なエラーハンドリング:** ネットワークエラー、APIエラー、トークン検証失敗など、あらゆるエラーケースを適切に処理する必要があります。
      * **PKCEの実装:** `code_verifier` を安全に生成・保存し、BFF経由でバックエンドに渡すロジックを完成させる必要があります。
      * **ロギングと監視:** 認証フローにおける重要なイベントをログに記録し、不正なアクティビティを監視する仕組みを導入してください。
      * [cite\_start]**リフレッシュトークンのローテーション:** レポートで言及されているリフレッシュトークンのローテーションを実装し、セキュリティをさらに強化することが推奨されます [cite: 127]。