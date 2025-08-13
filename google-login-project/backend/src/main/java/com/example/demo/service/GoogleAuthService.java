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
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Authorization code cannot be null or empty");
        }
        if (codeVerifier == null || codeVerifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Code verifier cannot be null or empty");
        }
        
        NetHttpTransport transport = new NetHttpTransport();
        HttpRequestFactory requestFactory = transport.createRequestFactory();
        
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        params.put("code", code);
        params.put("redirect_uri", redirectUri);
        params.put("grant_type", "authorization_code");
        params.put("code_verifier", codeVerifier);
        
        UrlEncodedContent content = new UrlEncodedContent(params);
        HttpRequest request = requestFactory.buildPostRequest(new GenericUrl(GOOGLE_TOKEN_SERVER_URL), content);
        
        HttpResponse response = request.execute();
        return response.parseAs(GoogleTokenResponse.class);
    }

    // IDトークン検証
    public GoogleIdToken.Payload verifyIdToken(String idTokenString, String expectedNonce) throws GeneralSecurityException, IOException {
        if (idTokenString == null || idTokenString.trim().isEmpty()) {
            throw new IllegalArgumentException("ID token cannot be null or empty");
        }
        if (expectedNonce == null || expectedNonce.trim().isEmpty()) {
            throw new IllegalArgumentException("Expected nonce cannot be null or empty");
        }
        
        // Googleのライブラリを使って署名、iss, aud, exp, nonceを検証 [cite: 191]
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList(clientId))
            .setIssuer(ISSUER)
            .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new GeneralSecurityException("ID Token is invalid.");
        }
        
        // nonceを検証してリプレイ攻撃を防ぐ [cite: 186]
        String nonce = (String) idToken.getPayload().get("nonce");
        if (!expectedNonce.equals(nonce)) {
            throw new GeneralSecurityException("Invalid nonce.");
        }

        return idToken.getPayload();
    }
}