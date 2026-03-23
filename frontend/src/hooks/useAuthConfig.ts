import { useQuery } from '@tanstack/react-query';
import { authApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';

export function useAuthConfig() {
  const { data } = useQuery({
    queryKey: queryKeys.auth.config,
    queryFn: authApi.config,
    staleTime: Infinity,
  });

  return {
    auth0Enabled: data?.auth0Enabled ?? false,
    registrationEnabled: data?.registrationEnabled ?? false,
    ldapEnabled: data?.ldapEnabled ?? false,
  };
}
