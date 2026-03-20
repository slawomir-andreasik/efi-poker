import { useState, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Check, Trash2, Eye, RotateCw } from 'lucide-react';
import { InlineConfirmAction } from '@/components/InlineConfirmAction';
import { getAuth } from '@/api/client';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { queryKeys } from '@/api/queryKeys';
import { roomApi } from '@/api/queries';
import { useRevealRoom, useNewRound, useSubmitEstimate, useDeleteEstimate, useUpdateRoom } from '@/api/mutations';
import { logger } from '@/utils/logger';
import { useToast } from '@/components/Toast';
import { Spinner, ButtonSpinner } from '@/components/Spinner';
import { RoundHistoryPanel } from '@/components/RoundHistoryPanel';
import { EstimateButtons } from '@/components/EstimateButtons';
import { AdminJoinBanner } from '@/components/AdminJoinBanner';
import { SP_NOT_APPLICABLE } from '@/api/types';
import type { StoryPoints, RoomDetailResponse } from '@/api/types';
import { getErrorMessage } from '@/utils/error';
import { useSaveIndicator } from '@/hooks/useSaveIndicator';
import { useDeleteRoomAction } from '@/hooks/useDeleteRoomAction';
import { TextInput } from '@/components/TextInput';
import { CommentInput } from '@/components/CommentInput';
import { RoomSettings } from '@/components/RoomSettings';
import { ShareButton } from '@/components/ShareButton';
import { getDraftComment, clearDraftComment, saveDraftComment } from '@/api/client';

interface LiveRoomViewProps {
  slug: string;
  roomId: string;
  room: RoomDetailResponse;
  auth: { adminCode?: string; participantId?: string; nickname?: string };
}

export function LiveRoomView({ slug, roomId, room: initialRoom, auth }: LiveRoomViewProps) {
  const { isAdmin: isSiteAdmin } = useCurrentUser();
  const isAdmin = Boolean(auth.adminCode) || isSiteAdmin;
  const { showToast } = useToast();

  const [topicInput, setTopicInput] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [selectedEstimate, setSelectedEstimate] = useState<StoryPoints | null>(null);
  const [comment, setComment] = useState('');
  const { saving, showSaveIndicator, resetSaving } = useSaveIndicator();
  const { handleDeleteRoom, isPending: deleteRoomPending } = useDeleteRoomAction(slug, roomId);

  const { data: liveState, isLoading: loading } = useQuery({
    queryKey: queryKeys.rooms.live(roomId),
    queryFn: () => roomApi.live(roomId, slug),
    refetchInterval: (query) => (query.state.status === 'error' ? false : 2_000),
  });

  const { data: history = [], isError: historyError } = useQuery({
    queryKey: queryKeys.rooms.history(roomId),
    queryFn: () => roomApi.history(roomId, slug),
  });

  // Mutations
  const revealRoom = useRevealRoom(slug);
  const newRound = useNewRound(slug, roomId);
  const submitEstimate = useSubmitEstimate(slug);
  const deleteEstimate = useDeleteEstimate(slug);
  const updateRoom = useUpdateRoom(slug);

  // Editable topic for active round (admin only)
  const [editingTopic, setEditingTopic] = useState<string | null>(null);

  function handleSaveTopic(value: string) {
    const serverTopic = liveState?.topic ?? '';
    const newTopic = value.trim();
    setEditingTopic(null);
    if (newTopic === (serverTopic ?? '')) return;
    updateRoom.mutate(
      { roomId, body: { topic: newTopic } },
      {
        onError: (err) => {
          logger.warn('Failed to save topic:', getErrorMessage(err));
          showToast(getErrorMessage(err));
        },
      },
    );
  }

  // Admin join state
  const [currentAuth, setCurrentAuth] = useState(auth);
  const hasParticipant = Boolean(currentAuth.participantId);

  // Sync selected estimate from live state
  useEffect(() => {
    if (liveState?.myEstimate?.storyPoints) {
      setSelectedEstimate(liveState.myEstimate.storyPoints as StoryPoints);
    } else if (!liveState?.myEstimate) {
      setSelectedEstimate(null);
    }
  }, [liveState?.myEstimate]);

  // Initialize comment from server, localStorage draft, or template (once)
  const commentInitializedRef = useRef(false);
  useEffect(() => {
    if (commentInitializedRef.current) return;
    if (liveState?.myEstimate?.comment) {
      setComment(liveState.myEstimate.comment);
      commentInitializedRef.current = true;
    } else {
      const draft = liveState?.taskId ? getDraftComment(liveState.taskId) : null;
      if (draft) {
        setComment(draft);
        commentInitializedRef.current = true;
      } else if (liveState?.commentTemplate) {
        setComment(liveState.commentTemplate);
        commentInitializedRef.current = true;
      }
    }
  }, [liveState?.myEstimate?.comment, liveState?.commentTemplate, liveState?.taskId]);

  async function handleEstimateSubmit(value: StoryPoints) {
    if (!liveState?.taskId) return;
    logger.debug(`Live estimate: task=${liveState.taskId} sp=${value}`);

    const isUnvote = selectedEstimate === value;
    const prev = selectedEstimate;
    setSelectedEstimate(isUnvote ? null : value);
    setSubmitting(true);

    try {
      if (isUnvote) {
        await deleteEstimate.mutateAsync(liveState.taskId);
      } else {
        await submitEstimate.mutateAsync({ taskId: liveState.taskId, storyPoints: value, comment: comment.trim() || undefined });
        clearDraftComment(liveState.taskId);
        showSaveIndicator();
      }
    } catch (err) {
      setSelectedEstimate(prev);
      logger.warn('Failed to submit estimate:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleEstimateFromButtons(value: StoryPoints | null) {
    if (value === null) {
      // Unvote: EstimateButtons sends null when clicking selected button
      if (selectedEstimate && liveState?.taskId) {
        const prev = selectedEstimate;
        setSelectedEstimate(null);
        setSubmitting(true);
        try {
          await deleteEstimate.mutateAsync(liveState.taskId);
        } catch (err) {
          setSelectedEstimate(prev);
          showToast(getErrorMessage(err));
        } finally {
          setSubmitting(false);
        }
      }
    } else {
      await handleEstimateSubmit(value);
    }
  }

  async function handleReveal() {
    logger.debug('Live reveal votes');
    try {
      await revealRoom.mutateAsync(roomId);
    } catch (err) {
      logger.warn('Failed to reveal:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleNewRound() {
    logger.debug(`Live new round: topic=${topicInput.trim() || '(none)'}`);
    try {
      const body = topicInput.trim() ? { topic: topicInput.trim() } : {};
      await newRound.mutateAsync(body);
      if (liveState?.taskId) clearDraftComment(liveState.taskId);
      setTopicInput('');
      setSelectedEstimate(null);
      setComment(liveState?.commentTemplate ?? '');
      resetSaving();
      commentInitializedRef.current = false;
    } catch (err) {
      logger.warn('Failed to start new round:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  if (loading && !liveState) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Spinner />
      </div>
    );
  }

  const status = liveState?.status ?? initialRoom.status;
  const isRevealed = status === 'REVEALED' || status === 'CLOSED';
  const commentRequired = liveState?.commentRequired ?? false;
  const showCommentBox = !isRevealed && (liveState?.commentTemplate || commentRequired);
  const topic = liveState?.topic ?? initialRoom.topic;
  const roundNumber = liveState?.roundNumber ?? initialRoom.roundNumber;
  const participants = liveState?.participants ?? [];
  const results = liveState?.results;
  const votedCount = participants.filter((p) => p.hasVoted).length;

  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8">
      {/* Header */}
      <div className="mb-6">
        <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between">
          <div>
            <div className="flex items-center gap-2 flex-wrap min-w-0">
              <h1 className="text-xl sm:text-2xl font-bold text-efi-text-primary truncate min-w-0">{initialRoom.title}</h1>
              <span className="shrink-0 text-xs font-mono text-efi-text-secondary bg-white/8 px-1.5 py-0.5 rounded">{initialRoom.slug}</span>
              <span className="shrink-0 inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium bg-efi-live/20 text-efi-live border border-efi-live/30">
                <span className="relative flex h-1.5 w-1.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-efi-live opacity-75" />
                  <span className="relative inline-flex rounded-full h-1.5 w-1.5 bg-efi-live" />
                </span>
                Live
              </span>
              <span className="text-sm text-efi-text-tertiary">Round {roundNumber}</span>
            </div>
            {topic && (
              <p className="text-efi-text-secondary mt-2 text-sm">
                <span className="text-efi-text-tertiary">Topic:</span>{' '}
                <span className="text-efi-text-primary font-medium">{topic}</span>
              </p>
            )}
          </div>
          <div className="flex items-center gap-2 mt-3 sm:mt-0">
            <ShareButton roomSlug={initialRoom.slug} />
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
      </div>

      {/* Admin join banner */}
      {isAdmin && !hasParticipant && !isRevealed && (
        <AdminJoinBanner slug={slug} onJoined={() => setCurrentAuth(getAuth(slug))} />
      )}

      {/* Admin controls */}
      {isAdmin && (
        <div className="glass-frost rounded-xl p-4 mb-6 border border-efi-info/20">
          <h2 className="text-xs font-semibold text-efi-text-secondary uppercase tracking-wider mb-3">Admin Controls</h2>
          {!isRevealed ? (
            <div className="flex flex-col sm:flex-row gap-3">
              <TextInput
                type="text"
                value={editingTopic ?? liveState?.topic ?? ''}
                onChange={(e) => setEditingTopic(e.target.value)}
                onBlur={(e) => handleSaveTopic(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') e.currentTarget.blur();
                  if (e.key === 'Escape') {
                    setEditingTopic(null);
                    e.currentTarget.blur();
                  }
                }}
                placeholder="Topic for this round (optional)"
                maxLength={2000}
                className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
              />
              <button
                type="button"
                onClick={() => void handleReveal()}
                disabled={revealRoom.isPending}
                className="px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-2 whitespace-nowrap focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
              >
                {revealRoom.isPending ? <><ButtonSpinner /> Revealing...</> : <><Eye className="w-4 h-4" /> Reveal</>}
              </button>
            </div>
          ) : (
            <div className="flex flex-col sm:flex-row gap-3">
              <TextInput
                type="text"
                value={topicInput}
                onChange={(e) => setTopicInput(e.target.value)}
                placeholder="Topic for next round (optional)"
                maxLength={255}
                className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
              />
              <button
                type="button"
                onClick={() => void handleNewRound()}
                disabled={newRound.isPending}
                className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-info/30 text-efi-info hover:bg-efi-info/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-2 whitespace-nowrap focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
              >
                {newRound.isPending ? <><ButtonSpinner /> Starting...</> : <><RotateCw className="w-4 h-4" /> New Round</>}
              </button>
            </div>
          )}
        </div>
      )}

      {/* Room settings (admin only) */}
      {isAdmin && (
        <div className="mb-6">
          <RoomSettings slug={slug} room={initialRoom} />
        </div>
      )}

      <div className="space-y-6 lg:space-y-0 lg:grid lg:grid-cols-[1fr_320px] lg:gap-6">
        {/* Left: estimate buttons + results */}
        <div className="space-y-6">
          {/* Estimate buttons */}
          {!isRevealed && (
            <div>
              <h2 className="text-sm font-semibold text-efi-text-secondary uppercase tracking-wider mb-3 flex items-center gap-2">
                Your Vote
                {votedCount > 0 && (
                  <span className="font-normal text-efi-text-tertiary lowercase">
                    {votedCount}/{participants.length} voted
                  </span>
                )}
                {liveState?.questionVotesCount != null && liveState.questionVotesCount > 0 && (
                  <span
                    className="inline-flex items-center justify-center w-5 h-5 rounded-full bg-efi-warning/20 text-efi-warning border border-efi-warning/30 text-xs font-bold cursor-help ml-1"
                    title={`${liveState.questionVotesCount} person${liveState.questionVotesCount > 1 ? 's' : ''} have questions/doubts. Please clarify.`}
                  >
                    ?
                  </span>
                )}
              </h2>
              <EstimateButtons
                selectedValue={selectedEstimate}
                onSelect={(value) => void handleEstimateFromButtons(value)}
                disabled={submitting || !hasParticipant}
              />
              {showCommentBox && hasParticipant && (
                <CommentInput
                  comment={comment}
                  onCommentChange={(value) => {
                    setComment(value);
                    if (liveState?.taskId) saveDraftComment(liveState.taskId, value);
                  }}
                  hasTemplate={Boolean(liveState?.commentTemplate)}
                  onCommentSave={(newComment) => {
                    if (liveState?.taskId) {
                      void submitEstimate.mutateAsync({
                        taskId: liveState.taskId,
                        storyPoints: selectedEstimate || undefined,
                        comment: newComment || undefined,
                      }).then(() => {
                        clearDraftComment(liveState.taskId!);
                        showSaveIndicator();
                      }).catch((err) => {
                        showToast(getErrorMessage(err));
                      });
                    }
                  }}
                  saving={saving}
                />
              )}
              {!hasParticipant && (
                <p className="text-xs text-efi-text-tertiary mt-2">Join as a voter above to submit estimates.</p>
              )}
            </div>
          )}

          {/* Results */}
          {isRevealed && results && (
              <div className="glass-frost rounded-xl p-4 space-y-4">
                <div className="flex gap-6">
                  {results.averagePoints != null && (
                    <div>
                      <p className="text-xs text-efi-text-secondary">Average</p>
                      <p className="text-2xl font-bold text-efi-gold">{results.averagePoints.toFixed(1)}</p>
                    </div>
                  )}
                  {results.medianPoints != null && (
                    <div>
                      <p className="text-xs text-efi-text-secondary">Median</p>
                      <p className="text-2xl font-bold text-efi-text-primary">{results.medianPoints.toFixed(1)}</p>
                    </div>
                  )}
                </div>

                {results.estimates.length > 0 && (
                  <div className="flex flex-wrap gap-2 pt-2 border-t border-white/8">
                    {results.estimates.map((est) => {
                      const isQuestion = est.storyPoints === '?';
                      const estIsNA = est.storyPoints === SP_NOT_APPLICABLE;
                      return (
                        <div key={est.id} className="flex flex-col items-center gap-1 max-w-[140px]">
                          <span className={`text-xs truncate max-w-full ${isQuestion ? 'text-efi-warning/70' : estIsNA ? 'text-efi-text-tertiary' : 'text-efi-text-secondary'}`}>{est.participantNickname}</span>
                          <span className={`text-lg font-bold rounded-lg w-12 h-12 flex items-center justify-center ${isQuestion
                              ? 'text-efi-warning bg-efi-warning/20 border border-efi-warning/40'
                              : estIsNA ? 'text-efi-text-tertiary bg-white/4 border border-white/8'
                              : 'text-efi-gold bg-efi-gold/10 border border-efi-gold/30'
                            }`}>
                            {est.storyPoints}
                          </span>
                          {est.comment && (
                            <p className="text-xs text-efi-text-secondary mt-1 text-center max-w-[140px] break-words line-clamp-3">{est.comment}</p>
                          )}
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
          )}

          {isRevealed && !results && (
            <div className="text-center py-8">
              <p className="text-efi-text-secondary">No estimates submitted this round.</p>
            </div>
          )}
        </div>

        {/* Right: participants */}
        <aside className="lg:sticky lg:top-[var(--nav-total-height)]">
            <div className="glass-whisper rounded-2xl p-4 border border-efi-info/20">
              <h2 className="text-xs font-semibold text-efi-text-secondary uppercase tracking-wider mb-3">
                Participants ({participants.length})
              </h2>
              {participants.length > 0 ? (
                <div className="space-y-2">
                  {participants.map((p) => (
                    <div key={p.participantId} className="flex items-center gap-2 bg-efi-well rounded-lg px-3 py-2">
                      <div className={`w-2 h-2 rounded-full flex-shrink-0 ${p.hasVoted ? 'bg-efi-success' : 'bg-white/20'}`} />
                      <span className={`text-sm flex-1 truncate ${p.hasVoted ? 'text-efi-text-primary' : 'text-efi-text-secondary'}`}>
                        {p.nickname}
                      </span>
                      {p.hasVoted ? (
                        <Check className="w-4 h-4 text-efi-success flex-shrink-0" />
                      ) : (
                        <span className="text-xs text-efi-text-tertiary">waiting</span>
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-efi-text-tertiary">No participants yet.</p>
              )}
            </div>
        </aside>
      </div>

      {/* Round history */}
      <div className="mt-6">
        {historyError ? (
          <p className="text-sm text-efi-error">Failed to load round history.</p>
        ) : (
          <RoundHistoryPanel history={history} />
        )}
      </div>
    </div>
  );
}
