import React from 'react';

const LoginPage = () => {
  const handleGoogleLogin = () => {
    // この値はバックエンドから取得するのが理想ですが、ここでは例としてフロントで構築します
    const client_id = "YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com"; // 自身のクライアントIDに置き換えてください
    const redirect_uri = "http://localhost:3000/api/auth/callback"; // BFFのURL
    const scope = "openid email profile";
    const response_type = "code";
    
    // 本番環境では、これらの値はサーバーサイドで生成し、セッションに保存すべきです
    const state = "aF0W8qV2z_SOME_RANDOM_STRING";
    const nonce = "n-0S6_WzA2Mj_SOME_RANDOM_STRING";
    const code_challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"; // PKCE code_verifierから生成した値
    const code_challenge_method = "S256";

    const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?client_id=${client_id}&redirect_uri=${redirect_uri}&response_type=${response_type}&scope=${scope}&state=${state}&nonce=${nonce}&code_challenge=${code_challenge}&code_challenge_method=${code_challenge_method}`;
    
    window.location.href = authUrl;
  };

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
      <button onClick={handleGoogleLogin} style={{ padding: '10px 20px', fontSize: '16px' }}>
        Sign in with Google
      </button>
    </div>
  );
};

export default LoginPage;
