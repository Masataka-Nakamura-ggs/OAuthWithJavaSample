package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.Instant;

/**
 * 外部IDプロバイダー（Google等）とアプリユーザーの紐付け情報エンティティ。
 * <p>
 * 各種トークンや有効期限、プロバイダー情報を保持します。
 * </p>
 */
@Entity
@Table(name = "USER_IDENTITIES")
@Data
@NoArgsConstructor
public class UserIdentity {
    /**
     * ユーザーID連携の一意ID
     */
    @Id
    private String id;

    /**
     * 紐付くアプリユーザー
     */
    @ManyToOne
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    /**
     * IDプロバイダー名（例: Google）
     */
    private String providerName;

    /**
     * プロバイダー側のユーザーID（Googleのsubクレーム等）
     */
    private String providerUserId; // Google 'sub' claim

    /**
     * 暗号化済みリフレッシュトークン
     */
    private String encryptedRefreshToken;

    /**
     * 暗号化済みアクセストークン
     */
    private String encryptedAccessToken;

    /**
     * アクセストークンの有効期限
     */
    private Instant accessTokenExpiresAt;
}