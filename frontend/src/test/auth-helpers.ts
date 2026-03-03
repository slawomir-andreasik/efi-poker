export function setAdminAuth(slug: string, adminCode: string): void {
  const projects = JSON.parse(localStorage.getItem('efi-projects') || '{}');
  projects[slug] = { ...projects[slug], adminCode };
  localStorage.setItem('efi-projects', JSON.stringify(projects));
}

export function setParticipantAuth(slug: string, participantId: string): void {
  const projects = JSON.parse(localStorage.getItem('efi-projects') || '{}');
  projects[slug] = { ...projects[slug], participantId };
  localStorage.setItem('efi-projects', JSON.stringify(projects));
}

export function setIdentity(name: string): void {
  localStorage.setItem('efi-identity', name);
}
