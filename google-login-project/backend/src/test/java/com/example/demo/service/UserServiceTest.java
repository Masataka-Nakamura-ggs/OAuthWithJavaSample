package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserIdentity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserIdentityRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @InjectMocks
    private UserService userService;

    private GoogleIdToken.Payload mockPayload;

    @BeforeEach
    void setUp() {
        // GoogleIdToken.Payloadのモックを明示的に作成
        mockPayload = mock(GoogleIdToken.Payload.class);
    }

    @Test
    void testProcessGoogleUserExistingUser() {
        // Given
        String providerUserId = "google-user-123";
        when(mockPayload.getSubject()).thenReturn(providerUserId);

        User existingUser = new User();
        existingUser.setId("existing-user-id");
        existingUser.setDisplayName("Existing User");

        UserIdentity existingIdentity = new UserIdentity();
        existingIdentity.setUser(existingUser);
        existingIdentity.setProviderUserId(providerUserId);

        when(userIdentityRepository.findByProviderNameAndProviderUserId("google", providerUserId))
                .thenReturn(Optional.of(existingIdentity));

        // When
        User result = userService.processGoogleUser(mockPayload);

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

        when(mockPayload.getSubject()).thenReturn(providerUserId);
        when(mockPayload.get("name")).thenReturn(userName);
        when(mockPayload.get("picture")).thenReturn(profileImageUrl);

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
        User result = userService.processGoogleUser(mockPayload);

        // Then
        assertNotNull(result);
        assertEquals(userName, result.getDisplayName());
        assertEquals(profileImageUrl, result.getProfileImageUrl());

        verify(userIdentityRepository).findByProviderNameAndProviderUserId("google", providerUserId);
        verify(userRepository).save(any(User.class));
        verify(userIdentityRepository).save(any(UserIdentity.class));
    }

    @Test
    void testProcessGoogleUserWithNullPayload() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            userService.processGoogleUser(null);
        });
    }

    @Test
    void testProcessGoogleUserWithNullSubject() {
        // Given
        when(mockPayload.getSubject()).thenReturn(null);

        // When & Then
        assertThrows(Exception.class, () -> {
            userService.processGoogleUser(mockPayload);
        });
    }
}
