@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${google.auth.client-id}")
    private String clientId;
    @Value("${google.auth.client-secret}")
    private String clientSecret;
    @Value("${google.auth.redirect-uri}")
    private String redirectUri;

    private static final String GOOGLE_TOKEN_SERVER_URL = "https://oauth2.googleapis.com/token";
    private static final String ISSUER = "https://accounts.google.com";

    // トークン交換処理
    public GoogleTokenResponse exchangeCodeForTokens(String code, String codeVerifier) throws IOException {
        // ... GoogleのトークンエンドポイントへのPOSTリクエストを実装 ...
        // パラメータ: client_id, client_secret, code, redirect_uri, grant_type='authorization_code', code_verifier
        // レスポンスをパースして返す
    }

    // IDトークン検証
    public GoogleIdToken.Payload verifyIdToken(String idTokenString, String expectedNonce) throws GeneralSecurityException, IOException {
        // Googleのライブラリを使って署名、iss, aud, exp, nonceを検証 [cite: 191]
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
            .setAudience(Collections.singletonList(clientId))
            .setIssuer(ISSUER)
            .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new GeneralSecurityException("ID Token is invalid.");
        }
        
        // nonceを検証してリプレイ攻撃を防ぐ [cite: 186]
        String nonce = (String) idToken.getPayload().get("nonce");
        if (!expectedNonce.equals(nonce)) {
            throw new GeneralSecurityException("Invalid nonce.");
        }

        return idToken.getPayload();
    }
}