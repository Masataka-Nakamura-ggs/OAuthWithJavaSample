package com.example.demo.service;

import com.example.demo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "myVerySecretKeyForTestingPurposes123456");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);
    }

    @Test
    void testGenerateToken() {
        // Given
        User user = new User();
        user.setId("test-user-id");
        user.setDisplayName("Test User");
        user.setProfileImageUrl("https://example.com/image.jpg");

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        // JWTトークンは3つの部分（header.payload.signature）に分かれている
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length);
    }

    @Test
    void testGenerateTokenWithNullUser() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            jwtService.generateToken(null);
        });
    }

    @Test
    void testGenerateTokenWithEmptyUserId() {
        // Given
        User user = new User();
        user.setId("");
        user.setDisplayName("Test User");

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testGenerateTokenConsistency() {
        // Given
        User user1 = new User();
        user1.setId("test-user-id-1");
        user1.setDisplayName("Test User 1");
        user1.setProfileImageUrl("https://example.com/image1.jpg");

        User user2 = new User();
        user2.setId("test-user-id-2");
        user2.setDisplayName("Test User 2");
        user2.setProfileImageUrl("https://example.com/image2.jpg");

        // When
        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);

        // Then
        assertNotNull(token1);
        assertNotNull(token2);
        // 異なるユーザーのため、トークンは異なる
        assertNotEquals(token1, token2);
        
        // 両方とも有効なJWTトークンの形式であることを確認
        String[] parts1 = token1.split("\\.");
        String[] parts2 = token2.split("\\.");
        assertEquals(3, parts1.length);
        assertEquals(3, parts2.length);
    }
}
