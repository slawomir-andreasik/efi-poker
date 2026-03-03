import { api } from './client';
import type {
  ProjectResponse,
  ProjectAdminResponse,
  RoomResponse,
  RoomDetailResponse,
  RoomAdminResponse,
  RoomResultsResponse,
  RoomSlugResponse,
  ParticipantResponse,
  LiveRoomStateResponse,
  RoundHistoryEntry,
  AuthConfigResponse,
  UserResponse,
  PagedUsersResponse,
  AdminUserResponse,
} from './types';

export const projectApi = {
  detail: (slug: string) => api<ProjectResponse>(`/projects/${slug}`, {}, slug),
  rooms: (slug: string) => api<RoomResponse[]>(`/projects/${slug}/rooms`, {}, slug),
  participants: (slug: string) => api<ParticipantResponse[]>(`/projects/${slug}/participants`, {}, slug),
  admin: (slug: string) => api<ProjectAdminResponse>(`/projects/${slug}/admin`, {}, slug),
};

export const roomApi = {
  detail: (id: string, slug: string) => api<RoomDetailResponse>(`/rooms/${id}`, {}, slug),
  live: (id: string, slug: string) => api<LiveRoomStateResponse>(`/rooms/${id}/live`, {}, slug),
  admin: (id: string, slug: string) => api<RoomAdminResponse>(`/rooms/${id}/admin`, {}, slug),
  results: (id: string, slug: string) => api<RoomResultsResponse>(`/rooms/${id}/results`, {}, slug),
  history: (id: string, slug: string) => api<RoundHistoryEntry[]>(`/rooms/${id}/history`, {}, slug),
  resultsExport: (id: string, slug: string) => api<string>(`/rooms/${id}/results/export`, {}, slug),
  bySlug: (slug: string) => api<RoomSlugResponse>(`/rooms/by-slug/${slug}`),
};

export const authApi = {
  config: () => api<AuthConfigResponse>('/auth/config'),
  me: () => api<UserResponse>('/auth/me'),
  myProjects: () => api<ProjectAdminResponse[]>('/auth/me/projects'),
  participatedProjects: () => api<ProjectResponse[]>('/auth/me/participated-projects'),
};

export const adminApi = {
  users: (page: number, size: number, search?: string) => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (search) params.set('search', search);
    return api<PagedUsersResponse>(`/admin/users?${params}`);
  },
  user: (id: string) => api<AdminUserResponse>(`/admin/users/${id}`),
};
