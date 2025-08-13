package com.example.demo.repository;

import com.example.demo.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindUser() {
        // Given
        User user = new User();
        user.setId("test-user-id");
        user.setDisplayName("Test User");
        user.setProfileImageUrl("https://example.com/image.jpg");

        // When
        User savedUser = userRepository.save(user);
        entityManager.flush();
        Optional<User> foundUser = userRepository.findById("test-user-id");

        // Then
        assertNotNull(savedUser);
        assertTrue(foundUser.isPresent());
        assertEquals("test-user-id", foundUser.get().getId());
        assertEquals("Test User", foundUser.get().getDisplayName());
        assertEquals("https://example.com/image.jpg", foundUser.get().getProfileImageUrl());
    }

    @Test
    void testFindNonExistentUser() {
        // When
        Optional<User> foundUser = userRepository.findById("non-existent-id");

        // Then
        assertFalse(foundUser.isPresent());
    }

    @Test
    void testDeleteUser() {
        // Given
        User user = new User();
        user.setId("test-user-to-delete");
        user.setDisplayName("User to Delete");
        userRepository.save(user);
        entityManager.flush();

        // When
        userRepository.deleteById("test-user-to-delete");
        entityManager.flush();
        Optional<User> foundUser = userRepository.findById("test-user-to-delete");

        // Then
        assertFalse(foundUser.isPresent());
    }
}
