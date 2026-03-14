export async function copyRoomLink(
  roomSlug: string,
  showToast: (message: string) => void,
): Promise<void> {
  const link = `${window.location.origin}/r/${roomSlug}`;
  try {
    await navigator.clipboard.writeText(link);
    showToast('Room link copied!');
  } catch {
    showToast('Failed to copy link');
  }
}
