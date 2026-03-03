import type { RoomType } from '@/api/types';

const STATUS_STYLES: Record<string, string> = {
  OPEN: 'bg-efi-success/20 text-efi-success border-efi-success/30',
  REVEALED: 'bg-efi-warning/20 text-efi-warning border-efi-warning/30',
  CLOSED: 'bg-efi-ash/20 text-efi-text-secondary border-efi-ash/30',
};

export function statusBadge(status: string): string {
  return STATUS_STYLES[status] || 'bg-efi-success/20 text-efi-success border-efi-success/30';
}

export function roomTypeBadge(roomType: RoomType): string {
  if (roomType === 'LIVE') {
    return 'bg-efi-live/20 text-efi-live border-efi-live/30';
  }
  return 'bg-efi-info/20 text-efi-info border-efi-info/30';
}

export function statusLabel(status: string): string {
  const labels: Record<string, string> = {
    OPEN: 'Voting open',
    REVEALED: 'Votes revealed',
    CLOSED: 'Completed',
  };
  return labels[status] ?? status;
}
