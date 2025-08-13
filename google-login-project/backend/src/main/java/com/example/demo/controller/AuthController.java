@RestController
@RequestMapping("/api/auth/google")
@RequiredArgsConstructor
public class AuthController {
    
    // ... GoogleAuthService, UserServiceなどをDI ...

    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(@RequestBody CallbackRequestDto callbackRequest, HttpServletRequest request) {
        try {
            // セッションからstate, nonce, code_verifierを取得
            HttpSession session = request.getSession(false);
            String savedState = (String) session.getAttribute("state");
            String savedNonce = (String) session.getAttribute("nonce");
            String codeVerifier = (String) session.getAttribute("code_verifier");

            // stateを検証してCSRFを防ぐ [cite: 185]
            if (savedState == null || !savedState.equals(callbackRequest.getState())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid state");
            }

            // トークンを交換
            GoogleTokenResponse tokenResponse = googleAuthService.exchangeCodeForTokens(callbackRequest.getCode(), codeVerifier);
            
            // IDトークンを検証
            GoogleIdToken.Payload payload = googleAuthService.verifyIdToken(tokenResponse.getIdToken(), savedNonce);

            // ユーザーを処理
            User user = userService.processGoogleUser(payload, ...);
            
            // アプリケーション独自のセッションJWTを生成
            String jwt = jwtService.generateToken(user);
            
            return ResponseEntity.ok(Map.of("sessionToken", jwt));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Authentication failed.");
        }
    }
}