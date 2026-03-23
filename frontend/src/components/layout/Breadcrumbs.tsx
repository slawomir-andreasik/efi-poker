import { ChevronDown, ChevronRight, ChevronUp } from 'lucide-react';
import { useCallback, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useBreadcrumbs } from '@/hooks/useBreadcrumbs';
import { roomTypeBadge, statusBadge, statusLabel } from '@/utils/roomBadges';
import { ProjectSwitcherDropdown } from './ProjectSwitcherDropdown';
import { RoomSwitcherDropdown } from './RoomSwitcherDropdown';

export function Breadcrumbs() {
  const { segments, slug, roomId, currentRoom } = useBreadcrumbs();
  const location = useLocation();
  const [roomSwitcherOpen, setRoomSwitcherOpen] = useState(false);
  const [projectSwitcherOpen, setProjectSwitcherOpen] = useState(false);

  const closeRoomSwitcher = useCallback(() => setRoomSwitcherOpen(false), []);
  const closeProjectSwitcher = useCallback(() => setProjectSwitcherOpen(false), []);

  const isHome = location.pathname === '/';

  function toggleProjectSwitcher() {
    setProjectSwitcherOpen((prev) => !prev);
    setRoomSwitcherOpen(false);
  }

  function toggleRoomSwitcher() {
    setRoomSwitcherOpen((prev) => !prev);
    setProjectSwitcherOpen(false);
  }

  return (
    <nav
      aria-label="Breadcrumb"
      className="hidden sm:block border-b border-white/4 bg-efi-graphite sm:sticky top-[var(--header-height)] z-40"
    >
      <div className="max-w-6xl mx-auto px-3 sm:px-4 py-1.5 flex items-center gap-1.5 text-sm min-w-0">
        {isHome ? (
          <span className="text-efi-text-secondary text-sm font-medium shrink-0">Projects</span>
        ) : (
          <Link
            to="/"
            className="text-efi-text-tertiary hover:text-efi-text-secondary transition-colors no-underline shrink-0"
          >
            Projects
          </Link>
        )}

        {segments.map((seg, i) => {
          const isLast = i === segments.length - 1;
          // On mobile with 3+ segments, hide middle segments
          const isMiddle = segments.length > 2 && i > 0 && !isLast;

          return (
            <span
              key={seg.path || seg.label}
              className={`flex items-center gap-1.5 min-w-0 ${isMiddle ? 'hidden sm:flex' : ''}`}
            >
              <ChevronRight className="w-3.5 h-3.5 text-efi-text-tertiary/40 shrink-0" />

              {seg.dropdownType === 'project' ? (
                // Project switcher dropdown
                <div className="relative min-w-0">
                  <button
                    type="button"
                    onClick={toggleProjectSwitcher}
                    className="flex items-center gap-1 px-1.5 py-0.5 rounded-md text-sm font-medium text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/8 transition-colors cursor-pointer max-w-36 sm:max-w-48 truncate"
                  >
                    <span className="truncate">{seg.label}</span>
                    {projectSwitcherOpen ? (
                      <ChevronUp className="w-3 h-3 shrink-0 text-efi-text-tertiary" />
                    ) : (
                      <ChevronDown className="w-3 h-3 shrink-0 text-efi-text-tertiary" />
                    )}
                  </button>
                  {projectSwitcherOpen && (
                    <ProjectSwitcherDropdown currentSlug={slug} onClose={closeProjectSwitcher} />
                  )}
                </div>
              ) : seg.dropdownType === 'room' && slug ? (
                // Room switcher dropdown
                <div className="relative min-w-0">
                  <button
                    type="button"
                    onClick={toggleRoomSwitcher}
                    className="flex items-center gap-1 px-1.5 py-0.5 rounded-md text-sm font-medium text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/8 transition-colors cursor-pointer max-w-36 sm:max-w-48 truncate"
                  >
                    <span className="truncate">{seg.label}</span>
                    {roomSwitcherOpen ? (
                      <ChevronUp className="w-3 h-3 shrink-0 text-efi-text-tertiary" />
                    ) : (
                      <ChevronDown className="w-3 h-3 shrink-0 text-efi-text-tertiary" />
                    )}
                  </button>
                  {roomSwitcherOpen && (
                    <RoomSwitcherDropdown
                      slug={slug}
                      projectName={segments[0]?.label ?? slug}
                      currentRoomId={roomId}
                      onClose={closeRoomSwitcher}
                    />
                  )}
                </div>
              ) : seg.path ? (
                // Clickable parent segment
                <Link
                  to={seg.path}
                  className="text-sm font-medium text-efi-text-secondary hover:text-efi-text-primary transition-colors no-underline truncate max-w-36 sm:max-w-48"
                >
                  {seg.label}
                </Link>
              ) : (
                // Current segment (not clickable)
                <span className="text-sm font-medium text-efi-text-primary truncate max-w-36 sm:max-w-48">
                  {seg.label}
                </span>
              )}

              {/* Room badges - desktop only */}
              {seg.badges && currentRoom && (
                <div className="hidden sm:flex items-center gap-1 shrink-0">
                  <span
                    className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${statusBadge(currentRoom.status)}`}
                  >
                    {statusLabel(currentRoom.status)}
                  </span>
                  <span
                    className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${roomTypeBadge(currentRoom.roomType)}`}
                  >
                    {currentRoom.roomType === 'LIVE' ? 'Live' : 'Async'}
                  </span>
                </div>
              )}
            </span>
          );
        })}

        {/* Mobile ellipsis for collapsed middle segments */}
        {segments.length > 2 && (
          <span className="flex items-center gap-1.5 sm:hidden">
            <ChevronRight className="w-3.5 h-3.5 text-efi-text-tertiary/40 shrink-0" />
            <span className="text-sm text-efi-text-tertiary">...</span>
          </span>
        )}
      </div>
    </nav>
  );
}
