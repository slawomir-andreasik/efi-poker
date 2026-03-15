import type { ProjectAdminResponse, ProjectResponse, RoomResponse, ParticipantResponse, RoomDetailResponse, RoomResultsResponse, ParticipantProgressResponse } from '@/api/types';

export function mockProject(overrides?: Partial<ProjectResponse>): ProjectResponse {
  return {
    id: '1',
    name: 'Project',
    slug: 'test-project',
    createdAt: '2026-01-01',
    ...overrides,
  };
}

export function mockProjectAdmin(overrides?: Partial<ProjectAdminResponse>): ProjectAdminResponse {
  return {
    id: '1',
    name: 'Sprint 42',
    slug: 'sprint-42',
    adminCode: 'abc',
    createdAt: '2026-01-01',
    ...overrides,
  };
}

export function mockParticipant(overrides?: Partial<ParticipantResponse>): ParticipantResponse {
  return {
    id: 'p-1',
    nickname: 'Alice',
    createdAt: '2026-01-01',
    ...overrides,
  };
}

export function mockRoom(overrides?: Partial<RoomResponse>): RoomResponse {
  return {
    id: 'room-123',
    slug: 'test-room',
    projectId: '1',
    title: 'Test',
    deadline: '2026-03-01T00:00:00Z',
    roundNumber: 1,
    autoRevealOnDeadline: true,
    roomType: 'ASYNC',
    status: 'OPEN',
    createdAt: '2026-01-01',
    ...overrides,
  };
}

export function mockRoomDetail(overrides?: Partial<RoomDetailResponse>): RoomDetailResponse {
  return {
    id: 'room-123',
    slug: 'test-room',
    title: 'Test',
    deadline: '2026-03-01T00:00:00Z',
    roundNumber: 1,
    autoRevealOnDeadline: true,
    roomType: 'ASYNC',
    status: 'OPEN',
    tasks: [],
    ...overrides,
  };
}

export function mockRoomResults(overrides?: Partial<RoomResultsResponse>): RoomResultsResponse {
  return {
    roomId: 'room-123',
    slug: 'test-room',
    title: 'Results',
    status: 'REVEALED',
    tasks: [],
    ...overrides,
  };
}

export function mockParticipantProgress(overrides?: Partial<ParticipantProgressResponse>): ParticipantProgressResponse {
  return {
    roomId: 'room-1',
    slug: 'A3X-K7B',
    totalTasks: 5,
    participants: [
      { participantId: 'p1', nickname: 'Alice', votedCount: 5, totalTasks: 5, hasCommentedAll: true },
      { participantId: 'p2', nickname: 'Bob', votedCount: 2, totalTasks: 5, hasCommentedAll: false },
      { participantId: 'p3', nickname: 'Charlie', votedCount: 0, totalTasks: 5 },
    ],
    ...overrides,
  };
}
