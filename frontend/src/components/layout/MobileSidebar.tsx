import { useQuery } from '@tanstack/react-query';
import {
  ChevronDown,
  ChevronRight,
  Github,
  Home,
  LogIn,
  LogOut,
  Shield,
  UserPlus,
  X,
} from 'lucide-react';
import { useEffect, useLayoutEffect, useMemo, useState } from 'react';
import { Link, matchPath, useLocation } from 'react-router-dom';
import { getAllProjects, getIdentity, getJwt } from '@/api/client';
import { projectApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import { RoleBadge } from '@/components/RoleBadge';
import { useAuthConfig } from '@/hooks/useAuthConfig';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { roomTypeBadge, statusBadge, statusLabel } from '@/utils/roomBadges';

interface MobileSidebarProps {
  open: boolean;
  onClose: () => void;
  onLogout?: () => void;
}

const SIDEBAR_EXPANDED_KEY = 'efi-sidebar-expanded';

export function MobileSidebar({ open, onClose, onLogout }: MobileSidebarProps) {
  const location = useLocation();

  // Extract route params from pathname (useParams returns {} here because
  // Header/MobileSidebar render outside <Routes> in App.tsx)
  const roomMatch = matchPath('/p/:slug/r/:roomId/*', location.pathname);
  const projectMatch = matchPath('/p/:slug/*', location.pathname);
  const slug = roomMatch?.params.slug ?? projectMatch?.params.slug;
  const roomId = roomMatch?.params.roomId;
  const { isAdmin: isAppAdmin } = useCurrentUser();
  const { registrationEnabled } = useAuthConfig();
  const jwt = getJwt();

  // Initialize from sessionStorage (survives reload), fall back to URL slug
  const [expandedSlug, setExpandedSlug] = useState<string | null>(() => {
    return sessionStorage.getItem(SIDEBAR_EXPANDED_KEY) ?? slug ?? null;
  });

  // Persist to sessionStorage on change
  useEffect(() => {
    if (expandedSlug) {
      sessionStorage.setItem(SIDEBAR_EXPANDED_KEY, expandedSlug);
    } else {
      sessionStorage.removeItem(SIDEBAR_EXPANDED_KEY);
    }
  }, [expandedSlug]);

  // Sync expanded project when URL changes (useLayoutEffect to prevent flash)
  useLayoutEffect(() => {
    if (slug) setExpandedSlug(slug);
  }, [slug]);

  const projectEntries = useMemo(
    () =>
      Object.entries(getAllProjects())
        .map(([s, a]) => ({
          slug: s,
          name: a.projectName ?? s,
          isAdmin: Boolean(a.adminCode),
        }))
        .sort((a, b) => a.name.localeCompare(b.name)),
    [],
  );

  // Fetch rooms for expanded project
  const { data: rooms, isLoading: roomsLoading } = useQuery({
    queryKey: queryKeys.projects.rooms(expandedSlug!),
    queryFn: () => projectApi.rooms(expandedSlug!),
    enabled: Boolean(expandedSlug),
    staleTime: 10_000,
  });

  const sortedRooms = useMemo(
    () =>
      rooms?.slice().sort((a, b) => {
        // OPEN first, then by createdAt desc
        if (a.status === 'OPEN' && b.status !== 'OPEN') return -1;
        if (a.status !== 'OPEN' && b.status === 'OPEN') return 1;
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      }),
    [rooms],
  );

  // Close on route change
  useEffect(() => {
    onClose();
    // biome-ignore lint/correctness/useExhaustiveDependencies: close sidebar on navigation, onClose is stable
  }, [location.pathname]);

  // Close on Escape key
  useEffect(() => {
    if (!open) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  const linkClass = (active: boolean) =>
    `flex items-center gap-2.5 px-4 py-3 text-sm rounded-lg transition-colors ${
      active
        ? 'text-efi-text-primary bg-white/5'
        : 'text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/5'
    }`;

  const isHome = location.pathname === '/';

  return (
    <>
      {/* Overlay */}
      {open && (
        <div className="fixed inset-0 bg-black/50 z-[60]" onClick={onClose} aria-hidden="true" />
      )}

      {/* Sidebar panel */}
      <div
        className={`fixed inset-y-0 left-0 w-64 z-[70] glass-crystal border-r border-white/8 flex flex-col pt-safe pb-safe transform transition-transform duration-200 ease-in-out ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
        role="dialog"
        aria-modal="true"
        aria-label="Navigation menu"
      >
        {/* Sidebar header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-white/8">
          <span className="text-sm font-semibold text-efi-text-secondary uppercase tracking-wider">
            Menu
          </span>
          <button
            type="button"
            onClick={onClose}
            title="Close menu"
            className="p-2 rounded-lg text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/5 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Nav links */}
        <nav className="flex-1 overflow-y-auto px-2 py-3 space-y-0.5">
          <Link to="/" className={linkClass(isHome)}>
            <Home className="w-4 h-4 shrink-0" />
            Projects
          </Link>

          {isAppAdmin && (
            <Link to="/admin/users" className={linkClass(location.pathname.startsWith('/admin'))}>
              <Shield className="w-4 h-4 shrink-0" />
              Admin Panel
            </Link>
          )}

          {/* My Projects - on top with expandable rooms */}
          {projectEntries.length > 0 && (
            <>
              <div className="my-2 border-t border-white/8" />
              <p className="px-4 pb-1 text-[10px] font-bold uppercase text-efi-text-tertiary tracking-wider">
                My Projects
              </p>
              <div className="space-y-0.5">
                {projectEntries.map((entry) => {
                  const isCurrent = entry.slug === slug;
                  const isExpanded = entry.slug === expandedSlug;
                  return (
                    <div key={entry.slug}>
                      <div
                        className={`flex items-center gap-0 rounded-lg transition-colors ${
                          isExpanded || isCurrent ? 'bg-efi-gold/10' : ''
                        }`}
                      >
                        <button
                          type="button"
                          onClick={() =>
                            setExpandedSlug((prev) => (prev === entry.slug ? null : entry.slug))
                          }
                          title={isExpanded ? 'Collapse rooms' : 'Expand rooms'}
                          className="p-2.5 rounded-lg text-efi-text-tertiary hover:text-efi-text-primary hover:bg-white/5 transition-colors cursor-pointer shrink-0 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                        >
                          {isExpanded ? (
                            <ChevronDown className="w-3.5 h-3.5" />
                          ) : (
                            <ChevronRight className="w-3.5 h-3.5" />
                          )}
                        </button>
                        <Link
                          to={`/p/${entry.slug}`}
                          className={`flex-1 truncate py-2.5 text-sm no-underline rounded-lg transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none ${
                            isExpanded || isCurrent
                              ? 'text-efi-text-primary'
                              : 'text-efi-text-secondary hover:text-efi-text-primary'
                          }`}
                        >
                          {entry.name}
                        </Link>
                        <span className="shrink-0 mr-3">
                          <RoleBadge isAdmin={entry.isAdmin} />
                        </span>
                      </div>

                      {/* Expanded rooms */}
                      {isExpanded && (
                        <div className="ml-4 mt-0.5 space-y-0.5">
                          {roomsLoading && (
                            <p className="px-4 py-2 text-xs text-efi-text-tertiary">
                              Loading rooms...
                            </p>
                          )}
                          {!roomsLoading && sortedRooms && sortedRooms.length === 0 && (
                            <p className="px-4 py-2 text-xs text-efi-text-tertiary">No rooms</p>
                          )}
                          {sortedRooms?.map((room) => {
                            const isCurrentRoom = room.id === roomId;
                            return (
                              <Link
                                key={room.id}
                                to={`/p/${entry.slug}/r/${room.id}`}
                                className={`flex items-center gap-2 px-4 py-2 text-sm rounded-lg transition-colors no-underline ${
                                  isCurrentRoom
                                    ? 'text-efi-text-primary bg-efi-gold/10'
                                    : 'text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/5'
                                }`}
                              >
                                <span className="truncate flex-1">{room.title}</span>
                                <div className="flex items-center gap-1 shrink-0">
                                  <span
                                    className={`text-[10px] font-bold uppercase px-1 py-0.5 rounded border ${roomTypeBadge(room.roomType)}`}
                                  >
                                    {room.roomType === 'LIVE' ? 'Live' : 'Async'}
                                  </span>
                                  <span
                                    className={`text-[10px] font-bold uppercase px-1 py-0.5 rounded border ${statusBadge(room.status)}`}
                                  >
                                    {statusLabel(room.status)}
                                  </span>
                                </div>
                              </Link>
                            );
                          })}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </>
          )}

          {!jwt && !getIdentity() && (
            <>
              <div className="my-2 border-t border-white/8" />
              <Link to="/login" className={linkClass(location.pathname === '/login')}>
                <LogIn className="w-4 h-4 shrink-0" />
                Login
              </Link>
              {registrationEnabled && (
                <Link to="/register" className={linkClass(location.pathname === '/register')}>
                  <UserPlus className="w-4 h-4 shrink-0" />
                  Register
                </Link>
              )}
            </>
          )}
        </nav>

        {/* Footer */}
        <div className="px-2 py-3 border-t border-white/8">
          {onLogout && (
            <button
              type="button"
              onClick={onLogout}
              className="flex items-center gap-2.5 w-full px-4 py-3 text-sm text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/5 rounded-lg transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
            >
              <LogOut className="w-4 h-4 shrink-0" />
              <span>Logout</span>
            </button>
          )}
          <a
            href="https://github.com/slawomir-andreasik/efi-poker"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-2.5 px-4 py-3 text-sm text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/5 rounded-lg transition-colors"
          >
            <Github className="w-4 h-4 shrink-0" />
            <span>GitHub</span>
          </a>
        </div>
      </div>
    </>
  );
}
