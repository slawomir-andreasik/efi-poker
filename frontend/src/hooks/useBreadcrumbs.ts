import { useLocation, matchPath } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAuth, getLastActiveSlug } from '@/api/client';
import { queryKeys } from '@/api/queryKeys';
import { projectApi } from '@/api/queries';
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
  const isAdmin = Boolean(auth.adminCode);
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
  const segments: BreadcrumbSegment[] = [];

  if (!slug) {
    // Non-project routes
    if (path === '/login') {
      segments.push({ label: 'Login', path: null });
    } else if (path === '/register') {
      segments.push({ label: 'Register', path: null });
    } else if (path.startsWith('/admin')) {
      segments.push({ label: 'Admin', path: null });
    } else if (path !== '/') {
      segments.push({ label: 'Page Not Found', path: null });
    }
  } else {
    const isJoinPage = path === `/p/${slug}/join`;
    const isResultsPage = slug && roomId ? path === `/p/${slug}/r/${roomId}/results` : false;
    const isRoomAnalyticsPage = slug && roomId ? path === `/p/${slug}/r/${roomId}/analytics` : false;
    const isProjectAnalyticsPage = path === `/p/${slug}/analytics`;
    const isRoomPage = slug && roomId ? path.startsWith(`/p/${slug}/r/${roomId}`) && !isResultsPage && !isRoomAnalyticsPage : false;
    const isProjectPage = path === `/p/${slug}`;

    if (isProjectPage) {
      // Project page: project name with project switcher
      segments.push({ label: projectName, path: null, dropdownType: 'project' });
    } else if (isJoinPage) {
      // Join page: project (dropdown) + Join current
      segments.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
      segments.push({ label: 'Join', path: null });
    } else if (isProjectAnalyticsPage) {
      // Project analytics page: project (link) + Analytics current
      segments.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
      segments.push({ label: 'Analytics', path: null });
    } else if (isRoomAnalyticsPage && currentRoom) {
      // Room analytics page: project (dropdown) + room (link) + Analytics current
      segments.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
      segments.push({
        label: currentRoom.title,
        path: `/p/${slug}/r/${roomId}`,
        dropdownType: 'room',
        badges: { status: currentRoom.status, roomType: currentRoom.roomType },
      });
      segments.push({ label: 'Analytics', path: null });
    } else if (isRoomPage && currentRoom) {
      // Room page: project (dropdown) + room title (dropdown)
      segments.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
      segments.push({
        label: currentRoom.title,
        path: null,
        dropdownType: 'room',
        badges: { status: currentRoom.status, roomType: currentRoom.roomType },
      });
    } else if (isResultsPage && currentRoom) {
      // Results page: project (dropdown) + room (dropdown) + Results current
      segments.push({ label: projectName, path: `/p/${slug}`, dropdownType: 'project' });
      segments.push({
        label: currentRoom.title,
        path: `/p/${slug}/r/${roomId}`,
        dropdownType: 'room',
        badges: { status: currentRoom.status, roomType: currentRoom.roomType },
      });
      segments.push({ label: 'Results', path: null });
    } else if (slug) {
      // Unknown project sub-route - show project name as link
      segments.push({ label: projectName, path: `/p/${slug}` });
    }
  }

  return { segments, slug, roomId, currentRoom, isAdmin };
}
