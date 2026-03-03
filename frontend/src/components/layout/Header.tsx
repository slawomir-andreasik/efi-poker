import { useEffect, useLayoutEffect, useState } from 'react';
import { Link, useLocation, useNavigate, matchPath } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { getAuth, setLastActiveSlug, getLastActiveSlug, getIdentity, getJwt, clearAllStorage } from '@/api/client';
import { queryKeys } from '@/api/queryKeys';
import { projectApi } from '@/api/queries';
import { useAuthConfig } from '@/hooks/useAuthConfig';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { Copy, Menu } from 'lucide-react';
import { useToast } from '@/components/Toast';
import { NicknameDropdown } from './NicknameDropdown';
import { MobileSidebar } from './MobileSidebar';

export function Header() {
  const location = useLocation();

  // Extract route params from pathname (useParams returns {} here because
  // Header renders outside <Routes> in App.tsx)
  const roomMatch = matchPath('/p/:slug/r/:roomId/*', location.pathname);
  const projectMatch = matchPath('/p/:slug/*', location.pathname);
  const slug = roomMatch?.params.slug ?? projectMatch?.params.slug;
  const roomId = roomMatch?.params.roomId;
  const navigate = useNavigate();
  const jwt = getJwt(); // synchronous read, fresh on every render (re-renders on useLocation)
  const { auth0Enabled, registrationEnabled } = useAuthConfig();
  const { user: currentUser, isAdmin: isAppAdmin } = useCurrentUser();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const queryClient = useQueryClient();
  const { showToast } = useToast();

  useEffect(() => {
    if (slug) setLastActiveSlug(slug);
  }, [slug]);

  // Close sidebar on route change (useLayoutEffect to prevent flash)
  useLayoutEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  const effectiveSlug = slug ?? getLastActiveSlug() ?? undefined;
  const auth = effectiveSlug ? getAuth(effectiveSlug) : {};
  const [displayName, setDisplayName] = useState(auth.nickname || getIdentity() || currentUser?.username || null);
  const isAdmin = Boolean(auth.adminCode);
  const participantId = auth.participantId;

  // Sync displayName when slug/user changes
  useEffect(() => {
    const currentAuth = effectiveSlug ? getAuth(effectiveSlug) : {};
    setDisplayName(currentAuth.nickname || getIdentity() || currentUser?.username || null);
  }, [effectiveSlug, location.pathname, currentUser?.username]);

  // Fetch rooms for share button (TanStack cache deduplicates with Breadcrumbs)
  const { data: rooms } = useQuery({
    queryKey: queryKeys.projects.rooms(slug!),
    queryFn: () => projectApi.rooms(slug!),
    enabled: Boolean(slug),
    staleTime: 10_000,
  });
  const currentRoom = rooms?.find((r) => r.id === roomId);

  function handleLogout() {
    clearAllStorage();
    queryClient.clear();
    if (auth0Enabled) {
      window.location.href = '/api/v1/auth/logout';
    } else {
      navigate('/');
    }
  }

  function handleGuestLogout() {
    clearAllStorage();
    queryClient.clear();
    setDisplayName(null);
    navigate('/');
  }

  async function handleCopyRoomLink() {
    if (!currentRoom?.slug) return;
    const link = `${window.location.origin}/r/${currentRoom.slug}`;
    try {
      await navigator.clipboard.writeText(link);
      showToast('Room link copied!');
    } catch {
      showToast('Failed to copy link');
    }
  }

  return (
    <>
      <MobileSidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} onLogout={jwt ? handleLogout : undefined} />
      <header className="border-b border-white/6 glass-whisper sticky top-0 z-50">
        <div className="max-w-6xl mx-auto px-3 sm:px-4 py-2 sm:py-3 flex items-center justify-between gap-2">
          {/* Left: hamburger + logo */}
          <div className="flex items-center gap-2 min-w-0">
            <button
              type="button"
              onClick={() => setSidebarOpen(true)}
              title="Open menu"
              className="p-1.5 rounded-lg text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/8 transition-colors cursor-pointer shrink-0 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
            >
              <Menu className="w-5 h-5" />
            </button>

            <Link to="/" className="flex items-center gap-1.5 no-underline group shrink-0">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" className="w-5 h-5 shrink-0" aria-hidden="true">
                <defs>
                  <linearGradient id="hdr-bg" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#141418"/>
                    <stop offset="100%" stopColor="#0B0B0F"/>
                  </linearGradient>
                  <linearGradient id="hdr-e" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#C8D0DC"/>
                    <stop offset="100%" stopColor="#8A92A0"/>
                  </linearGradient>
                </defs>
                <rect width="64" height="64" rx="14" fill="url(#hdr-bg)"/>
                <path d="M17 12 H47 V20 H26 V28 H43 V36 H26 V44 H47 V52 H17 Z" fill="url(#hdr-e)"/>
              </svg>
              <span className="text-sm sm:text-xl font-bold bg-gradient-to-r from-efi-gold via-efi-gold-light to-efi-gold bg-clip-text text-transparent">
                EFI Poker
              </span>
            </Link>
          </div>

          {/* Right: share + role badge + user + auth */}
          <div className="flex items-center gap-3 sm:gap-4 shrink-0">
            {/* Share room link - only on room/results pages */}
            {currentRoom?.slug && (
              <button
                type="button"
                onClick={() => void handleCopyRoomLink()}
                title="Copy room join link"
                className="p-2 rounded-lg text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/8 transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
              >
                <Copy className="w-4 h-4" />
              </button>
            )}

            {/* Role badge - only inside a project */}
            {slug && (
              <span className={`inline-flex text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${
                isAdmin
                  ? 'bg-efi-gold/20 text-efi-gold-light border-efi-gold/30'
                  : 'bg-white/8 text-efi-text-secondary border-white/10'
              }`}>
                {isAdmin ? 'Admin' : 'Voter'}
              </span>
            )}

            {isAppAdmin && (
              <Link
                to="/admin/users"
                className="text-xs px-2 py-1 rounded-lg text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/8 transition-colors no-underline"
              >
                Admin
              </Link>
            )}

            {displayName && (
              <NicknameDropdown
                displayName={displayName}
                slug={effectiveSlug}
                participantId={participantId}
                onNicknameChanged={setDisplayName}
                onLogout={jwt ? handleLogout : handleGuestLogout}
              />
            )}

            {!displayName && !jwt && location.pathname !== '/login' && location.pathname !== '/register' && (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="text-xs px-2.5 py-1 rounded-lg font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 transition-opacity no-underline"
                >
                  Login
                </Link>
                {registrationEnabled && (
                  <Link
                    to="/register"
                    className="text-xs px-2.5 py-1 rounded-lg text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/8 transition-colors border border-white/10 no-underline"
                  >
                    Register
                  </Link>
                )}
              </div>
            )}
          </div>
        </div>
      </header>
    </>
  );
}
