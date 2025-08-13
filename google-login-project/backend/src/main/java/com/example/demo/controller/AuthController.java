package com.example.demo.controller;

import com.example.demo.dto.CallbackRequestDto;
import com.example.demo.entity.User;
import com.example.demo.service.GoogleAuthService;
import com.example.demo.service.JwtService;
import com.example.demo.service.UserService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
/**
 * Google OAuth2認証のコールバック処理を担当するコントローラー。
 * <p>
 * Google認証後のコールバックを受け取り、トークン交換・IDトークン検証・ユーザー登録・JWT発行までを行う。
 * </p>
 */
public class AuthController {
    
    private final GoogleAuthService googleAuthService;
    private final UserService userService;
    private final JwtService jwtService;

    /**
     * Google OAuth2認証のコールバックを受けて、
     * トークン交換・IDトークン検証・ユーザー登録・JWT発行までを行うエンドポイント。
     * <p>
     * セッションからstate/nonce/code_verifierを取得し、CSRF・リプレイ攻撃対策も行う。
     * </p>
     * @param callbackRequest Google認証から返却された情報（code, state等）
     * @param request HTTPリクエスト（セッション取得用）
     * @return JWT（sessionToken）を含むレスポンス or エラー
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestBody CallbackRequestDto callbackRequest, HttpServletRequest request) {
        try {
            // セッションからstate, nonce, code_verifierを取得
            HttpSession session = request.getSession(false);
            String savedState = (String) session.getAttribute("state");
            String savedNonce = (String) session.getAttribute("nonce");
            String codeVerifier = (String) session.getAttribute("code_verifier");

            // stateを検証してCSRFを防ぐ
            if (savedState == null || !savedState.equals(callbackRequest.getState())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid state");
            }

            // トークンを交換
            GoogleTokenResponse tokenResponse = googleAuthService.exchangeCodeForTokens(callbackRequest.getCode(), codeVerifier);
            
            // IDトークンを検証
            GoogleIdToken.Payload payload = googleAuthService.verifyIdToken(tokenResponse.getIdToken(), savedNonce);

            // ユーザーを処理
            User user = userService.processGoogleUser(payload);
            
            // アプリケーション独自のセッションJWTを生成
            String jwt = jwtService.generateToken(user);
            
            return ResponseEntity.ok(Collections.singletonMap("sessionToken", jwt));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication failed.");
        }
    }
}