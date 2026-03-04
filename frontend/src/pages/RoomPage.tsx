import { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Copy } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { getAuth, getJwt, saveAuth, ApiError } from '@/api/client';
import { queryKeys } from '@/api/queryKeys';
import { roomApi, projectApi } from '@/api/queries';
import { useSubmitEstimate, useDeleteEstimate, useSetFinalEstimate, useUpdateTask, useAdminJoinMutation } from '@/api/mutations';
import { logger } from '@/utils/logger';
import { getErrorMessage } from '@/utils/error';
import { useSortedTasks } from '@/hooks/useSortedTasks';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { useToast } from '@/components/Toast';
import { Spinner, ButtonSpinner } from '@/components/Spinner';
import { CountdownTimer } from '@/components/CountdownTimer';
import { TaskCard } from '@/components/TaskCard';
import { SortControls } from '@/components/SortControls';
import { RoomSidebar } from '@/components/RoomSidebar';
import { LiveRoomView } from '@/components/LiveRoomView';
import { TextInput } from '@/components/TextInput';
import type { StoryPoints } from '@/api/types';

export function RoomPage() {
  const { slug, roomId } = useParams<{ slug: string; roomId: string }>();
  const [auth, setAuth] = useState(() => (slug ? getAuth(slug) : {}));
  const isAdmin = Boolean(auth.adminCode);
  const hasParticipant = Boolean(auth.participantId);
  const { showToast } = useToast();
  const projectName = auth.projectName ?? slug;

  const [votes, setVotes] = useState<Record<string, StoryPoints>>({});

  const { data: room, isLoading: loading, error } = useQuery({
    queryKey: queryKeys.rooms.detail(roomId!),
    queryFn: () => roomApi.detail(roomId!, slug!),
    enabled: Boolean(roomId && slug),
    refetchInterval: (query) => (query.state.status === 'error' ? false : 5_000),
  });

  const jwt = getJwt();

  // Fallback: logged-in owner without adminCode in localStorage
  const { data: adminProject } = useQuery({
    queryKey: queryKeys.projects.admin(slug!),
    queryFn: () => projectApi.admin(slug!),
    enabled: Boolean(slug && jwt && !auth.adminCode),
    retry: false,
  });

  useEffect(() => {
    if (adminProject?.adminCode && slug) {
      saveAuth(slug, { adminCode: adminProject.adminCode, projectName: adminProject.name });
      setAuth(getAuth(slug));
    }
  }, [adminProject, slug]);

  useDocumentTitle(room?.title, projectName);

  // Admin join
  const [joinNickname, setJoinNickname] = useState('');
  const adminJoin = useAdminJoinMutation(slug ?? '');

  async function handleCopyRoomLink() {
    if (!room?.slug) return;
    const link = `${window.location.origin}/r/${room.slug}`;
    try {
      await navigator.clipboard.writeText(link);
      showToast('Room link copied!');
    } catch {
      showToast('Failed to copy link');
    }
  }

  async function handleAdminJoin(e: React.FormEvent) {
    e.preventDefault();
    if (!joinNickname.trim()) return;
    logger.info('Admin joining as voter');
    try {
      await adminJoin.mutateAsync(joinNickname.trim());
      setAuth(getAuth(slug!));
    } catch (err) {
      logger.warn('Failed to join as voter:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  const {
    sortedTasks,
    sortField,
    sortDirection,
    onlyUnestimated,
    unestimatedCount,
    setSortField,
    setSortDirection,
    setOnlyUnestimated,
  } = useSortedTasks(room?.tasks ?? [], roomId ?? '');

  const submitEstimate = useSubmitEstimate(slug ?? '');
  const deleteEstimate = useDeleteEstimate(slug ?? '');
  const setFinalEstimate = useSetFinalEstimate(slug ?? '');
  const updateTask = useUpdateTask(slug ?? '');

  async function handleEstimate(taskId: string, value: StoryPoints | null) {
    if (!slug || !roomId) return;
    logger.debug(`Estimate: task=${taskId} sp=${value}`);

    const previousValue = votes[taskId] ?? null;

    if (value === null) {
      setVotes((prev) => {
        const updated = { ...prev };
        delete updated[taskId];
        return updated;
      });
    } else {
      setVotes((prev) => ({ ...prev, [taskId]: value }));
    }

    try {
      if (value === null) {
        await deleteEstimate.mutateAsync(taskId);
      } else {
        await submitEstimate.mutateAsync({ taskId, storyPoints: value });
      }
    } catch (err) {
      if (previousValue) {
        setVotes((prev) => ({ ...prev, [taskId]: previousValue }));
      } else {
        setVotes((prev) => {
          const updated = { ...prev };
          delete updated[taskId];
          return updated;
        });
      }
      logger.warn('Failed to submit estimate:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleSetFinalEstimate(taskId: string, value: StoryPoints) {
    if (!slug) return;
    try {
      await setFinalEstimate.mutateAsync({ taskId, storyPoints: value });
    } catch (err) {
      logger.warn('Failed to set final estimate:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleUpdateDescription(taskId: string, description: string) {
    if (!slug) return;
    try {
      await updateTask.mutateAsync({ taskId, body: { description } });
    } catch (err) {
      logger.warn('Failed to update description:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  if (loading && !room) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Spinner />
      </div>
    );
  }

  if (error) {
    if (error instanceof ApiError && error.status === 404) {
      return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
          <p className="text-efi-text-primary font-medium">Room not found</p>
          <Link
            to={`/p/${slug}`}
            className="text-sm text-efi-gold-light hover:text-efi-gold transition-colors no-underline hover:underline"
          >
            Back to Project
          </Link>
        </div>
      );
    }
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <p className="text-efi-error">{getErrorMessage(error)}</p>
        {error instanceof ApiError && error.traceId && (
          <button
            type="button"
            onClick={() => void navigator.clipboard.writeText(error.traceId!).catch(() => {})}
            title="Copy trace ID for support"
            className="text-xs text-efi-text-tertiary hover:text-efi-text-secondary font-mono transition-colors cursor-pointer rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
          >
            Trace: {error.traceId.slice(0, 8)}… [copy]
          </button>
        )}
      </div>
    );
  }

  // Split: LIVE rooms get their own dedicated view
  if (room?.roomType === 'LIVE') {
    return (
      <LiveRoomView
        slug={slug!}
        roomId={roomId!}
        room={room}
        auth={auth}
      />
    );
  }

  const isRevealed = room?.status === 'REVEALED' || room?.status === 'CLOSED';
  const tasks = room?.tasks ?? [];

  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8">
      {/* Header */}
      <div className="mb-5">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl sm:text-2xl font-bold text-efi-text-primary">
                <span className="text-sm font-normal text-efi-text-secondary mr-1">Room:</span>
                {room?.title}
              </h1>
              {room?.slug && <span className="text-xs font-mono text-efi-text-secondary bg-white/8 px-1.5 py-0.5 rounded">{room.slug}</span>}
              {room?.slug && (
                <button
                  type="button"
                  onClick={() => void handleCopyRoomLink()}
                  title="Copy room join link"
                  className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-efi-text-secondary border border-white/10 rounded-lg hover:text-efi-text-primary hover:border-white/20 transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                >
                  <Copy className="w-3 h-3" />
                  Share
                </button>
              )}
            </div>
            {room?.description && (
              <p className="text-efi-text-secondary mt-2">{room.description}</p>
            )}
          </div>
          <div className="flex gap-2 mt-3 sm:mt-0">
            {isRevealed && slug && (
              <Link
                to={`/p/${slug}/r/${roomId}/results`}
                className="px-3.5 py-1.5 rounded-lg text-sm font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 transition-colors no-underline whitespace-nowrap active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
              >
                View Results
              </Link>
            )}
          </div>
        </div>

        {room?.deadline && (
          <div className="flex flex-col sm:flex-row sm:items-center gap-4 sm:gap-6 mt-4 lg:hidden">
            <div className="flex items-center gap-2">
              <span className="text-sm text-efi-text-secondary">Deadline:</span>
              <CountdownTimer deadline={room.deadline} />
            </div>
          </div>
        )}
      </div>

      {/* Admin join banner */}
      {isAdmin && !hasParticipant && !isRevealed && (
        <form
          onSubmit={(e) => void handleAdminJoin(e)}
          className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 glass-gold rounded-xl p-4 mb-6"
        >
          <p className="text-sm text-efi-gold-light font-medium sm:mr-2">
            Join as voter to estimate tasks
          </p>
          <TextInput
            type="text"
            value={joinNickname}
            onChange={(e) => setJoinNickname(e.target.value)}
            placeholder="Your nickname"
            maxLength={100}
            className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
          />
          <button
            type="submit"
            disabled={adminJoin.isPending || !joinNickname.trim()}
            className="px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] flex items-center justify-center gap-2 whitespace-nowrap focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            {adminJoin.isPending ? <><ButtonSpinner /> Joining...</> : 'Join & Vote'}
          </button>
        </form>
      )}

      {/* Two-column layout: tasks + sidebar */}
      <div className="space-y-6 lg:space-y-0 lg:grid lg:grid-cols-[1fr_320px] lg:gap-6">
        <div>
          {tasks.length > 0 && (
            <SortControls
              sortField={sortField}
              sortDirection={sortDirection}
              onlyUnestimated={onlyUnestimated}
              unestimatedCount={unestimatedCount}
              onSortFieldChange={setSortField}
              onSortDirectionChange={setSortDirection}
              onOnlyUnestimatedChange={setOnlyUnestimated}
            />
          )}

          <div className="space-y-3">
            {sortedTasks.map((task) => (
              <TaskCard
                key={task.id}
                id={task.id}
                title={task.title}
                description={task.description || undefined}
                votedCount={task.votedCount}
                questionVotesCount={task.questionVotesCount}
                totalParticipants={task.totalParticipants}
                selectedSp={votes[task.id] ?? (task.myEstimate?.storyPoints as StoryPoints) ?? null}
                onEstimate={(taskId, value) => void handleEstimate(taskId, value)}
                disabled={isRevealed}
                revealed={isRevealed}
                allEstimates={task.allEstimates}
                averagePoints={task.averagePoints}
                medianPoints={task.medianPoints}
                finalEstimate={task.finalEstimate}
                isAdmin={isAdmin}
                onSetFinalEstimate={(taskId, value) => void handleSetFinalEstimate(taskId, value)}
                onUpdateDescription={isAdmin && !isRevealed ? (taskId, desc) => void handleUpdateDescription(taskId, desc) : undefined}
              />
            ))}

            {tasks.length === 0 && (
              <div className="text-center py-16 bg-white/3 rounded-2xl border border-dashed border-white/8">
                <span className="text-4xl mb-4 block">&#128203;</span>
                <p className="text-efi-text-secondary">No tasks yet.</p>
                {isAdmin && (
                  <p className="text-sm text-efi-text-tertiary mt-1">
                    <Link to={`/p/${slug}`} className="text-efi-gold-light hover:text-efi-text-primary transition-colors">
                      Manage tasks
                    </Link> in the project dashboard.
                  </p>
                )}
              </div>
            )}
          </div>
        </div>

        <RoomSidebar
          deadline={room?.deadline}
          roomType={room?.roomType ?? 'ASYNC'}
          tasks={tasks}
          isRevealed={isRevealed}
        />
      </div>

    </div>
  );
}

