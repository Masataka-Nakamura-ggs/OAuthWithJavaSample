package com.example.demo.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.Instant;

/**
 * アプリケーションユーザー情報のエンティティ。
 * <p>
 * Google認証ユーザーのID・表示名・プロフィール画像URL・作成日時を保持します。
 * </p>
 */
@Entity
@Table(name = "USERS")
@Data
@NoArgsConstructor
public class User {
    /**
     * ユーザーID（Googleのsub値など）
     */
    @Id
    private String id;

    /**
     * ユーザーの表示名
     */
    private String displayName;

    /**
     * プロフィール画像URL
     */
    private String profileImageUrl;

    /**
     * レコード作成日時
     */
    @CreationTimestamp
    private Instant createdAt;
}