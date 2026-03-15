import { useState, useEffect, useMemo } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useQuery, useQueries } from '@tanstack/react-query';
import { api, ApiError, saveAuth, getAllProjects, removeProject, getIdentity, setIdentity, getJwt } from '@/api/client';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { queryKeys } from '@/api/queryKeys';
import { projectApi, authApi } from '@/api/queries';
import { useCreateProject } from '@/api/mutations';
import type { ProjectAuth } from '@/api/client';
import { useToast } from '@/components/Toast';
import { getErrorMessage } from '@/utils/error';
import { Info, ChevronRight, Plus, Target, Clock, BarChart3 } from 'lucide-react';
import { Spinner } from '@/components/Spinner';
import { RoleBadge } from '@/components/RoleBadge';
import { RandomNameButton } from '@/components/RandomNameButton';
import { JoinByCodeModal } from '@/components/JoinByCodeModal';
import { CreateProjectModal } from '@/components/CreateProjectModal';
import { TextInput } from '@/components/TextInput';
import type { RoomResponse, ParticipantResponse } from '@/api/types';

interface ProjectEntry {
  slug: string;
  auth: ProjectAuth;
}

export function HomePage() {
  useDocumentTitle();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [identity, setIdentityState] = useState(() => getIdentity());
  const [nameInput, setNameInput] = useState('');

  const [jwt] = useState(() => getJwt());

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showJoinModal, setShowJoinModal] = useState(false);

  // Known projects from localStorage
  const [projectEntries, setProjectEntries] = useState<ProjectEntry[]>(() =>
    Object.entries(getAllProjects()).map(([slug, auth]) => ({ slug, auth })),
  );

  const isGuest = !jwt;

  // Hydrate owned projects from server for logged-in users
  const { data: myServerProjects } = useQuery({
    queryKey: queryKeys.auth.myProjects,
    queryFn: authApi.myProjects,
    enabled: !isGuest,
  });

  // Hydrate participated projects from server for logged-in users
  const { data: participatedProjects } = useQuery({
    queryKey: queryKeys.auth.participatedProjects,
    queryFn: authApi.participatedProjects,
    enabled: !isGuest,
  });

  // Sync owned projects from server to localStorage
  useEffect(() => {
    if (!myServerProjects?.length) return;
    let changed = false;
    const current = getAllProjects();
    for (const p of myServerProjects) {
      if (!current[p.slug]?.adminCode && p.adminCode) {
        saveAuth(p.slug, { projectName: p.name, adminCode: p.adminCode });
        changed = true;
      }
    }
    if (changed) {
      setProjectEntries(Object.entries(getAllProjects()).map(([slug, auth]) => ({ slug, auth })));
    }
  }, [myServerProjects]);

  // Sync participated projects from server to localStorage
  useEffect(() => {
    if (!participatedProjects?.length) return;
    let changed = false;
    const current = getAllProjects();
    for (const p of participatedProjects) {
      if (!current[p.slug]) {
        saveAuth(p.slug, { projectName: p.name });
        changed = true;
      }
    }
    if (changed) {
      setProjectEntries(Object.entries(getAllProjects()).map(([slug, auth]) => ({ slug, auth })));
    }
  }, [participatedProjects]);

  // Fetch rooms for all known projects in parallel
  const roomQueries = useQueries({
    queries: identity
      ? projectEntries.map((entry) => ({
          queryKey: queryKeys.projects.rooms(entry.slug),
          queryFn: () => projectApi.rooms(entry.slug),
          retry: false,
        }))
      : [],
  });

  // Clean up 404'd projects from localStorage
  useEffect(() => {
    if (!identity) return;
    const removedSlugs = new Set<string>();
    roomQueries.forEach((q, i) => {
      const slug = projectEntries[i]?.slug;
      if (q.error instanceof ApiError && q.error.status === 404 && slug) {
        removedSlugs.add(slug);
        removeProject(slug);
      }
    });
    if (removedSlugs.size > 0) {
      setProjectEntries((prev) => prev.filter((p) => !removedSlugs.has(p.slug)));
    }
  }, [roomQueries.map((q) => q.status).join(',')]); // eslint-disable-line react-hooks/exhaustive-deps

  // Build projects with rooms data
  const projectsWithRooms = useMemo(() =>
    projectEntries.map((entry, i) => {
      const query = roomQueries[i];
      return {
        slug: entry.slug,
        auth: entry.auth,
        rooms: (query?.data ?? []) as RoomResponse[],
        loading: query?.isLoading ?? true,
        error: query?.isError ?? false,
      };
    }),
  [projectEntries, roomQueries],
  );

  // Mutations
  const createProject = useCreateProject();

  function handleSetName(e: React.FormEvent) {
    e.preventDefault();
    if (!nameInput.trim()) return;
    setIdentity(nameInput.trim());
    setIdentityState(nameInput.trim());
  }

  async function handleCreate(name: string) {
    if (!identity) return;

    try {
      const project = await createProject.mutateAsync({ name });

      saveAuth(project.slug, { adminCode: project.adminCode, projectName: project.name });

      try {
        const participant = await api<ParticipantResponse>(
          `/projects/${project.slug}/participants`,
          { method: 'POST', body: { nickname: identity } },
          project.slug,
        );
        saveAuth(project.slug, { participantId: participant.id, nickname: participant.nickname });
      } catch {
        // Auto-join failed, admin can join manually from the project page
      }

      setShowCreateModal(false);
      navigate(`/p/${project.slug}`);
    } catch (err) {
      showToast(getErrorMessage(err));
    }
  }

  // Sort: projects with OPEN rooms first, then by room count desc
  const sortedProjects = useMemo(
    () =>
      [...projectsWithRooms].sort((a, b) => {
        const aHasOpen = a.rooms.some((r) => r.status === 'OPEN');
        const bHasOpen = b.rooms.some((r) => r.status === 'OPEN');
        if (aHasOpen !== bHasOpen) return aHasOpen ? -1 : 1;
        return b.rooms.length - a.rooms.length;
      }),
    [projectsWithRooms],
  );


  if (!identity) {
    const features = [
      { icon: Target, text: 'Live voting - your team estimates together in real time' },
      { icon: Clock, text: 'Async mode - everyone votes on their own schedule' },
      { icon: BarChart3, text: 'Smart results - see consensus, outliers, and discussion points instantly' },
    ];

    return (
      <div className="flex flex-col items-center justify-center min-h-[80vh] px-4">
        <div className="text-center mb-8 animate-[fade-in-up_0.6s_ease-out]">
          <p className="text-xs font-bold text-efi-gold-light/40 uppercase tracking-[0.2em] mb-4">
            Estimate. Focus. Improve.
          </p>
        </div>

        <p className="text-efi-text-secondary text-base max-w-md mx-auto text-center mb-8 animate-[fade-in-up_0.6s_ease-out_0.15s_both]">
          Sprint planning poker for distributed teams.
        </p>

        <form onSubmit={(e) => handleSetName(e)} className="w-full max-w-sm animate-[fade-in-up_0.6s_ease-out_0.3s_both]">
          <div className="glass-frost rounded-2xl p-4 sm:p-6">
            <label htmlFor="name-input" className="block text-sm font-medium text-efi-text-secondary mb-2">
              What&apos;s your name?
            </label>
            <div className="flex items-center gap-1">
              <TextInput
                id="name-input"
                type="text"
                value={nameInput}
                onChange={(e) => setNameInput(e.target.value)}
                placeholder="e.g. Alice"
                maxLength={100}
                autoFocus
                className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
              />
              <RandomNameButton onGenerate={setNameInput} />
            </div>
            <button
              type="submit"
              disabled={!nameInput.trim()}
              className="w-full mt-4 py-3 rounded-lg font-medium text-sm bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
            >
              Continue
            </button>
          </div>
        </form>

        <div className="flex flex-col gap-2 mt-6 w-full max-w-sm">
          {features.map((feature, i) => (
            <div
              key={feature.text}
              className="flex items-center gap-2.5 px-3 py-2.5 rounded-lg bg-white/3 border border-white/5"
              style={{ animation: `fade-in-up 0.6s ease-out ${0.5 + i * 0.12}s both` }}
            >
              <feature.icon className="w-4 h-4 text-efi-gold/70 shrink-0" />
              <span className="text-xs text-efi-text-tertiary">{feature.text}</span>
            </div>
          ))}
        </div>

      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8">
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-sm font-semibold text-efi-text-secondary uppercase tracking-wider">
          My Projects
        </h2>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setShowJoinModal(true)}
            className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            Join
          </button>
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 transition-opacity cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            <Plus className="w-4 h-4 inline -mt-0.5" /> New Project
          </button>
        </div>
      </div>

      {isGuest && identity && (
        <div className="flex items-center gap-2 mb-6 px-3 py-2 rounded-lg border border-efi-info/20 bg-efi-info/5 text-xs text-efi-text-secondary">
          <Info className="w-3.5 h-3.5 text-efi-info shrink-0" />
          <span>
            You're using EFI Poker as a guest. Your projects are stored locally.{' '}
            <Link to="/login" className="text-efi-info hover:text-efi-info/80 font-medium no-underline hover:underline">
              Log in
            </Link>{' '}
            to keep your data safe across devices.
          </span>
        </div>
      )}

      <JoinByCodeModal
        isOpen={showJoinModal}
        onClose={() => setShowJoinModal(false)}
      />

      <CreateProjectModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onCreate={handleCreate}
        isPending={createProject.isPending}
        isGuest={isGuest}
      />

      {/* Empty state for first-time users */}
      {sortedProjects.length === 0 && (
        <div className="mb-8 text-center py-12 bg-white/3 rounded-2xl border border-dashed border-white/8">
          <span className="text-4xl block mb-3">&#127183;</span>
          <p className="text-efi-text-primary font-medium mb-1">Create your first project to get started</p>
          <p className="text-xs text-efi-text-tertiary mb-4">or ask your team lead for a join link</p>
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="px-6 py-3 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 transition-opacity cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            <Plus className="w-4 h-4 inline -mt-0.5" /> New Project
          </button>
        </div>
      )}

      {/* My Projects - project cards with room count */}
      {sortedProjects.length > 0 && (
        <div className="mb-8">
          <div className="space-y-2">
            {sortedProjects.map((project) => (
              <Link
                key={project.slug}
                to={`/p/${project.slug}`}
                className="block glass-frost rounded-xl px-4 py-3 hover:border-efi-gold/30 active:scale-[0.995] transition-all duration-150 no-underline focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 min-w-0">
                    <span className="text-sm font-medium text-efi-text-primary truncate">
                      {project.auth.projectName ?? project.slug}
                    </span>
                    <RoleBadge isAdmin={Boolean(project.auth.adminCode)} />
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {project.loading ? (
                      <Spinner className="h-4 w-4" />
                    ) : project.error ? (
                      <span className="text-xs text-efi-text-tertiary">Unavailable</span>
                    ) : (
                      <span className="text-xs text-efi-text-tertiary">
                        {project.rooms.length} {project.rooms.length === 1 ? 'room' : 'rooms'}
                      </span>
                    )}
                    <ChevronRight className="w-4 h-4 text-efi-text-tertiary" />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

    </div>
  );
}
