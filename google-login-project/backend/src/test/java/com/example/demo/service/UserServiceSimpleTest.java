package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserIdentity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserIdentityRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceSimpleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userIdentityRepository);
    }

    @Test
    void testProcessGoogleUserExistingUser() {
        // Given
        String providerUserId = "google-user-123";
        
        // Create a real GoogleIdToken.Payload object
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(providerUserId);
        payload.put("name", "Existing User");
        payload.put("picture", "https://example.com/existing.jpg");

        User existingUser = new User();
        existingUser.setId("existing-user-id");
        existingUser.setDisplayName("Existing User");

        UserIdentity existingIdentity = new UserIdentity();
        existingIdentity.setUser(existingUser);
        existingIdentity.setProviderUserId(providerUserId);

        when(userIdentityRepository.findByProviderNameAndProviderUserId("google", providerUserId))
                .thenReturn(Optional.of(existingIdentity));

        // When
        User result = userService.processGoogleUser(payload);

        // Then
        assertEquals(existingUser, result);
        verify(userIdentityRepository).findByProviderNameAndProviderUserId("google", providerUserId);
        verify(userRepository, never()).save(any(User.class));
        verify(userIdentityRepository, never()).save(any(UserIdentity.class));
    }

    @Test
    void testProcessGoogleUserNewUser() {
        // Given
        String providerUserId = "google-user-456";
        String userName = "New User";
        String profileImageUrl = "https://example.com/new-image.jpg";

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject(providerUserId);
        payload.put("name", userName);
        payload.put("picture", profileImageUrl);

        when(userIdentityRepository.findByProviderNameAndProviderUserId("google", providerUserId))
                .thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setId("new-user-id");
        savedUser.setDisplayName(userName);
        savedUser.setProfileImageUrl(profileImageUrl);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserIdentity savedIdentity = new UserIdentity();
        when(userIdentityRepository.save(any(UserIdentity.class))).thenReturn(savedIdentity);

        // When
        User result = userService.processGoogleUser(payload);

        // Then
        assertNotNull(result);
        assertEquals(userName, result.getDisplayName());
        assertEquals(profileImageUrl, result.getProfileImageUrl());

        verify(userIdentityRepository).findByProviderNameAndProviderUserId("google", providerUserId);
        verify(userRepository).save(any(User.class));
        verify(userIdentityRepository).save(any(UserIdentity.class));
    }
}
