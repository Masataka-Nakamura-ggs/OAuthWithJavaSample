import type { NextApiRequest, NextApiResponse } from 'next';
import axios from 'axios';
import { serialize } from 'cookie';

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  const { code, state } = req.query;

  if (!code || !state) {
    return res.status(400).json({ error: 'Invalid request from Google' });
  }

  try {
    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
    
    // BFFがバックエンドに認可コードとstateを送信
    const response = await axios.post(`${apiBaseUrl}/api/auth/google/callback`, {
      code: code as string,
      state: state as string,
      // 本番実装では、PKCEのcode_verifierをCookieなどから読み取り、ここに含める必要があります
    });

    const { sessionToken } = response.data;

    // バックエンドから受け取ったセッショントークンをHttpOnlyクッキーに設定
    const cookie = serialize('sessionToken', sessionToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV !== 'development',
      maxAge: 60 * 60 * 24 * 7, // 1週間
      sameSite: 'lax',
      path: '/',
    });
    res.setHeader('Set-Cookie', cookie);
    
    // ログイン後のダッシュボードページにリダイレクト
    res.redirect('/dashboard');

  } catch (error) {
    console.error('Authentication failed:', error);
    // ユーザーにエラーがあったことを知らせるページにリダイレクトするなどの処理
    res.redirect('/login?error=auth_failed');
  }
}