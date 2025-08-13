import type { GetServerSideProps, NextPage } from 'next';

interface DashboardProps {
  user: {
    name: string;
    email: string;
  };
}

const Dashboard: NextPage<DashboardProps> = ({ user }) => {
  return (
    <div>
      <h1>Welcome, {user.name}</h1>
      <p>Your email is: {user.email}</p>
    </div>
  );
};

export const getServerSideProps: GetServerSideProps = async (context) => {
  const { req } = context;
  const token = req.cookies.sessionToken;

  if (!token) {
    return {
      redirect: {
        destination: '/login',
        permanent: false,
      },
    };
  }

  try {
    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
    const response = await fetch(`${apiBaseUrl}/api/user/me`, { // このエンドポイントはバックエンドで実装が必要です
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    if (!response.ok) throw new Error('Failed to fetch user');

    const user = await response.json();

    return {
      props: { user },
    };
  } catch (error) {
    console.error(error);
    return {
      redirect: {
        destination: '/login',
        permanent: false,
      },
    };
  }
};

export default Dashboard;
