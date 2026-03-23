import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { matchPath, useLocation } from 'react-router-dom';
import { getAuth, getLastActiveSlug, isGuestAdmin } from '@/api/client';
import { projectApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import type { RoomResponse } from '@/api/types';

export interface BreadcrumbSegment {
  label: string;
  path: string | null; // null = current page (not clickable)
  dropdownType?: 'project' | 'room'; // which switcher dropdown to attach
  badges?: { status?: string; roomType?: string }; // room segment badges
}

interface UseBreadcrumbsResult {
  segments: BreadcrumbSegment[];
  slug: string | undefined;
  roomId: string | undefined;
  currentRoom: RoomResponse | undefined;
  isAdmin: boolean;
}

export function useBreadcrumbs(): UseBreadcrumbsResult {
  const location = useLocation();

  const roomMatch = matchPath('/p/:slug/r/:roomId/*', location.pathname);
  const projectMatch = matchPath('/p/:slug/*', location.pathname);
  const slug = roomMatch?.params.slug ?? projectMatch?.params.slug;
  const roomId = roomMatch?.params.roomId;

  const effectiveSlug = slug ?? getLastActiveSlug() ?? undefined;
  const auth = effectiveSlug ? getAuth(effectiveSlug) : {};
  const isAdmin = Boolean(auth.adminCode) || isGuestAdmin(auth);
  const projectName = auth.projectName ?? slug ?? '';

  // Fetch rooms for room title (TanStack cache deduplicates with other queries)
  const { data: rooms } = useQuery({
    queryKey: queryKeys.projects.rooms(slug!),
    queryFn: () => projectApi.rooms(slug!),
    enabled: Boolean(slug),
    staleTime: 10_000,
  });
  const currentRoom = rooms?.find((r) => r.id === roomId);

  const path = location.pathname;

  const segments = useMemo(() => {
    const result: BreadcrumbSegment[] = [];

    if (!slug) {
      // Non-project routes
      if (path === '/login') {
        result.push({ label: 'Login', path: null });
      } else if (path === '/register') {
        result.push({ label: 'Register', path: null });
      } else if (path.startsWith('/admin')) {
        result.push({ label: 'Admin', path: null });
      } else if (path !== '/') {
        result.push({ label: 'Page Not Found', path: null });
      }
    } else {
      const isJoinPage = path === `/p/${slug}/join`;
      const isResultsPage = slug && roomId ? path === `/p/${slug}/r/${roomId}/results` : false;
      const isRoomAnalyticsPage =
        slug && roomId ? path === `/p/${slug}/r/${roomId}/analytics` : false;
      const isProjectAnalyticsPage = path === `/p/${slug}/analytics`;
      const isRoomPage =
        slug && roomId
          ? path.startsWith(`/p/${slug}/r/${roomId}`) && !isResultsPage && !isRoomAnalyticsPage
          : false;
      const isProjectPage = path === `/p/${slug}`;

      if (isProjectPage) {
        // Project page: project name with project switcher
        result.push({ label: projectName, path: null, dropdownType: 'project' });
      } else if (isJoinPage) {
        // Join page: project (dropdown) + Join current
        result.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
        result.push({ label: 'Join', path: null });
      } else if (isProjectAnalyticsPage) {
        // Project analytics page: project (link) + Analytics current
        result.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
        result.push({ label: 'Analytics', path: null });
      } else if (isRoomAnalyticsPage && currentRoom) {
        // Room analytics page: project (dropdown) + room (link) + Analytics current
        result.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
        result.push({
          label: currentRoom.title,
          path: `/p/${slug}/r/${roomId}`,
          dropdownType: 'room',
          badges: { status: currentRoom.status, roomType: currentRoom.roomType },
        });
        result.push({ label: 'Analytics', path: null });
      } else if (isRoomPage && currentRoom) {
        // Room page: project (dropdown) + room title (dropdown)
        result.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
        result.push({
          label: currentRoom.title,
          path: null,
          dropdownType: 'room',
          badges: { status: currentRoom.status, roomType: currentRoom.roomType },
        });
      } else if (isResultsPage && currentRoom) {
        // Results page: project (dropdown) + room (dropdown) + Results current
        result.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
        result.push({
          label: currentRoom.title,
          path: `/p/${slug}/r/${roomId}`,
          dropdownType: 'room',
          badges: { status: currentRoom.status, roomType: currentRoom.roomType },
        });
        result.push({ label: 'Results', path: null });
      } else if (slug) {
        // Unknown project sub-route - show project name as link
        result.push({ label: projectName, path: `/p/${slug}` });
      }
    }

    return result;
    // biome-ignore lint/correctness/useExhaustiveDependencies: granular deps avoid re-render on every poll (currentRoom ref changes each cycle)
  }, [
    path,
    slug,
    roomId,
    currentRoom?.id,
    currentRoom?.title,
    currentRoom?.status,
    currentRoom?.roomType,
    projectName,
  ]);

  return { segments, slug, roomId, currentRoom, isAdmin };
}
