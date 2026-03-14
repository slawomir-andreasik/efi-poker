export const queryKeys = {
  projects: {
    detail: (slug: string) => ['projects', slug] as const,
    rooms: (slug: string) => ['projects', slug, 'rooms'] as const,
    participants: (slug: string) => ['projects', slug, 'participants'] as const,
    admin: (slug: string) => ['projects', slug, 'admin'] as const,
    myParticipant: (slug: string) => ['projects', slug, 'my-participant'] as const,
  },
  rooms: {
    detail: (id: string) => ['rooms', id] as const,
    live: (id: string) => ['rooms', id, 'live'] as const,
    admin: (id: string) => ['rooms', id, 'admin'] as const,
    results: (id: string) => ['rooms', id, 'results'] as const,
    history: (id: string) => ['rooms', id, 'history'] as const,
    bySlug: (slug: string) => ['rooms', 'by-slug', slug] as const,
  },
  auth: {
    config: ['auth', 'config'] as const,
    me: ['auth', 'me'] as const,
    myProjects: ['auth', 'me', 'projects'] as const,
    participatedProjects: ['auth', 'me', 'participated-projects'] as const,
  },
  admin: {
    users: (page: number, size: number, search?: string) =>
      ['admin', 'users', { page, size, search }] as const,
    user: (id: string) => ['admin', 'users', id] as const,
  },
  analytics: {
    room: (roomId: string) => ['analytics', 'room', roomId] as const,
    project: (slug: string) => ['analytics', 'project', slug] as const,
  },
};
