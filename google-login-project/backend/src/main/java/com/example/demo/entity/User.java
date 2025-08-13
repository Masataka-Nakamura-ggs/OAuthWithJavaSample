package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.Instant;

// User.java
@Entity @Table(name = "USERS")
@Data @NoArgsConstructor
public class User {
    @Id private String id;
    private String displayName;
    private String profileImageUrl;
    @CreationTimestamp private Instant createdAt;
}