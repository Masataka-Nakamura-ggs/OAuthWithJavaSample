import type { GetServerSideProps } from 'next';

// このページコンポーネント自体は、リダイレクトされるため実際には表示されません。
// そのため、中身は何もレンダリングしない`null`を返すだけで十分です。
const HomePage = () => {
  return null;
};

// サーバーサイドで実行される関数
// ページがレンダリングされる前に、この関数が呼び出されます。
export const getServerSideProps: GetServerSideProps = async (context) => {
  // ここでリダイレクトの設定を返します。
  return {
    redirect: {
      destination: '/login', // リダイレクト先のパス
      permanent: false,      // 永続的なリダイレクトではない(307 Temporary Redirect)
    },
  };
};

export default HomePage;
