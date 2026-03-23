import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, api } from './client';

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('ApiError class', () => {
  it('should store status and traceId', () => {
    const err = new ApiError('Not found', 404, 'abc123def456');
    expect(err.message).toBe('Not found');
    expect(err.status).toBe(404);
    expect(err.traceId).toBe('abc123def456');
    expect(err.name).toBe('ApiError');
  });

  it('should allow undefined traceId', () => {
    const err = new ApiError('Server error', 500);
    expect(err.status).toBe(500);
    expect(err.traceId).toBeUndefined();
  });
});

describe('api() silent refresh on 401', () => {
  it('should attempt refresh when user JWT returns 401', async () => {
    localStorage.setItem('efi-jwt', 'expired-token');

    const refreshResponse = new Response(
      JSON.stringify({ token: 'new-token', expiresAt: '2026-12-31T00:00:00Z' }),
      { status: 200, headers: { 'content-type': 'application/json' } },
    );
    const retryResponse = new Response(JSON.stringify({ ok: true }), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    });

    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response('{}', { status: 401, headers: { 'content-type': 'application/json' } }),
      )
      .mockResolvedValueOnce(refreshResponse)
      .mockResolvedValueOnce(retryResponse);
    globalThis.fetch = fetchMock;

    const result = await api<{ ok: boolean }>('/test');
    expect(result.ok).toBe(true);
    expect(localStorage.getItem('efi-jwt')).toBe('new-token');
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it('should clear auth when refresh fails', async () => {
    localStorage.setItem('efi-jwt', 'expired-token');
    localStorage.setItem('efi-identity', 'testuser');

    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        new Response('{}', { status: 401, headers: { 'content-type': 'application/json' } }),
      )
      .mockResolvedValueOnce(
        new Response('{}', { status: 403, headers: { 'content-type': 'application/json' } }),
      );
    globalThis.fetch = fetchMock;

    await expect(api('/test')).rejects.toThrow();
    expect(localStorage.getItem('efi-jwt')).toBeNull();
    expect(localStorage.getItem('efi-identity')).toBeNull();
  });
});

describe('api() error handling', () => {
  it('should throw ApiError with traceId from X-Trace-Id header', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          title: 'Not Found',
          detail: 'Room not found',
          status: 404,
          type: 'about:blank',
        }),
        {
          status: 404,
          headers: { 'content-type': 'application/json', 'X-Trace-Id': 'trace-abc-123' },
        },
      ),
    );

    await expect(api('/rooms/nonexistent')).rejects.toSatisfy((err: ApiError) => {
      expect(err).toBeInstanceOf(ApiError);
      expect(err.status).toBe(404);
      expect(err.traceId).toBe('trace-abc-123');
      expect(err.message).toBe('Room not found');
      return true;
    });
  });

  it('should use detail field from ProblemDetail body', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          title: 'Bad Request',
          detail: 'Deadline is required for ASYNC rooms',
          status: 400,
        }),
        {
          status: 400,
          headers: { 'content-type': 'application/json' },
        },
      ),
    );

    await expect(api('/projects/test/rooms', { method: 'POST', body: {} })).rejects.toThrow(
      'Deadline is required for ASYNC rooms',
    );
  });

  it('should fall back to title when detail is missing', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ title: 'Forbidden', status: 403 }), {
        status: 403,
        headers: { 'content-type': 'application/json' },
      }),
    );

    await expect(api('/projects/test/rooms', { method: 'POST', body: {} })).rejects.toThrow(
      'Forbidden',
    );
  });

  it('should handle non-JSON error response gracefully', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response('Internal Server Error', {
        status: 500,
        headers: { 'content-type': 'text/plain' },
      }),
    );

    await expect(api('/something')).rejects.toSatisfy((err: ApiError) => {
      expect(err).toBeInstanceOf(ApiError);
      expect(err.status).toBe(500);
      expect(err.message).toBe('Request failed');
      return true;
    });
  });

  it('should not set traceId when header is absent', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ title: 'Not Found', status: 404 }), {
        status: 404,
        headers: { 'content-type': 'application/json' },
      }),
    );

    await expect(api('/rooms/nonexistent')).rejects.toSatisfy((err: ApiError) => {
      expect(err).toBeInstanceOf(ApiError);
      expect(err.traceId).toBeUndefined();
      return true;
    });
  });
});
