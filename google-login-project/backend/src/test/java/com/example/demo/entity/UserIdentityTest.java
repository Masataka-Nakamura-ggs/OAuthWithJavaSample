package com.example.demo.entity;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class UserIdentityTest {

    @Test
    void testUserIdentityCreation() {
        // Given
        User user = new User();
        user.setId("user-id");
        
        UserIdentity identity = new UserIdentity();
        identity.setId("identity-id");
        identity.setUser(user);
        identity.setProviderName("google");
        identity.setProviderUserId("google-user-id");
        identity.setEncryptedRefreshToken("encrypted-refresh-token");
        identity.setEncryptedAccessToken("encrypted-access-token");
        identity.setAccessTokenExpiresAt(Instant.now());

        // Then
        assertEquals("identity-id", identity.getId());
        assertEquals(user, identity.getUser());
        assertEquals("google", identity.getProviderName());
        assertEquals("google-user-id", identity.getProviderUserId());
        assertEquals("encrypted-refresh-token", identity.getEncryptedRefreshToken());
        assertEquals("encrypted-access-token", identity.getEncryptedAccessToken());
        assertNotNull(identity.getAccessTokenExpiresAt());
    }

    @Test
    void testUserIdentityEqualsAndHashCode() {
        // Given
        User user = new User();
        user.setId("user-id");

        UserIdentity identity1 = new UserIdentity();
        identity1.setId("identity-id");
        identity1.setUser(user);
        identity1.setProviderName("google");
        
        UserIdentity identity2 = new UserIdentity();
        identity2.setId("identity-id");
        identity2.setUser(user);
        identity2.setProviderName("google");

        // Then
        assertEquals(identity1, identity2);
        assertEquals(identity1.hashCode(), identity2.hashCode());
    }
}
