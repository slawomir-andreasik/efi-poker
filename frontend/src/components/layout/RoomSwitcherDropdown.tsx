import { useRef, useMemo } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '@/api/queryKeys';
import { projectApi } from '@/api/queries';
import { getAuth } from '@/api/client';
import { statusBadge, roomTypeBadge, statusLabel } from '@/utils/roomBadges';
import { CountdownTimer } from '@/components/CountdownTimer';
import { Spinner } from '@/components/Spinner';
import { useDropdownDismiss } from '@/hooks/useDropdownDismiss';

interface RoomSwitcherDropdownProps {
  slug: string;
  projectName: string;
  currentRoomId?: string;
  onClose: () => void;
}

export function RoomSwitcherDropdown({ slug, projectName, currentRoomId, onClose }: RoomSwitcherDropdownProps) {
  const navigate = useNavigate();
  const dropdownRef = useRef<HTMLDivElement>(null);
  const isAdmin = Boolean(getAuth(slug).adminCode);

  const { data: rooms, isLoading } = useQuery({
    queryKey: queryKeys.projects.rooms(slug),
    queryFn: () => projectApi.rooms(slug),
    staleTime: 10_000,
  });

  useDropdownDismiss(dropdownRef, true, onClose);

  // Sort: OPEN first, then by createdAt desc
  const sortedRooms = useMemo(() => [...(rooms ?? [])].sort((a, b) => {
    if (a.status === 'OPEN' && b.status !== 'OPEN') return -1;
    if (a.status !== 'OPEN' && b.status === 'OPEN') return 1;
    return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
  }), [rooms]);

  return (
    <div
      ref={dropdownRef}
      className="absolute left-0 top-full mt-1 glass-crystal rounded-lg shadow-xl w-[85vw] sm:w-auto sm:min-w-64 max-w-sm z-50 overflow-hidden"
    >
      <div className="px-3 py-2 border-b border-white/8">
        <p className="text-xs font-semibold text-efi-text-secondary uppercase tracking-wider">{projectName}</p>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-4">
          <Spinner className="h-5 w-5" />
        </div>
      ) : sortedRooms.length > 0 ? (
        <div className="max-h-80 overflow-y-auto py-1">
          {sortedRooms.map((room) => {
            const isCurrent = room.id === currentRoomId;
            return (
              <button
                key={room.id}
                onClick={() => {
                  navigate(`/p/${slug}/r/${room.id}`);
                  onClose();
                }}
                className={`w-full text-left px-3 py-3 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none ${
                  isCurrent
                    ? 'bg-efi-gold/15 border-l-2 border-efi-gold'
                    : 'border-l-2 border-transparent hover:bg-white/5'
                }`}
              >
                <div className="flex items-center gap-2">
                  <span className={`text-sm font-medium truncate ${isCurrent ? 'text-efi-gold-light' : 'text-efi-text-primary'}`}>
                    {room.title}
                  </span>
                  <span className={`shrink-0 text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${roomTypeBadge(room.roomType)}`}>
                    {room.roomType === 'LIVE' ? 'Live' : 'Async'}
                  </span>
                  <span className={`shrink-0 text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${statusBadge(room.status)}`}>
                    {statusLabel(room.status)}
                  </span>
                </div>
                <div className="mt-0.5">
                  {room.roomType === 'LIVE' && room.status === 'OPEN' && (
                    <span className="text-xs text-efi-text-tertiary flex items-center gap-1.5">
                      <span className="relative flex h-1.5 w-1.5">
                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-efi-live opacity-75" />
                        <span className="relative inline-flex rounded-full h-1.5 w-1.5 bg-efi-live" />
                      </span>
                      {room.topic ? `Round ${room.roundNumber}: ${room.topic}` : `Round ${room.roundNumber}`}
                    </span>
                  )}
                  {room.roomType === 'ASYNC' && room.deadline && room.status === 'OPEN' && (
                    <span className="text-xs text-efi-text-tertiary">
                      <CountdownTimer deadline={room.deadline} />
                    </span>
                  )}
                </div>
              </button>
            );
          })}
        </div>
      ) : (
        <div className="px-3 py-4 text-center">
          <p className="text-xs text-efi-text-tertiary">No rooms yet</p>
        </div>
      )}

      <div className="border-t border-white/8 py-1">
        <Link
          to={`/p/${slug}`}
          onClick={onClose}
          className="block px-3 py-2.5 text-xs text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/5 transition-colors no-underline"
        >
          {isAdmin ? 'Manage rooms' : 'View all rooms'}
        </Link>
      </div>
    </div>
  );
}
