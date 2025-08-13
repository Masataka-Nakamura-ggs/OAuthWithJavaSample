package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.entity.UserIdentity;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserIdentityRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    // TODO: トークンを暗号化/復号化するためのEncryptorをDIする

    public User processGoogleUser(GoogleIdToken.Payload payload) {
        String providerUserId = payload.getSubject(); // 'sub'クレームを永続的なキーとして使用 [cite: 189]

        Optional<UserIdentity> identityOpt = userIdentityRepository.findByProviderNameAndProviderUserId("google", providerUserId);

        if (identityOpt.isPresent()) {
            // 既存ユーザー
            UserIdentity identity = identityOpt.get();
            // TODO: トークン情報を更新
            // identity.setEncryptedAccessToken(encryptedAccessToken);
            // identity.setEncryptedRefreshToken(encryptedRefreshToken);
            // identity.setAccessTokenExpiresAt(expiresAt);
            // userIdentityRepository.save(identity);
            return identity.getUser();
        } else {
            // 新規ユーザー [cite: 160]
            User newUser = new User();
            newUser.setId(UUID.randomUUID().toString());
            newUser.setDisplayName((String) payload.get("name"));
            newUser.setProfileImageUrl((String) payload.get("picture"));
            userRepository.save(newUser);

            UserIdentity newIdentity = new UserIdentity();
            newIdentity.setId(UUID.randomUUID().toString());
            newIdentity.setUser(newUser);
            newIdentity.setProviderName("google");
            newIdentity.setProviderUserId(providerUserId);
            // TODO: refreshTokenとaccessTokenを暗号化してセット
            // newIdentity.setEncryptedAccessToken(encryptedAccessToken);
            // newIdentity.setEncryptedRefreshToken(encryptedRefreshToken);
            // newIdentity.setAccessTokenExpiresAt(expiresAt);
            userIdentityRepository.save(newIdentity);

            return newUser;
        }
    }
}