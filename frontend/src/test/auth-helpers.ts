export function setAdminAuth(slug: string, adminCode: string): void {
  const projects = JSON.parse(localStorage.getItem('efi-projects') || '{}');
  projects[slug] = { ...projects[slug], adminCode };
  localStorage.setItem('efi-projects', JSON.stringify(projects));
}

export function setGuestTokenAuth(slug: string, guestToken: string): void {
  const projects = JSON.parse(localStorage.getItem('efi-projects') || '{}');
  projects[slug] = { ...projects[slug], guestToken };
  localStorage.setItem('efi-projects', JSON.stringify(projects));
}

/** Create a fake JWT with given payload (header.payload.signature format, not cryptographically valid) */
export function fakeJwt(payload: Record<string, unknown> = {}): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify({ participantId: 'p-1', ...payload }));
  return `${header}.${body}.fake-signature`;
}

export function setIdentity(name: string): void {
  localStorage.setItem('efi-identity', name);
}
