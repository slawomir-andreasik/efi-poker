import { useQuery } from '@tanstack/react-query';
import { getJwt } from '@/api/client';
import { authApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';

export function useCurrentUser() {
  const jwt = getJwt();

  const { data: user, isLoading } = useQuery({
    queryKey: queryKeys.auth.me,
    queryFn: authApi.me,
    enabled: Boolean(jwt),
    staleTime: 5 * 60_000,
    retry: false,
  });

  return {
    user: user ?? null,
    isLoading: Boolean(jwt) && isLoading,
    isAdmin: user?.role === 'ADMIN',
    isLoggedIn: Boolean(jwt && user),
  };
}
