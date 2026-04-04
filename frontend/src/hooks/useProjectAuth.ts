import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { api, getAuth, getJwt, isGuestAdmin, type ProjectAuth, saveAuth } from '@/api/client';
import { authApi, projectApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import type { ParticipantResponse } from '@/api/types';
import { logger } from '@/utils/logger';

interface UseProjectAuthResult {
  auth: ProjectAuth;
  isAdmin: boolean;
}

export function useProjectAuth(slug: string | undefined): UseProjectAuthResult {
  const [auth, setAuth] = useState<ProjectAuth>(() => (slug ? getAuth(slug) : {}));
  const jwt = getJwt();
  const qc = useQueryClient();
  const autoJoinAttempted = useRef(false);

  // Fallback: logged-in owner without adminCode in localStorage
  const { data: adminProject } = useQuery({
    queryKey: queryKeys.projects.admin(slug as string),
    queryFn: () => projectApi.admin(slug as string),
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

  // Check if logged-in user has a participant record linked to their account.
  // Always check for JWT users - localStorage nickname may be from a guest session.
  const { data: myParticipant, isError: myParticipantNotFound } = useQuery({
    queryKey: queryKeys.projects.myParticipant(slug as string),
    queryFn: () => projectApi.myParticipant(slug as string),
    enabled: Boolean(slug && jwt),
    retry: false,
  });

  useEffect(() => {
    if (myParticipant?.id && slug) {
      saveAuth(slug, { nickname: myParticipant.nickname });
      setAuth(getAuth(slug));
    }
  }, [myParticipant, slug]);

  // Auto-join: logged-in user who is not yet a participant in this project
  const { data: me } = useQuery({
    queryKey: queryKeys.auth.me,
    queryFn: () => authApi.me(),
    enabled: Boolean(jwt && myParticipantNotFound && slug && !autoJoinAttempted.current),
  });

  const autoJoin = useMutation({
    mutationFn: (nickname: string) =>
      api<ParticipantResponse>(
        `/projects/${slug}/participants`,
        { method: 'POST', body: { nickname } },
        slug as string,
      ),
    onSuccess: (participant) => {
      if (slug) {
        saveAuth(slug, { nickname: participant.nickname });
        setAuth(getAuth(slug));
        void qc.invalidateQueries({ queryKey: queryKeys.projects.myParticipant(slug) });
        logger.debug(`Auto-joined project ${slug} as ${participant.nickname}`);
      }
    },
  });

  useEffect(() => {
    if (me?.username && slug && myParticipantNotFound && !autoJoinAttempted.current) {
      autoJoinAttempted.current = true;
      autoJoin.mutate(me.username);
    }
  }, [me, slug, myParticipantNotFound, autoJoin]);

  const guestIsAdmin = useMemo(() => isGuestAdmin(auth), [auth]);

  return {
    auth,
    isAdmin: Boolean(auth.adminCode) || guestIsAdmin,
  };
}
