/**
 * API URL integration tests: verify that each page calls the correct
 * backend endpoints with correct HTTP methods and request bodies.
 * These tests mock fetch and check the actual URLs hit by each page.
 */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../helpers';
import { setAdminAuth, setGuestTokenAuth, setIdentity, fakeJwt } from '../auth-helpers';
import { mockProjectAdmin, mockParticipant, mockProject, mockRoomDetail, mockRoomResults } from '../fixtures';

// Must mock react-router-dom params before importing pages
const mockParams = vi.hoisted(() => ({ slug: 'test-project', roomId: 'room-123', roomSlug: 'test-room' }));
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => mockParams,
    useNavigate: () => vi.fn(),
  };
});

import { HomePage } from '@/pages/HomePage';
import { JoinPage } from '@/pages/JoinPage';
import { ProjectPage } from '@/pages/ProjectPage';
import { RoomPage } from '@/pages/RoomPage';
import { ResultsPage } from '@/pages/ResultsPage';
import { RoomJoinRedirectPage } from '@/pages/RoomJoinRedirectPage';

interface FetchCall {
  url: string;
  method: string;
  body: string | null;
}

let calls: FetchCall[];

function routedFetch(routes: Record<string, unknown>): typeof globalThis.fetch {
  return vi.fn(async (input: string | URL | Request, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
    const method = init?.method || 'GET';
    const body = init?.body as string | null ?? null;
    calls.push({ url, method, body });

    // Find matching route (prefix match)
    let responseBody: unknown = {};
    for (const [pattern, value] of Object.entries(routes)) {
      if (url.includes(pattern)) {
        responseBody = value;
        break;
      }
    }

    return new Response(JSON.stringify(responseBody), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    });
  });
}

beforeEach(() => {
  calls = [];
  localStorage.clear();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('HomePage API calls', () => {
  it('should POST /projects with project name', async () => {
    setIdentity('Alice');

    const user = userEvent.setup();
    globalThis.fetch = routedFetch({
      '/api/v1/projects': mockProjectAdmin(),
      '/participants': mockParticipant(),
    });

    renderWithProviders(<HomePage />);

    // Open create form
    const newProjectButtons = screen.getAllByRole('button', { name: /New Project/ });
    await user.click(newProjectButtons[0]!);

    await user.type(screen.getByLabelText('Project Name'), 'Sprint 42');
    await user.click(screen.getByRole('button', { name: 'Create Project' }));

    await waitFor(() => {
      expect(calls.some((c) => c.method === 'POST' && c.url === '/api/v1/projects')).toBe(true);
    });

    const postCall = calls.find((c) => c.method === 'POST' && c.url === '/api/v1/projects')!;
    expect(postCall.url).toBe('/api/v1/projects');
    expect(JSON.parse(postCall.body!)).toMatchObject({ name: 'Sprint 42' });
  });

});

describe('JoinPage API calls', () => {
  it('should POST /projects/{slug}/participants with nickname', async () => {
    const user = userEvent.setup();
    globalThis.fetch = routedFetch({
      '/participants': mockParticipant(),
    });

    renderWithProviders(<JoinPage />);

    await user.type(screen.getByLabelText('Your Nickname'), 'Alice');
    await user.click(screen.getByRole('button', { name: 'Join' }));

    await waitFor(() => {
      expect(calls.some((c) => c.method === 'POST')).toBe(true);
    });

    const postCall = calls.find((c) => c.method === 'POST')!;
    expect(postCall.url).toBe('/api/v1/projects/test-project/participants');
    expect(JSON.parse(postCall.body!)).toEqual({ nickname: 'Alice' });
  });
});

describe('ProjectPage API calls', () => {
  it('should fetch project and rooms separately', async () => {
    globalThis.fetch = routedFetch({
      '/rooms': [],
      '/projects/': mockProject(),
    });

    renderWithProviders(<ProjectPage />);

    await waitFor(() => {
      expect(calls.length).toBeGreaterThanOrEqual(2);
    });

    const urls = calls.map((c) => c.url);
    expect(urls).toContain('/api/v1/projects/test-project');
    expect(urls).toContain('/api/v1/projects/test-project/rooms');
  });

  it('should fetch project, rooms and participants when admin', async () => {
    setAdminAuth('test-project', 'admin-code');
    globalThis.fetch = routedFetch({
      '/rooms': [],
      '/participants': [],
      '/projects/': mockProject(),
    });

    renderWithProviders(<ProjectPage />);

    await waitFor(() => {
      expect(calls.length).toBeGreaterThanOrEqual(3);
    });

    const urls = calls.map((c) => c.url);
    expect(urls).toContain('/api/v1/projects/test-project');
    expect(urls).toContain('/api/v1/projects/test-project/rooms');
    expect(urls).toContain('/api/v1/projects/test-project/participants');
  });
});

describe('RoomPage API calls', () => {
  it('should fetch room from flat path /rooms/{id}', async () => {
    setGuestTokenAuth('test-project', fakeJwt({ participantId: 'p-1' }));
    globalThis.fetch = routedFetch({
      '/rooms/': { id: 'room-123', title: 'Test', status: 'OPEN', roomType: 'ASYNC', deadline: '2026-03-01T00:00:00Z', roundNumber: 1, tasks: [] },
    });

    renderWithProviders(<RoomPage />);

    await waitFor(() => {
      expect(calls.length).toBeGreaterThanOrEqual(1);
    });

    const urls = calls.map((c) => c.url);
    expect(urls).toContain('/api/v1/rooms/room-123');

    // Verify NO nested project path
    const nestedCalls = calls.filter((c) => c.url.includes('/projects/') && c.url.includes('/rooms/'));
    expect(nestedCalls).toHaveLength(0);
  });

  it('should POST estimate with storyPoints string to /tasks/{id}/estimates', async () => {
    const user = userEvent.setup();
    setGuestTokenAuth('test-project', fakeJwt({ participantId: 'p-1' }));
    globalThis.fetch = routedFetch({
      '/estimates': {},
      '/rooms/': {
        id: 'room-123', title: 'Test', status: 'OPEN', roomType: 'ASYNC', deadline: '2026-03-01T00:00:00Z', roundNumber: 1,
        tasks: [{ id: 'task-1', title: 'Task One', sortOrder: 0, myEstimate: null, allEstimates: null, votedCount: 0, totalParticipants: 3, revealed: false, active: false }],
      },
    });

    renderWithProviders(<RoomPage />);

    await waitFor(() => {
      expect(screen.getByText('Task One')).toBeInTheDocument();
    });

    calls.length = 0;
    const fiveButtons = screen.getAllByRole('button', { name: '5' });
    await user.click(fiveButtons[0]!);

    await waitFor(() => {
      expect(calls.some((c) => c.method === 'POST')).toBe(true);
    });

    const estimateCall = calls.find((c) => c.method === 'POST' && c.url.includes('/estimates'))!;
    expect(estimateCall.url).toBe('/api/v1/tasks/task-1/estimates');
    const body = JSON.parse(estimateCall.body!);
    expect(body).toEqual({ storyPoints: '5' });
    expect(typeof body.storyPoints).toBe('string');
  });

  it('should DELETE /tasks/{id}/estimates when clicking selected SP (unvote)', async () => {
    const user = userEvent.setup();
    setGuestTokenAuth('test-project', fakeJwt({ participantId: 'p-1' }));
    globalThis.fetch = routedFetch({
      '/estimates': {},
      '/rooms/': mockRoomDetail({
        tasks: [{ id: 'task-1', title: 'Task One', sortOrder: 0, myEstimate: { id: 'e-1', participantId: 'p-1', participantNickname: 'Alice', storyPoints: '5', createdAt: '2026-01-01' }, allEstimates: [], votedCount: 1, totalParticipants: 3, revealed: false, active: false, description: undefined, averagePoints: null, medianPoints: null, finalEstimate: null, questionVotesCount: 0 }],
      }),
    });

    renderWithProviders(<RoomPage />);

    await waitFor(() => {
      expect(screen.getByText('Task One')).toBeInTheDocument();
    });

    calls.length = 0;
    // Click the already-selected "5" button to unvote
    const fiveButtons = screen.getAllByRole('button', { name: '5' });
    await user.click(fiveButtons[0]!);

    await waitFor(() => {
      expect(calls.some((c) => c.method === 'DELETE')).toBe(true);
    });

    const deleteCall = calls.find((c) => c.method === 'DELETE' && c.url.includes('/estimates'))!;
    expect(deleteCall.url).toBe('/api/v1/tasks/task-1/estimates');
    expect(deleteCall.body).toBeNull();
  });
});

describe('ResultsPage API calls', () => {
  it('should fetch results from flat path /rooms/{id}/results', async () => {
    globalThis.fetch = routedFetch({
      '/results': mockRoomResults(),
    });

    renderWithProviders(<ResultsPage />);

    await waitFor(() => {
      expect(calls.length).toBeGreaterThanOrEqual(1);
    });

    expect(calls[0]!.url).toBe('/api/v1/rooms/room-123/results');

    // Verify NO nested project path
    const nestedCalls = calls.filter((c) => c.url.includes('/projects/'));
    expect(nestedCalls).toHaveLength(0);
  });
});

describe('RoomJoinRedirectPage API calls', () => {
  it('should GET /rooms/by-slug/{slug}', async () => {
    globalThis.fetch = routedFetch({
      '/rooms/by-slug/': {
        roomId: 'room-123',
        roomTitle: 'Test Room',
        roomSlug: 'test-room',
        projectSlug: 'test-project',
        projectName: 'Test Project',
      },
    });

    renderWithProviders(<RoomJoinRedirectPage />);

    await waitFor(() => {
      expect(calls.some((c) => c.url.includes('/rooms/by-slug/'))).toBe(true);
    });

    expect(calls[0]!.url).toBe('/api/v1/rooms/by-slug/test-room');
  });
});
