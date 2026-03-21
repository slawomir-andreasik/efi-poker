import { useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Trash2 } from 'lucide-react';
import { InlineConfirmAction } from '@/components/InlineConfirmAction';
import { useQuery } from '@tanstack/react-query';
import { ApiError } from '@/api/client';
import { queryKeys } from '@/api/queryKeys';
import { roomApi } from '@/api/queries';
import { useProjectAuth } from '@/hooks/useProjectAuth';
import { useSubmitEstimate, useDeleteEstimate, useSetFinalEstimate, useUpdateTask } from '@/api/mutations';
import { logger } from '@/utils/logger';
import { getErrorMessage } from '@/utils/error';
import { useSortedTasks } from '@/hooks/useSortedTasks';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { useDeleteRoomAction } from '@/hooks/useDeleteRoomAction';
import { useToast } from '@/components/Toast';
import { PageSpinner } from '@/components/PageSpinner';
import { NotFoundState } from '@/components/NotFoundState';
import { TraceCopyButton } from '@/components/TraceCopyButton';
import { ShareButton } from '@/components/ShareButton';
import { CountdownTimer } from '@/components/CountdownTimer';
import { TaskCard } from '@/components/TaskCard';
import { SortControls } from '@/components/SortControls';
import { RoomSidebar } from '@/components/RoomSidebar';
import { LiveRoomView } from '@/components/LiveRoomView';
import { RoomSettings } from '@/components/RoomSettings';
import { ParticipantProgress } from '@/components/ParticipantProgress';
import { AdminJoinBanner } from '@/components/AdminJoinBanner';
import type { StoryPoints } from '@/api/types';

export function RoomPage() {
  const { slug, roomId } = useParams<{ slug: string; roomId: string }>();
  const { auth, isAdmin } = useProjectAuth(slug);
  const hasParticipant = Boolean(auth.participantId);
  const { showToast } = useToast();
  const projectName = auth.projectName ?? slug;

  const [votes, setVotes] = useState<Record<string, StoryPoints>>({});
  const { handleDeleteRoom, isPending: deleteRoomPending } = useDeleteRoomAction(slug ?? '', roomId ?? '');

  const { data: room, isLoading: loading, error } = useQuery({
    queryKey: queryKeys.rooms.detail(roomId!),
    queryFn: () => roomApi.detail(roomId!, slug!),
    enabled: Boolean(roomId && slug),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'REVEALED' || status === 'CLOSED') return false;
      if (query.state.status === 'error') return false;
      return 5_000;
    },
  });

  useDocumentTitle(room?.title, projectName);

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

  const handleEstimate = useCallback(async (taskId: string, value: StoryPoints | null, comment?: string) => {
    if (!slug || !roomId) return;

    // Draft save: no SP, only comment (blur without selecting SP)
    if (value === null && comment) {
      logger.debug(`Draft comment: task=${taskId}`);
      try {
        await submitEstimate.mutateAsync({ taskId, comment });
      } catch (err) {
        logger.warn('Failed to save draft comment:', getErrorMessage(err));
      }
      return;
    }

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
        await submitEstimate.mutateAsync({ taskId, storyPoints: value, comment });
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
  }, [deleteEstimate, submitEstimate, votes, showToast, roomId, slug]);

  const handleSetFinalEstimate = useCallback(async (taskId: string, value: StoryPoints) => {
    if (!slug) return;
    try {
      await setFinalEstimate.mutateAsync({ taskId, storyPoints: value });
    } catch (err) {
      logger.warn('Failed to set final estimate:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }, [setFinalEstimate, showToast, slug]);

  const handleUpdateDescription = useCallback(async (taskId: string, description: string) => {
    if (!slug) return;
    try {
      await updateTask.mutateAsync({ taskId, body: { description } });
    } catch (err) {
      logger.warn('Failed to update description:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }, [updateTask, showToast, slug]);

  if (loading && !room) {
    return <PageSpinner />;
  }

  if (error) {
    if (error instanceof ApiError && error.status === 404) {
      return <NotFoundState message="Room not found" backTo={`/p/${slug}`} backLabel="Back to Project" />;
    }
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <p className="text-efi-error">{getErrorMessage(error)}</p>
        {error instanceof ApiError && error.traceId && (
          <TraceCopyButton traceId={error.traceId} />
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
            <div className="flex items-center gap-2 flex-wrap min-w-0">
              <h1 className="text-xl sm:text-2xl font-bold text-efi-text-primary truncate min-w-0">
                <span className="text-sm font-normal text-efi-text-secondary mr-1">Room:</span>
                {room?.title}
              </h1>
              {room?.slug && <span className="text-xs font-mono text-efi-text-secondary bg-white/8 px-1.5 py-0.5 rounded">{room.slug}</span>}
              {room?.slug && <ShareButton roomSlug={room.slug} />}
            </div>
            {room?.description && (
              <p className="text-efi-text-secondary mt-2">{room.description}</p>
            )}
          </div>
          <div className="flex items-center gap-2 mt-3 sm:mt-0">
            {isRevealed && slug && (
              <Link
                to={`/p/${slug}/r/${roomId}/results`}
                className="px-3.5 py-1.5 rounded-lg text-sm font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 transition-colors no-underline whitespace-nowrap active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
              >
                View Results
              </Link>
            )}
            {isAdmin && (
              <InlineConfirmAction
                label="Delete room?"
                onConfirm={() => void handleDeleteRoom()}
                isLoading={deleteRoomPending}
                icon={<Trash2 className="w-4 h-4" />}
                title="Delete room"
              />
            )}
          </div>
        </div>

        {room?.deadline && (
          <div className="flex flex-col sm:flex-row sm:items-center gap-4 sm:gap-6 mt-4 lg:hidden">
            <div className="flex items-center gap-2">
              <span className="text-sm text-efi-text-secondary">Deadline:</span>
              <CountdownTimer deadline={room.deadline} />
            </div>
            {isAdmin && room.autoRevealOnDeadline === false && (
              <span className="text-xs text-efi-text-tertiary">Manual reveal required</span>
            )}
          </div>
        )}
      </div>

      {/* Admin join banner */}
      {isAdmin && !hasParticipant && !isRevealed && (
        <AdminJoinBanner slug={slug!} onJoined={() => {}} />
      )}

      {/* Session ended banner (non-admin participants) */}
      {room?.status === 'CLOSED' && !isAdmin && (
        <div className="glass-frost rounded-xl p-4 mb-6 border border-efi-ash/20 text-center">
          <p className="text-efi-text-secondary font-medium">
            This session has ended. View results below.
          </p>
        </div>
      )}

      {/* Room settings (admin only) */}
      {isAdmin && room && (
        <div className="mb-6">
          <RoomSettings slug={slug!} room={room} />
        </div>
      )}

      {/* Participant progress (OPEN state only) */}
      {!isRevealed && room && slug && roomId && (
        <div className="mb-6">
          <ParticipantProgress roomId={roomId} slug={slug} />
        </div>
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
                onEstimate={(taskId, value, comment) => void handleEstimate(taskId, value, comment)}
                disabled={isRevealed}
                revealed={isRevealed}
                allEstimates={task.allEstimates}
                averagePoints={task.averagePoints}
                medianPoints={task.medianPoints}
                finalEstimate={task.finalEstimate}
                isAdmin={isAdmin}
                onSetFinalEstimate={(taskId, value) => void handleSetFinalEstimate(taskId, value)}
                onUpdateDescription={isAdmin ? (taskId, desc) => void handleUpdateDescription(taskId, desc) : undefined}
                commentTemplate={room?.commentTemplate}
                commentRequired={room?.commentRequired}
                myComment={task.myEstimate?.comment}
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

