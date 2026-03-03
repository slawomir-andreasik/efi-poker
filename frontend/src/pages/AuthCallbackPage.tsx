import { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { setJwt, setIdentity } from '@/api/client';
import { Spinner } from '@/components/Spinner';

export function AuthCallbackPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const token = searchParams.get('token');
    const error = searchParams.get('error');

    if (token) {
      setJwt(token);
      const name = searchParams.get('name');
      if (name) setIdentity(name);
      navigate('/', { replace: true });
    } else {
      const message = error ? `auth_failed: ${error}` : 'auth_failed';
      navigate(`/?error=${encodeURIComponent(message)}`, { replace: true });
    }
  }, [searchParams, navigate]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
      <Spinner className="h-12 w-12" />
      <p className="text-efi-text-secondary text-sm">Authenticating...</p>
    </div>
  );
}
