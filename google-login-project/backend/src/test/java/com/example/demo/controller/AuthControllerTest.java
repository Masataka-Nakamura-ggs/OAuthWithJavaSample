package com.example.demo.controller;

import com.example.demo.dto.CallbackRequestDto;
import com.example.demo.entity.User;
import com.example.demo.service.GoogleAuthService;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GoogleAuthService googleAuthService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private CallbackRequestDto callbackRequest;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        callbackRequest = new CallbackRequestDto();
        callbackRequest.setCode("auth-code");
        callbackRequest.setState("test-state");

        session = new MockHttpSession();
        session.setAttribute("state", "test-state");
        session.setAttribute("nonce", "test-nonce");
        session.setAttribute("code_verifier", "test-code-verifier");
    }

    @Test
    @WithMockUser
    void testHandleCallbackSuccess() throws Exception {
        // Given
        GoogleTokenResponse tokenResponse = new GoogleTokenResponse();
        tokenResponse.setIdToken("mock-id-token");

        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        
        User user = new User();
        user.setId("user-id");
        user.setDisplayName("Test User");

        String jwt = "mock-jwt-token";

        when(googleAuthService.exchangeCodeForTokens(anyString(), anyString())).thenReturn(tokenResponse);
        when(googleAuthService.verifyIdToken(anyString(), anyString())).thenReturn(payload);
        when(userService.processGoogleUser(any(GoogleIdToken.Payload.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn(jwt);

        // When & Then
        mockMvc.perform(post("/api/auth/google/callback")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callbackRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionToken").value(jwt));

        verify(googleAuthService).exchangeCodeForTokens("auth-code", "test-code-verifier");
        verify(googleAuthService).verifyIdToken("mock-id-token", "test-nonce");
        verify(userService).processGoogleUser(payload);
        verify(jwtService).generateToken(user);
    }

    @Test
    @WithMockUser
    void testHandleCallbackInvalidState() throws Exception {
        // Given
        callbackRequest.setState("invalid-state");

        // When & Then
        mockMvc.perform(post("/api/auth/google/callback")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callbackRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid state"));

        verifyNoInteractions(googleAuthService, userService, jwtService);
    }

    @Test
    @WithMockUser
    void testHandleCallbackNoSession() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/google/callback")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callbackRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid state"));

        verifyNoInteractions(googleAuthService, userService, jwtService);
    }

    @Test
    @WithMockUser
    void testHandleCallbackNullState() throws Exception {
        // Given
        session.removeAttribute("state");

        // When & Then
        mockMvc.perform(post("/api/auth/google/callback")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callbackRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Invalid state"));

        verifyNoInteractions(googleAuthService, userService, jwtService);
    }

    @Test
    @WithMockUser
    void testHandleCallbackServiceException() throws Exception {
        // Given
        when(googleAuthService.exchangeCodeForTokens(anyString(), anyString()))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(post("/api/auth/google/callback")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callbackRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Authentication failed."));

        verify(googleAuthService).exchangeCodeForTokens("auth-code", "test-code-verifier");
        verifyNoInteractions(userService, jwtService);
    }
}
