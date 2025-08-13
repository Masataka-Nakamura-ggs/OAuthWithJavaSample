package com.example.demo.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserCreation() {
        // Given
        User user = new User();
        user.setId("test-id");
        user.setDisplayName("Test User");
        user.setProfileImageUrl("https://example.com/image.jpg");

        // Then
        assertEquals("test-id", user.getId());
        assertEquals("Test User", user.getDisplayName());
        assertEquals("https://example.com/image.jpg", user.getProfileImageUrl());
        // @CreationTimestampはJPA管理下でのみ動作するため、手動設定では動作しない
        // assertNotNull(user.getCreatedAt());
    }

    @Test
    void testUserEqualsAndHashCode() {
        // Given
        User user1 = new User();
        user1.setId("test-id");
        user1.setDisplayName("Test User");
        
        User user2 = new User();
        user2.setId("test-id");
        user2.setDisplayName("Test User");

        // Then
        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }
}
