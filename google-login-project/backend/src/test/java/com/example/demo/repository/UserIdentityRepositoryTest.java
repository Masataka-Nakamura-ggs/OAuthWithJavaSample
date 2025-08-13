package com.example.demo.repository;

import com.example.demo.entity.User;
import com.example.demo.entity.UserIdentity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserIdentityRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Test
    void testFindByProviderNameAndProviderUserId() {
        // Given
        User user = new User();
        user.setId("test-user-id");
        user.setDisplayName("Test User");
        entityManager.persist(user);

        UserIdentity identity = new UserIdentity();
        identity.setId("test-identity-id");
        identity.setUser(user);
        identity.setProviderName("google");
        identity.setProviderUserId("google-user-123");
        identity.setEncryptedAccessToken("encrypted-token");
        identity.setAccessTokenExpiresAt(Instant.now());
        entityManager.persist(identity);
        entityManager.flush();

        // When
        Optional<UserIdentity> foundIdentity = userIdentityRepository
                .findByProviderNameAndProviderUserId("google", "google-user-123");

        // Then
        assertTrue(foundIdentity.isPresent());
        assertEquals("test-identity-id", foundIdentity.get().getId());
        assertEquals("google", foundIdentity.get().getProviderName());
        assertEquals("google-user-123", foundIdentity.get().getProviderUserId());
        assertEquals(user, foundIdentity.get().getUser());
    }

    @Test
    void testFindByProviderNameAndProviderUserIdNotFound() {
        // When
        Optional<UserIdentity> foundIdentity = userIdentityRepository
                .findByProviderNameAndProviderUserId("google", "non-existent-user");

        // Then
        assertFalse(foundIdentity.isPresent());
    }

    @Test
    void testSaveUserIdentity() {
        // Given
        User user = new User();
        user.setId("test-user-id-2");
        user.setDisplayName("Test User 2");
        entityManager.persist(user);

        UserIdentity identity = new UserIdentity();
        identity.setId("test-identity-id-2");
        identity.setUser(user);
        identity.setProviderName("google");
        identity.setProviderUserId("google-user-456");
        identity.setEncryptedRefreshToken("encrypted-refresh");
        identity.setEncryptedAccessToken("encrypted-access");
        identity.setAccessTokenExpiresAt(Instant.now().plusSeconds(3600));

        // When
        UserIdentity savedIdentity = userIdentityRepository.save(identity);
        entityManager.flush();

        // Then
        assertNotNull(savedIdentity);
        assertEquals("test-identity-id-2", savedIdentity.getId());
        assertEquals("google-user-456", savedIdentity.getProviderUserId());
    }
}
