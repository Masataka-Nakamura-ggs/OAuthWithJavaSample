package com.example.demo.service;

import com.example.demo.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    /**
     * JWTの署名に使用するシークレットキー。
     * application.properties等で設定可能。デフォルトは"mySecretKey"。
     */
    @Value("${jwt.secret:mySecretKey}")
    private String secretKey;

    /**
     * JWTの有効期限（ミリ秒）。デフォルトは1時間（3600000ms）。
     */
    @Value("${jwt.expiration:3600000}")
    private Long expiration;

    /**
     * 指定したユーザー情報からJWTトークンを生成します。
     * <p>
     * トークンにはユーザーID（subject）、表示名（displayName）、プロフィール画像URL（profileImageUrl）を含みます。
     * 有効期限は{@code expiration}で指定したミリ秒後です。
     * </p>
     *
     * @param user トークンに含めるユーザー情報
     * @return 生成されたJWTトークン（文字列）
     */
    public String generateToken(User user) {
        // シークレットキーからHMAC用のSecretKeyを生成
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes());

        // JWTトークンを構築
        return Jwts.builder()
                .setSubject(user.getId()) // ユーザーIDをsubjectに設定
                .claim("displayName", user.getDisplayName()) // 表示名をクレームに追加
                .claim("profileImageUrl", user.getProfileImageUrl()) // プロフィール画像URLをクレームに追加
                .setIssuedAt(new Date()) // 発行日時
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 有効期限
                .signWith(key, SignatureAlgorithm.HS256) // HMAC-SHA256で署名
                .compact(); // JWT文字列として返却
    }
}
