package com.example.demo.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleAuthServiceTest {

    private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        googleAuthService = new GoogleAuthService();
        ReflectionTestUtils.setField(googleAuthService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(googleAuthService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(googleAuthService, "redirectUri", "http://localhost:3000/callback");
    }

    @Test
    void testExchangeCodeForTokensWithNullCode() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            googleAuthService.exchangeCodeForTokens(null, "code-verifier");
        });
    }

    @Test
    void testExchangeCodeForTokensWithEmptyCode() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            googleAuthService.exchangeCodeForTokens("", "code-verifier");
        });
    }

    @Test
    void testExchangeCodeForTokensWithNullCodeVerifier() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            googleAuthService.exchangeCodeForTokens("auth-code", null);
        });
    }

    @Test
    void testVerifyIdTokenWithNullToken() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            googleAuthService.verifyIdToken(null, "expected-nonce");
        });
    }

    @Test
    void testVerifyIdTokenWithEmptyToken() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            googleAuthService.verifyIdToken("", "expected-nonce");
        });
    }

    @Test
    void testVerifyIdTokenWithNullNonce() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            googleAuthService.verifyIdToken("id-token", null);
        });
    }

    // 実際のHTTPリクエストのテストは統合テストで行う
    // ここでは入力検証のみをテストする
    @Test
    void testInputValidation() {
        // Given - valid inputs
        String validCode = "valid-auth-code";
        String validCodeVerifier = "valid-code-verifier";
        String validNonce = "valid-nonce";

        // When & Then - should not throw exceptions for valid inputs for parameter validation
        assertDoesNotThrow(() -> {
            // These will fail due to network calls, but input validation should pass
            try {
                googleAuthService.exchangeCodeForTokens(validCode, validCodeVerifier);
            } catch (IOException e) {
                // Expected - network call will fail in test environment
            }
        });

        // Test ID token validation separately without actual token parsing
        // Since we can't create a valid JWT token easily in tests, we only test parameter validation
        assertThrows(IllegalArgumentException.class, () -> {
            try {
                googleAuthService.verifyIdToken("", validNonce);
            } catch (GeneralSecurityException | IOException e) {
                // This should not happen for empty string validation
                fail("Should have thrown IllegalArgumentException for empty token");
            }
        });
    }
}
