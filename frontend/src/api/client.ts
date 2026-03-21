import { logger } from '@/utils/logger';
import { getTracingHeaders } from '@/lib/tracing';

const BASE_URL = '/api/v1';

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly traceId?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export const STORAGE_KEYS = {
  PROJECTS: 'efi-projects',
  JWT: 'efi-jwt',
  BOARDS_LEGACY: 'efi-boards',
  LAST_SLUG: 'efi-last-slug',
  IDENTITY: 'efi-identity',
  DRAFT_COMMENTS: 'efi-draft-comments',
} as const;

const STORAGE_KEY = STORAGE_KEYS.PROJECTS;
const JWT_KEY = STORAGE_KEYS.JWT;

export function getJwt(): string | null {
  return localStorage.getItem(JWT_KEY);
}

export function setJwt(token: string): void {
  localStorage.setItem(JWT_KEY, token);
}

export function removeJwt(): void {
  localStorage.removeItem(JWT_KEY);
}

export interface ProjectAuth {
  guestToken?: string;
  adminCode?: string;
  nickname?: string;
  projectName?: string;
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  headers?: Record<string, string>;
}

// Migrate localStorage key from old 'efi-boards' to 'efi-projects'
function migrateStorage(): void {
  const old = localStorage.getItem(STORAGE_KEYS.BOARDS_LEGACY);
  if (old && !localStorage.getItem(STORAGE_KEY)) {
    localStorage.setItem(STORAGE_KEY, old);
    localStorage.removeItem(STORAGE_KEYS.BOARDS_LEGACY);
  }
}
migrateStorage();

export function saveAuth(slug: string, data: Partial<ProjectAuth>) {
  const projects = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') as Record<string, unknown>;
  const existing = (projects[slug] || {}) as Record<string, unknown>;
  projects[slug] = { ...existing, ...data };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(projects));
}

export function getAllProjects(): Record<string, ProjectAuth> {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') as Record<string, ProjectAuth>;
  } catch {
    return {};
  }
}

export function removeProject(slug: string) {
  try {
    const projects = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') as Record<string, unknown>;
    delete projects[slug];
    localStorage.setItem(STORAGE_KEY, JSON.stringify(projects));
  } catch {
    // ignore
  }
}

export function getAuth(slug: string): ProjectAuth {
  try {
    const projects = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') as Record<string, ProjectAuth>;
    return projects[slug] || {};
  } catch {
    return {};
  }
}

export async function api<T>(path: string, options: RequestOptions = {}, slug?: string): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.headers,
  };

  // Auth priority: user JWT > per-project guest JWT
  const jwt = getJwt();
  if (jwt) {
    headers['Authorization'] = `Bearer ${jwt}`;
  } else if (slug) {
    const auth = getAuth(slug);
    if (auth.guestToken) {
      headers['Authorization'] = `Bearer ${auth.guestToken}`;
    }
  }

  const method = options.method || 'GET';

  logger.debug(`API ${method} ${path}`);
  Object.assign(headers, getTracingHeaders());

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
      signal: AbortSignal.timeout(30_000),
    });
  } catch (err) {
    logger.error(`API network error ${method} ${path}`);
    throw err;
  }

  // If 401 with auth, token is expired/invalid - clear and retry without auth
  if (response.status === 401 && headers['Authorization']) {
    if (jwt) {
      logger.warn(`User JWT expired/invalid, clearing and retrying ${method} ${path}`);
      removeJwt();
    } else if (slug) {
      logger.warn(`Guest JWT expired/invalid, clearing and retrying ${method} ${path}`);
      saveAuth(slug, { guestToken: undefined });
    }
    delete headers['Authorization'];
    try {
      response = await fetch(`${BASE_URL}${path}`, {
        method,
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined,
        signal: AbortSignal.timeout(30_000),
      });
    } catch (err) {
      logger.error(`API network error on retry ${method} ${path}`);
      throw err;
    }
  }

  if (!response.ok) {
    const traceId = response.headers.get('X-Trace-Id') ?? undefined;
    if (response.status >= 500) {
      logger.error(`API server error ${method} ${path}: ${response.status}${traceId ? ` traceId=${traceId}` : ''}`);
    } else {
      logger.warn(`API error ${method} ${path}: ${response.status}${traceId ? ` traceId=${traceId}` : ''}`);
    }
    const error = await response.json().catch(() => ({ title: 'Request failed' })) as { detail?: string; title?: string };
    throw new ApiError(error.detail || error.title || `HTTP ${response.status}`, response.status, traceId);
  }

  if (response.status === 204) return undefined as T;

  const contentType = response.headers.get('content-type');
  if (contentType?.includes('text/csv')) {
    return (await response.text()) as T;
  }

  return response.json() as Promise<T>;
}

export function parseJwtPayload(token: string): Record<string, unknown> {
  const base64 = token.split('.')[1] ?? '';
  return JSON.parse(atob(base64)) as Record<string, unknown>;
}

export function isGuestAdmin(auth: ProjectAuth): boolean {
  if (!auth.guestToken) return false;
  try {
    return Boolean(parseJwtPayload(auth.guestToken).admin);
  } catch {
    return false;
  }
}

export function getParticipantIdFromToken(slug: string): string | undefined {
  const auth = getAuth(slug);
  if (auth.guestToken) {
    try {
      const payload = parseJwtPayload(auth.guestToken);
      return payload.participantId as string | undefined;
    } catch {
      return undefined;
    }
  }
  return undefined;
}

export function setLastActiveSlug(slug: string): void {
  localStorage.setItem(STORAGE_KEYS.LAST_SLUG, slug);
}

export function getLastActiveSlug(): string | null {
  return localStorage.getItem(STORAGE_KEYS.LAST_SLUG);
}

const IDENTITY_KEY = STORAGE_KEYS.IDENTITY;

export function getIdentity(): string | null {
  return localStorage.getItem(IDENTITY_KEY);
}

export function setIdentity(name: string): void {
  localStorage.setItem(IDENTITY_KEY, name);
}

export function removeIdentity(): void {
  localStorage.removeItem(IDENTITY_KEY);
}

function updateDrafts(fn: (drafts: Record<string, string>) => void): void {
  try {
    const drafts = JSON.parse(localStorage.getItem(STORAGE_KEYS.DRAFT_COMMENTS) || '{}') as Record<string, string>;
    fn(drafts);
    localStorage.setItem(STORAGE_KEYS.DRAFT_COMMENTS, JSON.stringify(drafts));
  } catch {
    // ignore
  }
}

export function saveDraftComment(taskId: string, comment: string): void {
  updateDrafts((drafts) => {
    if (comment.trim()) {
      drafts[taskId] = comment;
    } else {
      delete drafts[taskId];
    }
  });
}

export function getDraftComment(taskId: string): string | null {
  try {
    const drafts = JSON.parse(localStorage.getItem(STORAGE_KEYS.DRAFT_COMMENTS) || '{}') as Record<string, string>;
    return drafts[taskId] ?? null;
  } catch {
    return null;
  }
}

export function clearDraftComment(taskId: string): void {
  updateDrafts((drafts) => {
    delete drafts[taskId];
  });
}

export function clearAllStorage(): void {
  localStorage.removeItem(JWT_KEY);
  localStorage.removeItem(IDENTITY_KEY);
  localStorage.removeItem(STORAGE_KEY);
  localStorage.removeItem(STORAGE_KEYS.LAST_SLUG);
  localStorage.removeItem(STORAGE_KEYS.BOARDS_LEGACY);
  localStorage.removeItem(STORAGE_KEYS.DRAFT_COMMENTS);
  sessionStorage.clear();
}

export async function updateNickname(slug: string, participantId: string, nickname: string): Promise<{ id: string; nickname: string }> {
  const result = await api<{ id: string; nickname: string }>(
    `/projects/${slug}/participants/${participantId}`,
    { method: 'PATCH', body: { nickname } },
    slug,
  );
  saveAuth(slug, { nickname });
  setIdentity(nickname);
  return result;
}
