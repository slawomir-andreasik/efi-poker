import { useQuery } from '@tanstack/react-query';
import {
  Archive,
  Copy,
  Download,
  Eye,
  Plus,
  RotateCcw,
  Square,
  Trash2,
  Upload,
  X,
} from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ApiError, getJwt, removeProject, saveAuth } from '@/api/client';
import {
  useAddTask,
  useArchiveParticipant,
  useCreateRoom,
  useDeleteParticipant,
  useDeleteProject,
  useDeleteTask,
  useFinishSession,
  useImportTasks,
  useReopenRoom,
  useRevealRoom,
  useUnarchiveParticipant,
  useUpdateProject,
  useUpdateTask,
} from '@/api/mutations';
import { projectApi, roomApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import type { AutoAssignedEstimate, RoomType } from '@/api/types';
import { AddTaskForm } from '@/components/AddTaskForm';
import { CountdownTimer } from '@/components/CountdownTimer';
import { DeadlineInput, formatPreview, getDefaultDeadline } from '@/components/DeadlineInput';
import { FinishSessionDialog } from '@/components/FinishSessionDialog';
import { ImportModal } from '@/components/ImportModal';
import { InlineConfirmAction } from '@/components/InlineConfirmAction';
import { Modal } from '@/components/Modal';
import { NotFoundState } from '@/components/NotFoundState';
import { PageSpinner } from '@/components/PageSpinner';
import { RandomNameButton } from '@/components/RandomNameButton';
import { DEFAULT_COMMENT_TEMPLATE, RoomSettings } from '@/components/RoomSettings';
import { ShareButton } from '@/components/ShareButton';
import { ButtonSpinner } from '@/components/Spinner';
import { TextArea, TextInput } from '@/components/TextInput';
import { useToast } from '@/components/Toast';
import { TraceCopyButton } from '@/components/TraceCopyButton';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { useProjectAuth } from '@/hooks/useProjectAuth';
import { Linkify } from '@/lib/linkify';
import { ghostLinkBtn, outlineBtn, primaryBtn } from '@/styles/buttons';
import { getErrorMessage } from '@/utils/error';
import { logger } from '@/utils/logger';
import { generateRoomName } from '@/utils/nameGenerator';
import { roomTypeBadge, statusBadge, statusLabel } from '@/utils/roomBadges';

export function ProjectPage() {
  const { slug } = useParams<{ slug: string }>();
  const { auth, isAdmin } = useProjectAuth(slug);
  const { showToast } = useToast();
  useDocumentTitle(auth.projectName ?? slug);

  // Admin-only state
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const [showImport, setShowImport] = useState(false);
  const [editingTask, setEditingTask] = useState<{
    id: string;
    title: string;
    description: string;
  } | null>(null);
  const [pendingDelete, setPendingDelete] = useState<{
    type: 'task';
    id: string;
    name: string;
  } | null>(null);
  const [participantRemoveModal, setParticipantRemoveModal] = useState<{
    id: string;
    nickname: string;
  } | null>(null);
  const [showArchivedParticipants, setShowArchivedParticipants] = useState(false);
  const [editingName, setEditingName] = useState<string | null>(null);

  // Shared create room state
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [deadline, setDeadline] = useState(getDefaultDeadline(3));
  const [roomType, setRoomType] = useState<RoomType>('LIVE');
  const [autoRevealOnDeadline, setAutoRevealOnDeadline] = useState(true);
  const [commentTemplate, setCommentTemplate] = useState('');
  const [commentRequired, setCommentRequired] = useState(false);

  // Queries
  const {
    data: project,
    isLoading: loading,
    error,
  } = useQuery({
    queryKey: queryKeys.projects.detail(slug!),
    queryFn: () => projectApi.detail(slug!),
    enabled: Boolean(slug),
    refetchInterval: (query) => (query.state.status === 'error' ? false : 10_000),
  });

  const { data: rooms } = useQuery({
    queryKey: queryKeys.projects.rooms(slug!),
    queryFn: () => projectApi.rooms(slug!),
    enabled: Boolean(slug && project),
    refetchInterval: project ? 10_000 : false,
  });

  const { data: room } = useQuery({
    queryKey: queryKeys.rooms.admin(selectedRoomId!),
    queryFn: () => roomApi.admin(selectedRoomId!, slug!),
    enabled: Boolean(selectedRoomId && slug),
    refetchInterval: 3_000,
  });

  const { data: participants } = useQuery({
    queryKey: queryKeys.projects.participants(slug!),
    queryFn: () => projectApi.participants(slug!),
    enabled: Boolean(slug && project),
    refetchInterval: isAdmin && project ? 30_000 : undefined,
  });

  const activeParticipants = useMemo(
    () => participants?.filter((p) => !p.archived) ?? [],
    [participants],
  );
  const archivedParticipants = useMemo(
    () => participants?.filter((p) => p.archived) ?? [],
    [participants],
  );

  // Mutations
  const [showFinishDialog, setShowFinishDialog] = useState(false);
  const [finishRoomId, setFinishRoomId] = useState<string | null>(null);
  const [autoAssigned, setAutoAssigned] = useState<AutoAssignedEstimate[] | null>(null);

  const createRoom = useCreateRoom(slug ?? '');
  const revealRoom = useRevealRoom(slug ?? '');
  const reopenRoom = useReopenRoom(slug ?? '');
  const finishSession = useFinishSession(slug ?? '');
  const addTask = useAddTask(slug ?? '');
  const importTasks = useImportTasks(slug ?? '');
  const updateTask = useUpdateTask(slug ?? '');
  const deleteTask = useDeleteTask(slug ?? '');
  const deleteParticipant = useDeleteParticipant(slug ?? '');
  const archiveParticipant = useArchiveParticipant(slug ?? '');
  const unarchiveParticipant = useUnarchiveParticipant(slug ?? '');
  const updateProject = useUpdateProject(slug ?? '');
  const deleteProjectMutation = useDeleteProject(slug ?? '');
  const navigate = useNavigate();

  useEffect(() => {
    if (slug && project?.name) {
      logger.debug(`Caching project name: slug=${slug} name=${project.name}`);
      saveAuth(slug, { projectName: project.name });
    }
  }, [slug, project?.name]);

  if (loading && !project) {
    return <PageSpinner />;
  }

  if (error) {
    if (error instanceof ApiError && error.status === 404) {
      if (slug) removeProject(slug);
      return <NotFoundState message="Project not found" backTo="/" backLabel="Back to Projects" />;
    }
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <p className="text-efi-error">{getErrorMessage(error)}</p>
        {error instanceof ApiError && error.traceId && <TraceCopyButton traceId={error.traceId} />}
      </div>
    );
  }

  const roomList = rooms || [];

  async function handleCreateRoom(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim() || !slug) return;
    if (roomType === 'ASYNC' && !deadline) return;

    logger.debug(`Create room: type=${roomType} title=${title.trim()}`);
    try {
      await createRoom.mutateAsync({
        title: title.trim(),
        description: description.trim() || undefined,
        deadline: roomType === 'ASYNC' ? new Date(deadline).toISOString() : undefined,
        roomType,
        autoRevealOnDeadline: roomType === 'ASYNC' ? autoRevealOnDeadline : undefined,
        commentTemplate: commentTemplate.trim() || undefined,
        commentRequired: commentRequired || undefined,
      });
      setTitle('');
      setDescription('');
      setDeadline(getDefaultDeadline(3));
      setRoomType('LIVE');
      setAutoRevealOnDeadline(true);
      setCommentTemplate('');
      setCommentRequired(false);
      setShowForm(false);
    } catch (err) {
      logger.warn('Failed to create room:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleReveal(roomId: string) {
    logger.debug(`Reveal room: roomId=${roomId}`);
    try {
      await revealRoom.mutateAsync(roomId);
    } catch (err) {
      logger.warn('Failed to reveal:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleReopen(roomId: string) {
    logger.debug(`Reopen room: roomId=${roomId}`);
    try {
      await reopenRoom.mutateAsync(roomId);
    } catch (err) {
      logger.warn('Failed to reopen room:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleFinishSession(revealVotes: boolean) {
    if (!finishRoomId) return;
    try {
      const result = await finishSession.mutateAsync({ roomId: finishRoomId, revealVotes });
      if (result.autoAssignedEstimates.length > 0) {
        setAutoAssigned(result.autoAssignedEstimates);
      } else {
        setShowFinishDialog(false);
        setFinishRoomId(null);
      }
    } catch (err) {
      showToast(getErrorMessage(err));
    }
  }

  async function handleExportCsv(roomId: string) {
    setExporting(true);
    try {
      const csv = await roomApi.resultsExport(roomId, slug!);
      const blob = new Blob([csv], { type: 'text/csv' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `room-${roomId}.csv`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      logger.warn('Failed to export CSV:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    } finally {
      setExporting(false);
    }
  }

  async function handleAddTask(taskTitle: string, taskDescription?: string) {
    if (!selectedRoomId) return;
    logger.debug(`Add task: title=${taskTitle}`);
    try {
      await addTask.mutateAsync({
        roomId: selectedRoomId,
        title: taskTitle,
        description: taskDescription,
      });
    } catch (err) {
      logger.warn('Failed to add task:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleImportTasks(titles: string[]) {
    if (!selectedRoomId) return;
    logger.debug(`Import tasks: count=${titles.length}`);
    try {
      await importTasks.mutateAsync({ roomId: selectedRoomId, titles });
      showToast(`Imported ${titles.length} task${titles.length !== 1 ? 's' : ''}`, 'success');
    } catch (err) {
      logger.warn('Failed to import tasks:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleSaveTask() {
    if (!editingTask) return;
    try {
      await updateTask.mutateAsync({
        taskId: editingTask.id,
        body: {
          title: editingTask.title.trim(),
          description: editingTask.description.trim() || undefined,
        },
      });
      setEditingTask(null);
    } catch (err) {
      logger.warn('Failed to update task:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleDeleteTask(taskId: string) {
    try {
      await deleteTask.mutateAsync(taskId);
    } catch (err) {
      logger.warn('Failed to delete task:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleCopyLink(participantId: string) {
    const link = `${window.location.origin}/p/${slug}/join?pid=${participantId}`;
    try {
      await navigator.clipboard.writeText(link);
      showToast('Link copied to clipboard');
    } catch {
      showToast('Failed to copy link');
    }
  }

  async function handleCopyJoinLink() {
    const link = `${window.location.origin}/p/${slug}/join`;
    try {
      await navigator.clipboard.writeText(link);
      showToast('Join link copied!');
    } catch {
      showToast('Failed to copy link');
    }
  }

  async function handleDeleteParticipant(participantId: string) {
    try {
      await deleteParticipant.mutateAsync(participantId);
    } catch (err) {
      logger.warn('Failed to delete participant:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleArchiveParticipant(participantId: string) {
    try {
      await archiveParticipant.mutateAsync(participantId);
    } catch (err) {
      logger.warn('Failed to archive participant:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleUnarchiveParticipant(participantId: string) {
    try {
      await unarchiveParticipant.mutateAsync(participantId);
    } catch (err) {
      logger.warn('Failed to unarchive participant:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  function confirmDelete() {
    if (!pendingDelete) return;
    const { id } = pendingDelete;
    setPendingDelete(null);
    void handleDeleteTask(id);
  }

  async function handleSaveName() {
    if (editingName == null || !editingName.trim()) return;
    try {
      await updateProject.mutateAsync({ name: editingName.trim() });
      setEditingName(null);
      showToast('Project name updated', 'success');
    } catch (err) {
      logger.warn('Failed to update project name:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleDeleteProject() {
    if (!slug) return;
    try {
      await deleteProjectMutation.mutateAsync();
      removeProject(slug);
      showToast('Project deleted', 'success');
      void navigate('/');
    } catch (err) {
      logger.warn('Failed to delete project:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  // --- Admin view ---
  if (isAdmin) {
    const participantNames = room?.participants.map((p) => p.nickname) ?? [];
    const isRoomLive = room?.roomType === 'LIVE';

    return (
      <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8">
          <div>
            {editingName != null ? (
              <div className="flex items-center gap-2">
                <TextInput
                  type="text"
                  value={editingName}
                  onChange={(e) => setEditingName(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') void handleSaveName();
                    if (e.key === 'Escape') setEditingName(null);
                  }}
                  maxLength={255}
                  autoFocus
                  className="text-xl sm:text-2xl font-bold bg-efi-well border border-efi-gold-light/20 rounded-lg px-2 py-0.5 text-efi-text-primary focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
                />
                <button
                  type="button"
                  onClick={() => void handleSaveName()}
                  disabled={updateProject.isPending || !editingName.trim()}
                  className="px-2 py-1 rounded-lg text-xs font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                >
                  {updateProject.isPending ? <ButtonSpinner /> : 'Save'}
                </button>
                <button
                  type="button"
                  onClick={() => setEditingName(null)}
                  className="px-2 py-1 rounded-lg text-xs font-medium text-efi-text-secondary hover:text-efi-text-primary transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                >
                  Cancel
                </button>
              </div>
            ) : (
              <h1
                className="text-xl sm:text-2xl font-bold text-efi-text-primary cursor-pointer hover:text-efi-gold-light transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none rounded"
                onClick={() => setEditingName(project?.name ?? '')}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    setEditingName(project?.name ?? '');
                  }
                }}
                title="Click to edit project name"
              >
                <span className="text-sm font-normal text-efi-text-secondary mr-1">Project:</span>
                {project?.name}
              </h1>
            )}
            <div className="flex items-center gap-3 mt-1">
              <p className="text-sm text-efi-text-secondary">
                {roomList.length} {roomList.length === 1 ? 'room' : 'rooms'}
              </p>
            </div>
          </div>
          <div className="mt-3 sm:mt-0 flex gap-3">
            <button
              type="button"
              onClick={() => setShowForm(true)}
              className={`${primaryBtn} flex items-center gap-1`}
            >
              <Plus className="w-4 h-4" /> New Room
            </button>
            <button
              type="button"
              onClick={() => void handleCopyJoinLink()}
              title="Copy project join link"
              className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-efi-text-secondary border border-white/10 rounded-lg hover:text-efi-text-primary hover:border-white/20 transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
            >
              <Copy className="w-3 h-3" />
              Share
            </button>
            {(roomList ?? []).some((r) => r.status === 'REVEALED' || r.status === 'CLOSED') && (
              <Link to={`/p/${slug}/analytics`} className={ghostLinkBtn}>
                Analytics
              </Link>
            )}
            <InlineConfirmAction
              label="Delete project?"
              onConfirm={() => void handleDeleteProject()}
              isLoading={deleteProjectMutation.isPending}
              icon={<Trash2 className="w-4 h-4" />}
              title="Delete project"
            />
          </div>
        </div>

        {/* Main grid */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Left: rooms + room detail */}
          <div className="lg:col-span-3 space-y-6">
            {/* Room list header */}
            <div>
              <h2 className="text-sm font-semibold text-efi-text-secondary uppercase tracking-wider mb-3">
                Rooms
              </h2>

              {roomList.length > 0 ? (
                <div className="flex flex-col gap-1">
                  {roomList.map((r) => (
                    <div key={r.id} className="flex items-center gap-1">
                      <button
                        type="button"
                        onClick={() => {
                          setSelectedRoomId(r.id);
                          setPendingDelete(null);
                        }}
                        className={`flex-1 text-left px-3 py-2.5 rounded-lg text-sm transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none flex items-center justify-between gap-2 ${
                          selectedRoomId === r.id
                            ? 'bg-efi-gold/15 border border-efi-gold/30 text-efi-text-primary'
                            : 'bg-white/[0.03] border border-white/6 text-efi-text-primary hover:bg-white/6 hover:border-white/10'
                        }`}
                      >
                        <span className="font-medium truncate">{r.title}</span>
                        <div className="flex items-center gap-1.5 shrink-0">
                          <span className="text-[10px] font-mono text-efi-text-secondary bg-white/8 px-1.5 py-0.5 rounded">
                            {r.slug}
                          </span>
                          <span
                            className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${roomTypeBadge(r.roomType)}`}
                          >
                            {r.roomType === 'LIVE' ? 'Live' : 'Async'}
                          </span>
                          <span
                            className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${statusBadge(r.status)}`}
                          >
                            {statusLabel(r.status)}
                          </span>
                        </div>
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 bg-white/3 rounded-2xl border border-dashed border-white/8">
                  <span className="text-4xl mb-4 block">&#128203;</span>
                  <p className="text-sm text-efi-text-tertiary">No rooms yet</p>
                </div>
              )}
            </div>

            {/* Selected room detail */}
            {room ? (
              <div className="glass-frost rounded-xl sm:rounded-2xl p-4 sm:p-6 border border-efi-gold-light/10">
                <div className="mb-6">
                  <div className="flex items-center gap-2 flex-wrap">
                    <h2 className="text-lg font-semibold text-efi-text-primary">{room.title}</h2>
                    <span className="text-xs font-mono text-efi-text-secondary bg-white/8 px-1.5 py-0.5 rounded">
                      {room.slug}
                    </span>
                    <span
                      className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${roomTypeBadge(room.roomType)}`}
                    >
                      {isRoomLive ? 'Live' : 'Async'}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-2 mt-3">
                    {room.status !== 'REVEALED' && room.status !== 'CLOSED' && !isRoomLive && (
                      <button
                        type="button"
                        onClick={() => void handleReveal(room.id)}
                        disabled={revealRoom.isPending}
                        className={`${primaryBtn} flex items-center gap-2`}
                      >
                        {revealRoom.isPending ? (
                          <>
                            <ButtonSpinner /> Revealing...
                          </>
                        ) : (
                          <>
                            <Eye className="w-4 h-4" /> Reveal
                          </>
                        )}
                      </button>
                    )}
                    {(room.status === 'REVEALED' || room.status === 'CLOSED') && (
                      <button
                        type="button"
                        onClick={() => void handleReopen(room.id)}
                        disabled={reopenRoom.isPending}
                        className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-info/30 text-efi-info hover:bg-efi-info/10 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-2 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                      >
                        {reopenRoom.isPending ? (
                          <>
                            <ButtonSpinner /> Reopening...
                          </>
                        ) : (
                          <>
                            <RotateCcw className="w-4 h-4" /> Reopen Voting
                          </>
                        )}
                      </button>
                    )}
                    {room.status !== 'CLOSED' && (
                      <button
                        type="button"
                        onClick={() => {
                          setFinishRoomId(room.id);
                          setShowFinishDialog(true);
                        }}
                        className="px-3 py-1.5 rounded-lg text-xs font-medium border border-efi-error/30 text-efi-error hover:bg-efi-error/10 transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-1.5 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                      >
                        <Square className="w-3.5 h-3.5" /> Finish
                      </button>
                    )}
                    {!isRoomLive && room.status !== 'REVEALED' && room.status !== 'CLOSED' && (
                      <button
                        type="button"
                        onClick={() => setShowImport(true)}
                        className={`${outlineBtn} flex items-center gap-2`}
                      >
                        <Upload className="w-4 h-4" /> Import Tasks
                      </button>
                    )}
                    {!isRoomLive && (
                      <button
                        type="button"
                        onClick={() => void handleExportCsv(room.id)}
                        disabled={exporting}
                        className={`${outlineBtn} flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed`}
                      >
                        {exporting ? (
                          <>
                            <ButtonSpinner /> Exporting...
                          </>
                        ) : (
                          <>
                            <Download className="w-4 h-4" /> Export CSV
                          </>
                        )}
                      </button>
                    )}
                    <Link to={`/p/${slug}/r/${room.id}`} className={`${outlineBtn} no-underline`}>
                      Open Voting
                    </Link>
                    {!isRoomLive && (room.status === 'REVEALED' || room.status === 'CLOSED') && (
                      <Link
                        to={`/p/${slug}/r/${room.id}/results`}
                        className={`${outlineBtn} no-underline`}
                      >
                        View Results
                      </Link>
                    )}
                  </div>
                </div>

                {/* Room settings (admin) */}
                <div className="mb-6">
                  <RoomSettings key={room.id} slug={slug!} room={room} />
                </div>

                {/* Share link */}
                <div className="mb-4">
                  <ShareButton roomSlug={room.slug} />
                </div>

                {/* Deadline display for Async rooms */}
                {!isRoomLive && room.deadline && (
                  <div className="mb-6 flex items-center gap-2 text-sm">
                    <span className="text-efi-text-secondary">Deadline:</span>
                    <span className="text-efi-text-primary">{formatPreview(room.deadline)}</span>
                    <CountdownTimer deadline={room.deadline} />
                  </div>
                )}

                {/* Add task controls */}
                {room.status !== 'REVEALED' &&
                  room.status !== 'CLOSED' &&
                  room.roomType !== 'LIVE' && (
                    <AddTaskForm onAdd={(t, desc) => handleAddTask(t, desc)} />
                  )}

                {/* Estimates table - ASYNC only; LIVE rooms use round-based voting inside the room */}
                {isRoomLive ? (
                  <div className="text-center py-6 text-efi-text-tertiary text-sm">
                    Round history and results are available inside the room.
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    {room.tasks.length > 0 ? (
                      <table className="w-full text-sm min-w-[500px]">
                        <thead>
                          <tr className="border-b border-white/8">
                            <th className="text-left py-2 px-2 text-efi-text-secondary font-medium">
                              Task
                            </th>
                            {participantNames.map((name) => (
                              <th
                                key={name}
                                className="text-center py-2 px-2 text-efi-text-secondary font-medium"
                              >
                                {name}
                              </th>
                            ))}
                          </tr>
                        </thead>
                        <tbody>
                          {room.tasks.map((task) => (
                            <tr key={task.id} className="border-b border-white/4">
                              <td className="py-2 px-2">
                                {editingTask?.id === task.id ? (
                                  <div className="space-y-1.5">
                                    <TextInput
                                      type="text"
                                      value={editingTask.title}
                                      onChange={(e) =>
                                        setEditingTask(
                                          (prev) => prev && { ...prev, title: e.target.value },
                                        )
                                      }
                                      maxLength={255}
                                      className="w-full rounded bg-efi-well border border-efi-gold-light/20 px-2 py-1 text-base text-efi-text-primary focus:outline-none focus:border-efi-gold"
                                    />
                                    <TextArea
                                      value={editingTask.description}
                                      onChange={(e) =>
                                        setEditingTask(
                                          (prev) =>
                                            prev && { ...prev, description: e.target.value },
                                        )
                                      }
                                      rows={2}
                                      placeholder="Description (optional)"
                                      className="w-full rounded bg-efi-well border border-efi-gold-light/20 px-2 py-1 text-base text-efi-text-secondary placeholder-efi-text-tertiary focus:outline-none focus:border-efi-gold resize-none"
                                    />
                                    <div className="flex gap-1.5">
                                      <button
                                        type="button"
                                        onClick={() => void handleSaveTask()}
                                        disabled={updateTask.isPending || !editingTask.title.trim()}
                                        className="px-2 py-0.5 rounded text-xs font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 cursor-pointer"
                                      >
                                        {updateTask.isPending ? 'Saving...' : 'Save'}
                                      </button>
                                      <button
                                        type="button"
                                        onClick={() => setEditingTask(null)}
                                        className="px-2 py-0.5 rounded text-xs border border-white/12 text-efi-text-secondary hover:text-efi-text-primary cursor-pointer"
                                      >
                                        Cancel
                                      </button>
                                    </div>
                                  </div>
                                ) : (
                                  <div className="flex items-start gap-1">
                                    <div className="flex-1 min-w-0">
                                      <span
                                        className="text-efi-text-primary cursor-pointer hover:text-efi-gold-light transition-colors"
                                        onClick={() =>
                                          setEditingTask({
                                            id: task.id,
                                            title: task.title,
                                            description: task.description ?? '',
                                          })
                                        }
                                      >
                                        {task.title}
                                      </span>
                                      {task.description && (
                                        <p className="text-xs text-efi-text-secondary mt-0.5 line-clamp-2">
                                          <Linkify text={task.description} />
                                        </p>
                                      )}
                                    </div>
                                    {pendingDelete?.id === task.id ? (
                                      <span className="flex items-center gap-1 text-xs shrink-0">
                                        <span className="text-efi-text-tertiary">Delete?</span>
                                        <button
                                          type="button"
                                          onClick={confirmDelete}
                                          className="text-efi-error hover:text-red-400 cursor-pointer rounded focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                                        >
                                          Yes
                                        </button>
                                        <button
                                          type="button"
                                          onClick={() => setPendingDelete(null)}
                                          className="text-efi-text-secondary hover:text-efi-text-primary cursor-pointer rounded focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                                        >
                                          No
                                        </button>
                                      </span>
                                    ) : (
                                      <button
                                        type="button"
                                        onClick={() =>
                                          setPendingDelete({
                                            type: 'task',
                                            id: task.id,
                                            name: task.title,
                                          })
                                        }
                                        title="Delete task"
                                        className="shrink-0 p-2 text-efi-text-tertiary hover:text-efi-error transition-colors cursor-pointer rounded hover:bg-white/5 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                                      >
                                        <Trash2 className="w-3.5 h-3.5" />
                                      </button>
                                    )}
                                  </div>
                                )}
                              </td>
                              {participantNames.map((name) => {
                                const est = task.estimates.find(
                                  (e) => e.participantNickname === name,
                                );
                                const isQuestion = est?.storyPoints === '?';
                                return (
                                  <td
                                    key={name}
                                    className={`text-center py-2 px-2 font-medium ${
                                      isQuestion
                                        ? 'text-efi-warning bg-efi-warning/10'
                                        : 'text-efi-gold-light'
                                    }`}
                                  >
                                    {est?.storyPoints ?? '-'}
                                  </td>
                                );
                              })}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    ) : (
                      <div className="text-center py-8">
                        <p className="text-efi-text-secondary">No tasks in this room</p>
                        {room.status !== 'REVEALED' && room.status !== 'CLOSED' && (
                          <p className="text-sm text-efi-text-tertiary mt-1">
                            Add tasks above or import from a list.
                          </p>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center justify-center h-48">
                <p className="text-efi-text-secondary">
                  {roomList.length
                    ? 'Select a room to view estimates'
                    : 'Create a room to get started'}
                </p>
              </div>
            )}
          </div>

          {/* Right: participants sidebar */}
          <div className="lg:col-span-1">
            <aside className="lg:sticky lg:top-[var(--nav-total-height)] lg:self-start">
              <div className="glass-whisper rounded-2xl p-4 border border-efi-gold-light/10">
                <h2 className="text-sm font-semibold text-efi-text-primary mb-3">
                  Participants
                  {activeParticipants.length > 0 ? ` (${activeParticipants.length})` : ''}
                </h2>
                {activeParticipants.length > 0 ? (
                  <div className="space-y-2">
                    {activeParticipants.map((p) => {
                      const hasVoted = (room?.tasks ?? []).some((t) =>
                        t.estimates.some((e) => e.participantId === p.id),
                      );
                      return (
                        <div
                          key={p.id}
                          className="flex items-center gap-2 bg-efi-well rounded-lg px-3 py-2 border border-efi-gold-light/10"
                        >
                          <span
                            className={`text-xs shrink-0 ${hasVoted ? 'text-efi-success' : 'text-efi-text-tertiary'}`}
                          >
                            {hasVoted ? '✓' : '○'}
                          </span>
                          <span className="text-sm text-efi-text-primary flex-1 min-w-0 truncate">
                            {p.nickname}
                          </span>
                          <button
                            type="button"
                            onClick={() => void handleCopyLink(p.id)}
                            title="Copy magic link"
                            className="shrink-0 p-2 text-efi-text-tertiary hover:text-efi-gold-light transition-colors cursor-pointer rounded hover:bg-white/5 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                          >
                            <Copy className="w-3.5 h-3.5" />
                          </button>
                          <button
                            type="button"
                            onClick={() =>
                              setParticipantRemoveModal({
                                id: p.id,
                                nickname: p.nickname,
                              })
                            }
                            title="Remove participant"
                            className="shrink-0 p-2 text-efi-text-tertiary hover:text-efi-error transition-colors cursor-pointer rounded hover:bg-white/5 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                          >
                            <X className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-xs text-efi-text-tertiary">
                    No participants yet. Share the join link to invite your team.
                  </p>
                )}

                {archivedParticipants.length > 0 && (
                  <div className="mt-3 pt-3 border-t border-efi-gold-light/10">
                    <button
                      type="button"
                      onClick={() => setShowArchivedParticipants(!showArchivedParticipants)}
                      className="flex items-center gap-1 text-xs text-efi-text-tertiary hover:text-efi-text-secondary cursor-pointer w-full"
                    >
                      <Archive className="w-3 h-3" />
                      <span>Archived ({archivedParticipants.length})</span>
                      <span className="ml-auto">{showArchivedParticipants ? '▼' : '▶'}</span>
                    </button>
                    {showArchivedParticipants && (
                      <div className="space-y-2 mt-2">
                        {archivedParticipants.map((p) => (
                          <div
                            key={p.id}
                            className="flex items-center gap-2 bg-efi-well/50 rounded-lg px-3 py-2 border border-efi-gold-light/5"
                          >
                            <span className="text-sm text-efi-text-tertiary italic flex-1 min-w-0 truncate">
                              {p.nickname}
                            </span>
                            <button
                              type="button"
                              onClick={() => void handleUnarchiveParticipant(p.id)}
                              title="Unarchive participant"
                              className="shrink-0 p-2 text-efi-text-tertiary hover:text-efi-gold-light transition-colors cursor-pointer rounded hover:bg-white/5 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                            >
                              <RotateCcw className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </div>
            </aside>
          </div>
        </div>

        {/* Participant Removal Modal */}
        <Modal
          isOpen={!!participantRemoveModal}
          onClose={() => setParticipantRemoveModal(null)}
          title={`Remove participant: ${participantRemoveModal?.nickname ?? ''}`}
          maxWidth="max-w-sm"
        >
          <div className="space-y-3">
            <button
              type="button"
              onClick={() => {
                const id = participantRemoveModal?.id;
                setParticipantRemoveModal(null);
                if (id) void handleArchiveParticipant(id);
              }}
              className="w-full text-left rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 hover:border-efi-gold-light/40 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
            >
              <div className="flex items-center gap-2 text-sm font-medium text-efi-text-primary">
                <Archive className="w-4 h-4" />
                Archive
              </div>
              <p className="text-xs text-efi-text-tertiary mt-1">
                Hide from active view. Votes and comments are preserved.
              </p>
            </button>
            <button
              type="button"
              onClick={() => {
                const id = participantRemoveModal?.id;
                setParticipantRemoveModal(null);
                if (id) void handleDeleteParticipant(id);
              }}
              className="w-full text-left rounded-lg bg-efi-well border border-efi-error/20 px-4 py-3 hover:border-efi-error/40 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
            >
              <div className="flex items-center gap-2 text-sm font-medium text-efi-error">
                <Trash2 className="w-4 h-4" />
                Delete permanently
              </div>
              <p className="text-xs text-efi-text-tertiary mt-1">
                Remove participant AND all their votes and comments from all rooms.
              </p>
            </button>
            <button
              type="button"
              onClick={() => setParticipantRemoveModal(null)}
              className="w-full text-center rounded-lg px-4 py-2 text-sm text-efi-text-secondary hover:text-efi-text-primary transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
            >
              Cancel
            </button>
          </div>
        </Modal>

        {/* New Room Modal */}
        {showForm && (
          <div
            className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-[fade-in_0.2s_ease-out]"
            onClick={(e) => {
              if (e.target === e.currentTarget) setShowForm(false);
            }}
            onKeyDown={(e) => {
              if (e.key === 'Escape') setShowForm(false);
            }}
          >
            <div className="glass-crystal rounded-xl sm:rounded-2xl p-4 sm:p-6 w-full max-w-lg animate-[fade-in-scale_0.2s_ease-out]">
              <h2 className="text-lg font-semibold text-efi-text-primary mb-4">New Room</h2>
              <form onSubmit={(e) => void handleCreateRoom(e)} className="space-y-3">
                <div className="flex items-center gap-1">
                  <TextInput
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Sprint Backlog Review"
                    maxLength={255}
                    className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-obsidian"
                  />
                  <RandomNameButton onGenerate={setTitle} generator={generateRoomName} />
                </div>
                <TextArea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Estimate remaining backlog items"
                  rows={2}
                  className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold resize-none focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-obsidian"
                />
                <div>
                  <label className="block text-xs text-efi-text-secondary mb-1.5">Room Type</label>
                  <div className="flex gap-2">
                    <div className="flex-1 flex flex-col">
                      <button
                        type="button"
                        onClick={() => {
                          setRoomType('LIVE');
                          setCommentTemplate('');
                        }}
                        className={`w-full px-3 py-2 rounded-lg text-sm font-medium border transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none ${
                          roomType === 'LIVE'
                            ? roomTypeBadge('LIVE')
                            : 'border-white/12 text-efi-text-secondary hover:text-efi-text-primary hover:border-white/20'
                        }`}
                      >
                        Live
                      </button>
                      <p className="text-[11px] text-efi-text-tertiary mt-1 text-center">
                        Real-time session - team votes together, results revealed after each round
                      </p>
                    </div>
                    <div className="flex-1 flex flex-col">
                      <button
                        type="button"
                        onClick={() => {
                          setRoomType('ASYNC');
                          setCommentTemplate(DEFAULT_COMMENT_TEMPLATE);
                        }}
                        className={`w-full px-3 py-2 rounded-lg text-sm font-medium border transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none ${
                          roomType === 'ASYNC'
                            ? 'bg-efi-info/20 border-efi-info/40 text-efi-info'
                            : 'border-white/12 text-efi-text-secondary hover:text-efi-text-primary hover:border-white/20'
                        }`}
                      >
                        Async
                      </button>
                      <p className="text-[11px] text-efi-text-tertiary mt-1 text-center">
                        Deadline-based - team votes independently before a set deadline
                      </p>
                    </div>
                  </div>
                </div>
                {roomType === 'ASYNC' && (
                  <>
                    <DeadlineInput value={deadline} onChange={setDeadline} />
                    <label className="flex items-center gap-2 cursor-pointer select-none">
                      <input
                        type="checkbox"
                        checked={autoRevealOnDeadline}
                        onChange={(e) => setAutoRevealOnDeadline(e.target.checked)}
                        className="w-4 h-4 accent-efi-gold cursor-pointer text-base"
                      />
                      <span className="text-sm text-efi-text-secondary">
                        Auto-reveal when deadline passes
                      </span>
                    </label>
                  </>
                )}
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <label className="text-xs text-efi-text-secondary">
                      Comment Template (optional)
                    </label>
                    <div className="flex gap-2">
                      {commentTemplate && (
                        <button
                          type="button"
                          onClick={() => setCommentTemplate('')}
                          className="text-[11px] text-efi-text-tertiary hover:text-red-400 transition-colors cursor-pointer"
                        >
                          Clear
                        </button>
                      )}
                      {commentTemplate !== DEFAULT_COMMENT_TEMPLATE && (
                        <button
                          type="button"
                          onClick={() => setCommentTemplate(DEFAULT_COMMENT_TEMPLATE)}
                          className="text-[11px] text-efi-text-tertiary hover:text-efi-gold-light transition-colors cursor-pointer"
                        >
                          Restore default
                        </button>
                      )}
                    </div>
                  </div>
                  <TextArea
                    value={commentTemplate}
                    onChange={(e) => setCommentTemplate(e.target.value)}
                    placeholder="Paste your team's comment template..."
                    maxLength={2000}
                    rows={3}
                    className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold resize-y max-h-48 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
                  />
                </div>
                <label className="flex items-center gap-2 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={commentRequired}
                    onChange={(e) => setCommentRequired(e.target.checked)}
                    className="w-4 h-4 accent-efi-gold cursor-pointer text-base"
                  />
                  <span className="text-sm text-efi-text-secondary">
                    Require comments when voting
                  </span>
                </label>
                <div className="flex justify-end gap-3 pt-1">
                  <button
                    type="button"
                    onClick={() => setShowForm(false)}
                    className="px-4 py-2 rounded-lg text-sm text-efi-text-secondary hover:text-efi-text-primary transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={
                      createRoom.isPending || !title.trim() || (roomType === 'ASYNC' && !deadline)
                    }
                    className="px-4 py-2 rounded-lg text-sm font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-2 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                  >
                    {createRoom.isPending ? (
                      <>
                        <ButtonSpinner /> Creating...
                      </>
                    ) : (
                      'Create'
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        <ImportModal
          isOpen={showImport}
          onClose={() => setShowImport(false)}
          onImport={(titles) => void handleImportTasks(titles)}
        />

        <FinishSessionDialog
          isOpen={showFinishDialog}
          isPending={finishSession.isPending}
          onFinish={(revealVotes) => void handleFinishSession(revealVotes)}
          onCancel={() => {
            setShowFinishDialog(false);
            setFinishRoomId(null);
          }}
          autoAssigned={autoAssigned}
          onDismissAutoAssigned={() => {
            setAutoAssigned(null);
            setShowFinishDialog(false);
            setFinishRoomId(null);
          }}
        />
      </div>
    );
  }

  // --- Non-admin (participant) view ---
  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-efi-text-primary">
            <span className="text-sm font-normal text-efi-text-secondary mr-1">Project:</span>
            {project?.name}
          </h1>
          <p className="text-sm text-efi-text-secondary mt-1">
            {roomList.length} {roomList.length === 1 ? 'room' : 'rooms'}
          </p>
        </div>

        <div className="flex gap-3 mt-3 sm:mt-0 items-center flex-wrap">
          {slug && (
            <button
              type="button"
              onClick={() => void handleCopyJoinLink()}
              title="Copy project join link"
              className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-efi-text-secondary border border-white/10 rounded-lg hover:text-efi-text-primary hover:border-white/20 transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
            >
              <Copy className="w-3 h-3" />
              Share
            </button>
          )}
          {slug && roomList.some((r) => r.status === 'REVEALED' || r.status === 'CLOSED') && (
            <Link to={`/p/${slug}/analytics`} className={ghostLinkBtn}>
              Analytics
            </Link>
          )}
          {!auth.guestToken && !auth.nickname && !getJwt() && slug && (
            <Link
              to={`/p/${slug}/join`}
              className="px-4 py-2 rounded-lg text-sm font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 transition-colors no-underline active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
            >
              Join Project
            </Link>
          )}
        </div>
      </div>

      <div className="space-y-3">
        {roomList.map((r) => (
          <div
            key={r.id}
            role="link"
            tabIndex={0}
            onClick={() => void navigate(`/p/${slug}/r/${r.id}`)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') void navigate(`/p/${slug}/r/${r.id}`);
            }}
            className="block glass-frost rounded-xl p-4 hover:border-efi-gold/30 active:scale-[0.995] transition-all duration-150 cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-efi-text-primary font-medium">{r.title}</h3>
                  <span className="text-xs font-mono text-efi-text-secondary bg-white/8 px-1.5 py-0.5 rounded">
                    {r.slug}
                  </span>
                  <ShareButton roomSlug={r.slug} />
                  <span
                    className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${roomTypeBadge(r.roomType)}`}
                  >
                    {r.roomType === 'LIVE' ? 'Live' : 'Async'}
                  </span>
                </div>
                {r.description && (
                  <p className="text-sm text-efi-text-secondary mt-1 line-clamp-2">
                    {r.description}
                  </p>
                )}
                {r.roomType === 'ASYNC' && r.deadline && (
                  <div className="flex items-center gap-3 mt-2 text-xs text-efi-text-secondary">
                    <CountdownTimer deadline={r.deadline} />
                  </div>
                )}
              </div>
              <div className="flex items-center gap-2 mt-2 sm:mt-0">
                <span className={`text-xs px-3 py-1 rounded-full border ${statusBadge(r.status)}`}>
                  {statusLabel(r.status)}
                </span>
              </div>
            </div>
          </div>
        ))}

        {roomList.length === 0 && (
          <div className="text-center py-16 bg-white/3 rounded-2xl border border-dashed border-white/8">
            <span className="text-4xl mb-4 block">&#127183;</span>
            <p className="text-efi-text-secondary">No rooms yet.</p>
          </div>
        )}
      </div>
    </div>
  );
}
