import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api, saveAuth, setJwt, setIdentity } from './client';
import { queryKeys } from './queryKeys';
import type {
  RoomResponse,
  ParticipantResponse,
  ProjectAdminResponse,
  StoryPoints,
  AuthResponse,
  AdminUserResponse,
  AdminCreateUserRequest,
  AdminUpdateUserRequest,
  ChangePasswordRequest,
  AdminResetPasswordRequest,
  RoomType,
  FinishSessionResponse,
  GuestTokenResponse,
} from './types';

// --- Auth mutations ---

export function useLogin() {
  return useMutation({
    mutationFn: async (body: { username: string; password: string; rememberMe?: boolean }) => {
      const res = await api<AuthResponse>('/auth/login', { method: 'POST', body });
      setJwt(res.token);
      setIdentity(body.username);
      return res;
    },
  });
}

export function useRegister() {
  return useMutation({
    mutationFn: async (body: { username: string; password: string; email?: string }) => {
      const res = await api<AuthResponse>('/auth/register', { method: 'POST', body });
      setJwt(res.token);
      setIdentity(body.username);
      return res;
    },
  });
}

export function useChangePassword() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: ChangePasswordRequest) =>
      api<void>('/auth/me/password', { method: 'PUT', body }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.auth.me });
    },
  });
}

// --- Project mutations ---

export function useCreateProject() {
  return useMutation({
    mutationFn: (body: { name: string }) =>
      api<ProjectAdminResponse>('/projects', { method: 'POST', body }),
  });
}

export function useUpdateProject(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { name?: string }) =>
      api<ProjectAdminResponse>(`/projects/${slug}`, { method: 'PATCH', body }, slug),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.projects.detail(slug) });
    },
  });
}

export function useDeleteProject(slug: string) {
  return useMutation({
    mutationFn: () =>
      api<void>(`/projects/${slug}`, { method: 'DELETE' }, slug),
  });
}

// --- Room mutations ---

export function useCreateRoom(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { title: string; description?: string; deadline?: string; roomType: RoomType; autoRevealOnDeadline?: boolean; commentTemplate?: string; commentRequired?: boolean }) =>
      api<RoomResponse>(`/projects/${slug}/rooms`, { method: 'POST', body }, slug),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.projects.rooms(slug) });
    },
  });
}

export function useRevealRoom(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (roomId: string) =>
      api(`/rooms/${roomId}/reveal`, { method: 'POST' }, slug),
    onSuccess: (_data, roomId) => {
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.admin(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.detail(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.live(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.history(roomId) });
    },
  });
}

export function useReopenRoom(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (roomId: string) =>
      api(`/rooms/${roomId}/reopen`, { method: 'POST' }, slug),
    onSuccess: (_data, roomId) => {
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.admin(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.projects.rooms(slug) });
    },
  });
}

export function useNewRound(slug: string, roomId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { topic?: string }) =>
      api(`/rooms/${roomId}/new-round`, { method: 'POST', body }, slug),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.live(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.history(roomId) });
    },
  });
}

export function useFinishSession(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ roomId, revealVotes }: { roomId: string; revealVotes: boolean }) =>
      api<FinishSessionResponse>(`/rooms/${roomId}/finish?revealVotes=${revealVotes}`, { method: 'POST' }, slug),
    onSuccess: (_data, { roomId }) => {
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.admin(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.detail(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.history(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.projects.rooms(slug) });
    },
  });
}

export function useDeleteRoom(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (roomId: string) =>
      api<void>(`/rooms/${roomId}`, { method: 'DELETE' }, slug),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.projects.rooms(slug) });
    },
  });
}

export function useUpdateRoom(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ roomId, body }: { roomId: string; body: Record<string, unknown> }) =>
      api(`/rooms/${roomId}`, { method: 'PATCH', body }, slug),
    onSuccess: (_data, { roomId }) => {
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.admin(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.live(roomId) });
      void qc.invalidateQueries({ queryKey: queryKeys.projects.rooms(slug) });
    },
  });
}

// --- Task mutations ---

export function useAddTask(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ roomId, title, description }: { roomId: string; title: string; description?: string }) =>
      api(`/rooms/${roomId}/tasks`, { method: 'POST', body: { title, description } }, slug),
    onSuccess: (_data, { roomId }) => {
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.admin(roomId) });
    },
  });
}

export function useImportTasks(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ roomId, titles }: { roomId: string; titles: string[] }) =>
      api(`/rooms/${roomId}/tasks/import`, { method: 'POST', body: { titles } }, slug),
    onSuccess: (_data, { roomId }) => {
      void qc.invalidateQueries({ queryKey: queryKeys.rooms.admin(roomId) });
    },
  });
}

export function useUpdateTask(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, body }: { taskId: string; body: Record<string, unknown> }) =>
      api(`/tasks/${taskId}`, { method: 'PATCH', body }, slug),
    onSuccess: () => {
      // Invalidate all room queries since we don't know which room
      void qc.invalidateQueries({ queryKey: ['rooms'] });
    },
  });
}

export function useDeleteTask(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (taskId: string) =>
      api(`/tasks/${taskId}`, { method: 'DELETE' }, slug),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['rooms'] });
    },
  });
}

// --- Estimate mutations ---

export function useSubmitEstimate(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, storyPoints, comment }: { taskId: string; storyPoints?: StoryPoints | null; comment?: string }) =>
      api(`/tasks/${taskId}/estimates`, { method: 'POST', body: { storyPoints: storyPoints || undefined, comment: comment || undefined } }, slug),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['rooms'] });
    },
  });
}

export function useDeleteEstimate(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (taskId: string) =>
      api(`/tasks/${taskId}/estimates`, { method: 'DELETE' }, slug),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['rooms'] });
    },
  });
}

export function useSetFinalEstimate(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, storyPoints }: { taskId: string; storyPoints: StoryPoints }) =>
      api(`/tasks/${taskId}/final-estimate`, { method: 'PUT', body: { storyPoints } }, slug),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['rooms'] });
    },
  });
}

// --- Participant mutations ---

export function useJoinProject(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ nickname, roomId }: { nickname: string; roomId?: string }) =>
      api<ParticipantResponse>(
        `/projects/${slug}/participants`,
        { method: 'POST', body: { nickname, ...(roomId && { roomId }) } },
        slug,
      ),
    onSuccess: (participant) => {
      if (participant.token) {
        saveAuth(slug, { guestToken: participant.token, nickname: participant.nickname });
      } else {
        saveAuth(slug, { nickname: participant.nickname });
      }
      void qc.invalidateQueries({ queryKey: queryKeys.projects.participants(slug) });
    },
  });
}

export function useDeleteParticipant(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (participantId: string) =>
      api(`/projects/${slug}/participants/${participantId}`, { method: 'DELETE' }, slug),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: queryKeys.projects.participants(slug) });
      void qc.invalidateQueries({ queryKey: ['rooms'] });
    },
  });
}

export function useAdminJoinMutation(slug: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (nickname: string) =>
      api<ParticipantResponse>(
        `/projects/${slug}/participants`,
        { method: 'POST', body: { nickname } },
        slug,
      ),
    onSuccess: (participant) => {
      if (participant.token) {
        saveAuth(slug, { guestToken: participant.token, nickname: participant.nickname });
      } else {
        saveAuth(slug, { nickname: participant.nickname });
      }
      void qc.invalidateQueries({ queryKey: queryKeys.projects.participants(slug) });
      void qc.invalidateQueries({ queryKey: ['rooms'] });
    },
  });
}

// --- Admin mutations ---

export function useAdminCreateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: AdminCreateUserRequest) =>
      api<AdminUserResponse>('/admin/users', { method: 'POST', body }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });
}

export function useAdminUpdateUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: AdminUpdateUserRequest }) =>
      api<AdminUserResponse>(`/admin/users/${id}`, { method: 'PATCH', body }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });
}

export function useAdminResetPassword() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id: string; body: AdminResetPasswordRequest }) =>
      api<void>(`/admin/users/${id}/password`, { method: 'PUT', body }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });
}

export function useAdminDeleteUser() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) =>
      api<void>(`/admin/users/${id}`, { method: 'DELETE' }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['admin', 'users'] });
    },
  });
}

// --- Guest JWT mutations ---

export function useExchangeAdminCode() {
  return useMutation({
    mutationFn: (body: { slug: string; adminCode: string }) =>
      api<GuestTokenResponse>('/auth/guest/admin-exchange', { method: 'POST', body }),
  });
}

export function useRefreshGuestToken() {
  return useMutation({
    mutationFn: (slug: string) =>
      api<GuestTokenResponse>('/auth/guest/refresh', { method: 'POST' }, slug),
  });
}
