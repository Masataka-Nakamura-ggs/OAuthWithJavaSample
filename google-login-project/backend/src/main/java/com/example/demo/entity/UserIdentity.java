package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;

// UserIdentity.java
@Entity @Table(name = "USER_IDENTITIES")
@Data @NoArgsConstructor
public class UserIdentity {
    @Id private String id;
    @ManyToOne @JoinColumn(name = "USER_ID", nullable = false) private User user;
    private String providerName;
    private String providerUserId; // Google 'sub' claim
    private String encryptedRefreshToken;
    private String encryptedAccessToken;
    private Instant accessTokenExpiresAt;
}