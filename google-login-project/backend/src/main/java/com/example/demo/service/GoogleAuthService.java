package com.example.demo.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
/**
 * Google OAuth2認証に関する処理を提供するサービスクラス。
 * <p>
 * ・認可コードからアクセストークン/IDトークンへの交換
 * ・IDトークンの検証
 * などを行います。
 */
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    /**
     * Google APIのクライアントID
     */
    @Value("${google.auth.client-id}")
    private String clientId;

    /**
     * Google APIのクライアントシークレット
     */
    @Value("${google.auth.client-secret}")
    private String clientSecret;

    /**
     * OAuth2認証後のリダイレクトURI
     */
    @Value("${google.auth.redirect-uri}")
    private String redirectUri;

    /**
     * GoogleのトークンエンドポイントURL
     */
    private static final String GOOGLE_TOKEN_SERVER_URL = "https://oauth2.googleapis.com/token";

    /**
     * Google IDトークンのissuer値
     */
    private static final String ISSUER = "https://accounts.google.com";

    /**
     * 認可コードとcode_verifierからGoogleのトークンエンドポイントへリクエストし、
     * アクセストークン・IDトークン等を取得します。
     *
     * @param code 認可コード（Googleから返却されたもの）
     * @param codeVerifier PKCE用のcode_verifier
     * @return GoogleTokenResponse（アクセストークン・IDトークン等）
     * @throws IOException 通信失敗時
     */
    public GoogleTokenResponse exchangeCodeForTokens(String code, String codeVerifier) throws IOException {
        // 入力チェック
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty");
        }
        if (codeVerifier == null || codeVerifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Code verifier cannot be null or empty");
        }

        // HTTPリクエストの準備
        NetHttpTransport transport = new NetHttpTransport();
        HttpRequestFactory requestFactory = transport.createRequestFactory();

        // パラメータ設定
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("code", code);
        params.put("redirect_uri", redirectUri);
        params.put("grant_type", "authorization_code");
        params.put("code_verifier", codeVerifier);

        // POSTリクエスト送信
        UrlEncodedContent content = new UrlEncodedContent(params);
        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(GOOGLE_TOKEN_SERVER_URL), content);

        // レスポンス取得・パース
        HttpResponse response = request.execute();
        return response.parseAs(GoogleTokenResponse.class);
    }

    /**
     * GoogleのIDトークンを検証します。
     * <p>
     * 署名・issuer・audience・有効期限・nonceを検証し、
     * 問題なければペイロードを返却します。
     * </p>
     * @param idTokenString 検証対象のIDトークン文字列
     * @param expectedNonce 期待するnonce値（リプレイ攻撃防止）
     * @return GoogleIdToken.Payload（IDトークンのペイロード）
     * @throws GeneralSecurityException トークン不正時
     * @throws IOException 通信失敗時
     */
    public GoogleIdToken.Payload verifyIdToken(String idTokenString, String expectedNonce) throws GeneralSecurityException, IOException {
        // 入力チェック
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            throw new IllegalArgumentException("ID token cannot be null or empty");
        }
        if (expectedNonce == null || expectedNonce.trim().isEmpty()) {
            throw new IllegalArgumentException("Expected nonce cannot be null or empty");
        }

        // Googleのライブラリを使って署名、issuer、audience、exp、nonceを検証
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList(clientId))
            .setIssuer(ISSUER)
            .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new GeneralSecurityException("ID Token is invalid.");
        }

        // nonceを検証してリプレイ攻撃を防ぐ
        String nonce = (String) idToken.getPayload().get("nonce");
        if (!expectedNonce.equals(nonce)) {
            throw new GeneralSecurityException("Invalid nonce.");
        }

        return idToken.getPayload();
    }
}