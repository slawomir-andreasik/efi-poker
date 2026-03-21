import { useState, useEffect, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getAuth, getJwt, saveAuth, isGuestAdmin, type ProjectAuth } from '@/api/client';
import { queryKeys } from '@/api/queryKeys';
import { projectApi } from '@/api/queries';

interface UseProjectAuthResult {
  auth: ProjectAuth;
  isAdmin: boolean;
}

export function useProjectAuth(slug: string | undefined): UseProjectAuthResult {
  const [auth, setAuth] = useState<ProjectAuth>(() => (slug ? getAuth(slug) : {}));
  const jwt = getJwt();

  // Fallback: logged-in owner without adminCode in localStorage
  const { data: adminProject } = useQuery({
    queryKey: queryKeys.projects.admin(slug!),
    queryFn: () => projectApi.admin(slug!),
    enabled: Boolean(slug && jwt && !auth.adminCode),
    retry: false,
  });

  useEffect(() => {
    if (adminProject?.adminCode && slug) {
      saveAuth(slug, {
        adminCode: adminProject.adminCode,
        projectName: adminProject.name,
        ...(adminProject.token && { guestToken: adminProject.token }),
      });
      setAuth(getAuth(slug));
    }
  }, [adminProject, slug]);

  // Fallback: logged-in user without nickname in localStorage
  const { data: myParticipant } = useQuery({
    queryKey: queryKeys.projects.myParticipant(slug!),
    queryFn: () => projectApi.myParticipant(slug!),
    enabled: Boolean(slug && jwt && !auth.nickname),
    retry: false,
  });

  useEffect(() => {
    if (myParticipant?.id && slug) {
      saveAuth(slug, { nickname: myParticipant.nickname });
      setAuth(getAuth(slug));
    }
  }, [myParticipant, slug]);

  const guestIsAdmin = useMemo(() => isGuestAdmin(auth), [auth]);

  return {
    auth,
    isAdmin: Boolean(auth.adminCode) || guestIsAdmin,
  };
}
