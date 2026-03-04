import { useState, useEffect } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAuth, getJwt, saveAuth, removeProject, ApiError } from '@/api/client';
import { queryKeys } from '@/api/queryKeys';
import { projectApi, roomApi } from '@/api/queries';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import {
  useCreateRoom,
  useRevealRoom,
  useReopenRoom,
  useUpdateRoom,
  useUpdateProject,
  useDeleteProject,
  useAddTask,
  useImportTasks,
  useUpdateTask,
  useDeleteTask,
  useDeleteParticipant,
} from '@/api/mutations';
import { logger } from '@/utils/logger';
import { getErrorMessage } from '@/utils/error';
import { useToast } from '@/components/Toast';
import { Spinner, ButtonSpinner } from '@/components/Spinner';
import { CountdownTimer } from '@/components/CountdownTimer';
import { DeadlineInput, getDefaultDeadline, formatPreview } from '@/components/DeadlineInput';
import { ImportModal } from '@/components/ImportModal';
import { AddTaskForm } from '@/components/AddTaskForm';
import { Copy, Trash2, Plus, X } from 'lucide-react';
import { RandomNameButton } from '@/components/RandomNameButton';
import { generateRoomName } from '@/utils/nameGenerator';
import { Linkify } from '@/lib/linkify';
import { statusBadge, roomTypeBadge, statusLabel } from '@/utils/roomBadges';
import { TextInput, TextArea } from '@/components/TextInput';
import type { RoomType } from '@/api/types';

export function ProjectPage() {
  const { slug } = useParams<{ slug: string }>();
  const auth = slug ? getAuth(slug) : {};
  const [isAdmin, setIsAdmin] = useState(Boolean(auth.adminCode));
  const jwt = getJwt();
  const { showToast } = useToast();
  useDocumentTitle(auth.projectName ?? slug);

  // Admin-only state
  const [selectedRoomId, setSelectedRoomId] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const [showImport, setShowImport] = useState(false);
  const [editingDeadline, setEditingDeadline] = useState<string | null>(null);
  const [editingTask, setEditingTask] = useState<{ id: string; title: string; description: string } | null>(null);
  const [pendingDelete, setPendingDelete] = useState<{ type: 'task' | 'participant'; id: string; name: string } | null>(null);
  const [editingName, setEditingName] = useState<string | null>(null);
  const [pendingDeleteProject, setPendingDeleteProject] = useState(false);

  // Shared create room state
  const [showForm, setShowForm] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [deadline, setDeadline] = useState(getDefaultDeadline(3));
  const [roomType, setRoomType] = useState<RoomType>('LIVE');

  // Queries
  const { data: project, isLoading: loading, error } = useQuery({
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

  // Fallback: logged-in owner without adminCode in localStorage
  const { data: adminProject } = useQuery({
    queryKey: queryKeys.projects.admin(slug!),
    queryFn: () => projectApi.admin(slug!),
    enabled: Boolean(slug && jwt && !auth.adminCode) && !error,
    retry: false,
  });

  useEffect(() => {
    if (adminProject?.adminCode && slug) {
      saveAuth(slug, { adminCode: adminProject.adminCode, projectName: adminProject.name });
      setIsAdmin(true);
    }
  }, [adminProject, slug]);

  // Mutations
  const createRoom = useCreateRoom(slug ?? '');
  const revealRoom = useRevealRoom(slug ?? '');
  const reopenRoom = useReopenRoom(slug ?? '');
  const updateRoom = useUpdateRoom(slug ?? '');
  const addTask = useAddTask(slug ?? '');
  const importTasks = useImportTasks(slug ?? '');
  const updateTask = useUpdateTask(slug ?? '');
  const deleteTask = useDeleteTask(slug ?? '');
  const deleteParticipant = useDeleteParticipant(slug ?? '');
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
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Spinner />
      </div>
    );
  }

  if (error) {
    if (error instanceof ApiError && error.status === 404) {
      if (slug) removeProject(slug);
      return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
          <p className="text-efi-text-primary font-medium">Project not found</p>
          <Link to="/" className="text-sm text-efi-gold-light hover:text-efi-gold transition-colors no-underline hover:underline">
            Back to Projects
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
      });
      setTitle('');
      setDescription('');
      setDeadline(getDefaultDeadline(3));
      setRoomType('LIVE');
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
      await addTask.mutateAsync({ roomId: selectedRoomId, title: taskTitle, description: taskDescription });
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

  async function handleSaveDeadline() {
    if (!editingDeadline || !selectedRoomId) return;
    try {
      await updateRoom.mutateAsync({ roomId: selectedRoomId, body: { deadline: new Date(editingDeadline).toISOString() } });
      setEditingDeadline(null);
    } catch (err) {
      logger.warn('Failed to update deadline:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleSaveTask() {
    if (!editingTask) return;
    try {
      await updateTask.mutateAsync({ taskId: editingTask.id, body: { title: editingTask.title.trim(), description: editingTask.description.trim() || undefined } });
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

  async function handleCopyRoomLink(roomSlug: string) {
    const link = `${window.location.origin}/r/${roomSlug}`;
    try {
      await navigator.clipboard.writeText(link);
      showToast('Room link copied!');
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

  function confirmDelete() {
    if (!pendingDelete) return;
    const { type, id } = pendingDelete;
    setPendingDelete(null);
    if (type === 'task') void handleDeleteTask(id);
    else void handleDeleteParticipant(id);
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
    setPendingDeleteProject(false);
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
                onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setEditingName(project?.name ?? ''); } }}
                role="button"
                tabIndex={0}
                title="Click to edit project name"
              >
                <span className="text-sm font-normal text-efi-text-secondary mr-1">Project:</span>
                {project?.name}
              </h1>
            )}
            <div className="flex items-center gap-3 mt-1">
              <p className="text-sm text-efi-text-secondary">{roomList.length} {roomList.length === 1 ? 'room' : 'rooms'}</p>
            </div>
          </div>
          <div className="mt-3 sm:mt-0 flex gap-3">
            <button
              type="button"
              onClick={() => setShowForm(true)}
              className="px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 transition-opacity cursor-pointer active:scale-[0.98] flex items-center gap-1 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
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
            {pendingDeleteProject ? (
              <span className="flex items-center gap-2 text-sm">
                <span className="text-efi-text-tertiary">Delete project?</span>
                <button
                  type="button"
                  onClick={() => void handleDeleteProject()}
                  disabled={deleteProjectMutation.isPending}
                  className="text-efi-error hover:text-red-400 font-medium cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none rounded"
                >
                  {deleteProjectMutation.isPending ? <ButtonSpinner /> : 'Yes'}
                </button>
                <button
                  type="button"
                  onClick={() => setPendingDeleteProject(false)}
                  className="text-efi-text-secondary hover:text-efi-text-primary cursor-pointer transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none rounded"
                >
                  No
                </button>
              </span>
            ) : (
              <button
                type="button"
                onClick={() => setPendingDeleteProject(true)}
                title="Delete project"
                className="p-2 rounded-lg text-efi-text-tertiary hover:text-red-400 transition-colors cursor-pointer hover:bg-white/5 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>

        {/* Main grid */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Left: rooms + room detail */}
          <div className="lg:col-span-3 space-y-6">
            {/* Room list header */}
            <div>
              <h2 className="text-sm font-semibold text-efi-text-secondary uppercase tracking-wider mb-3">Rooms</h2>

              {roomList.length > 0 ? (
                <div className="flex flex-col gap-1">
                  {roomList.map((r) => (
                    <div key={r.id} className="flex items-center gap-1">
                      <button
                        onClick={() => { setSelectedRoomId(r.id); setEditingDeadline(null); setPendingDelete(null); }}
                        className={`flex-1 text-left px-3 py-2.5 rounded-lg text-sm transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none flex items-center justify-between gap-2 ${selectedRoomId === r.id
                          ? 'bg-efi-gold/15 border border-efi-gold/30 text-efi-text-primary'
                          : 'bg-white/[0.03] border border-white/6 text-efi-text-primary hover:bg-white/6 hover:border-white/10'
                          }`}
                      >
                        <span className="font-medium truncate">{r.title}</span>
                        <div className="flex items-center gap-1.5 shrink-0">
                          <span className="text-[10px] font-mono text-efi-text-secondary bg-white/8 px-1.5 py-0.5 rounded">{r.slug}</span>
                          <span className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${roomTypeBadge(r.roomType)}`}>
                            {r.roomType === 'LIVE' ? 'Live' : 'Async'}
                          </span>
                          <span className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${statusBadge(r.status)}`}>{statusLabel(r.status)}</span>
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
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-6">
                  <div className="flex items-center gap-2">
                    <h2 className="text-lg font-semibold text-efi-text-primary">{room.title}</h2>
                    <span className="text-xs font-mono text-efi-text-secondary bg-white/8 px-1.5 py-0.5 rounded">{room.slug}</span>
                    <span className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${roomTypeBadge(room.roomType)}`}>
                      {isRoomLive ? 'Live' : 'Async'}
                    </span>
                  </div>
                  <div className="flex flex-wrap gap-2 mt-3 sm:mt-0">
                    {room.status !== 'REVEALED' && room.status !== 'CLOSED' && !isRoomLive && (
                      <button
                        onClick={() => void handleReveal(room.id)}
                        disabled={revealRoom.isPending}
                        className="px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-2 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                      >
                        {revealRoom.isPending ? <><ButtonSpinner /> Revealing...</> : 'Reveal All'}
                      </button>
                    )}
                    {(room.status === 'REVEALED' || room.status === 'CLOSED') && (
                      <button
                        onClick={() => void handleReopen(room.id)}
                        disabled={reopenRoom.isPending}
                        className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-2 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                      >
                        {reopenRoom.isPending ? <><ButtonSpinner /> Reopening...</> : 'Reopen'}
                      </button>
                    )}
                    {!isRoomLive && room.status !== 'REVEALED' && room.status !== 'CLOSED' && (
                      <button
                        onClick={() => setShowImport(true)}
                        className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold hover:text-efi-text-primary transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-2 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                      >
                        Import
                      </button>
                    )}
                    {!isRoomLive && (
                      <button
                        onClick={() => void handleExportCsv(room.id)}
                        disabled={exporting}
                        className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold hover:text-efi-text-primary disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-2 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                      >
                        {exporting ? <><ButtonSpinner /> Exporting...</> : 'Export CSV'}
                      </button>
                    )}
                    <Link
                      to={`/p/${slug}/r/${room.id}`}
                      className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold hover:text-efi-text-primary transition-colors no-underline active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                    >
                      Open Voting
                    </Link>
                    {!isRoomLive && (room.status === 'REVEALED' || room.status === 'CLOSED') && (
                      <Link
                        to={`/p/${slug}/r/${room.id}/results`}
                        className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold hover:text-efi-text-primary transition-colors no-underline active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                      >
                        View Results
                      </Link>
                    )}
                  </div>
                </div>

                {/* Share link */}
                <div className="mb-4">
                  <button
                    type="button"
                    onClick={() => void handleCopyRoomLink(room.slug)}
                    title="Copy room link"
                    className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-efi-text-secondary border border-white/10 rounded-lg hover:text-efi-text-primary hover:border-white/20 transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                  >
                    <Copy className="w-3 h-3" />
                    Share
                  </button>
                </div>

                {/* Deadline for Async rooms */}
                {!isRoomLive && (
                  <div className="mb-6">
                    {editingDeadline != null ? (
                      <div className="space-y-2">
                        <DeadlineInput value={editingDeadline} onChange={setEditingDeadline} />
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => void handleSaveDeadline()}
                            disabled={updateRoom.isPending || !editingDeadline}
                            className="px-3 py-1 rounded-lg text-xs font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            {updateRoom.isPending ? <><ButtonSpinner /> Saving...</> : 'Save'}
                          </button>
                          <button
                            type="button"
                            onClick={() => setEditingDeadline(null)}
                            className="px-3 py-1 rounded-lg text-xs font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2 text-sm">
                        <span className="text-efi-text-secondary">Deadline:</span>
                        <span className="text-efi-text-primary">{formatPreview(room.deadline)}</span>
                        <CountdownTimer deadline={room.deadline} />
                        <button
                          type="button"
                          onClick={() => {
                            const d = new Date(room.deadline);
                            const pad = (n: number) => String(n).padStart(2, '0');
                            setEditingDeadline(`${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`);
                          }}
                          className="text-xs text-efi-gold-light hover:text-efi-text-primary transition-colors cursor-pointer"
                        >
                          Edit
                        </button>
                      </div>
                    )}
                  </div>
                )}

                {/* Add task controls */}
                {room.status !== 'REVEALED' && room.status !== 'CLOSED' && room.roomType !== 'LIVE' && (
                  <AddTaskForm
                    onAdd={(t, desc) => handleAddTask(t, desc)}
                  />
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
                          <th className="text-left py-2 px-2 text-efi-text-secondary font-medium">Task</th>
                          {participantNames.map((name) => (
                            <th key={name} className="text-center py-2 px-2 text-efi-text-secondary font-medium">
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
                                    onChange={(e) => setEditingTask((prev) => prev && { ...prev, title: e.target.value })}
                                    maxLength={255}
                                    className="w-full rounded bg-efi-well border border-efi-gold-light/20 px-2 py-1 text-base text-efi-text-primary focus:outline-none focus:border-efi-gold"
                                  />
                                  <TextArea
                                    value={editingTask.description}
                                    onChange={(e) => setEditingTask((prev) => prev && { ...prev, description: e.target.value })}
                                    rows={2}
                                    placeholder="Description (optional)"
                                    className="w-full rounded bg-efi-well border border-efi-gold-light/20 px-2 py-1 text-base text-efi-text-secondary placeholder-efi-text-tertiary focus:outline-none focus:border-efi-gold resize-none"
                                  />
                                  <div className="flex gap-1.5">
                                    <button
                                      onClick={() => void handleSaveTask()}
                                      disabled={updateTask.isPending || !editingTask.title.trim()}
                                      className="px-2 py-0.5 rounded text-xs font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 cursor-pointer"
                                    >
                                      {updateTask.isPending ? 'Saving...' : 'Save'}
                                    </button>
                                    <button
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
                                    {room.status !== 'REVEALED' && room.status !== 'CLOSED' ? (
                                      <span
                                        className="text-efi-text-primary cursor-pointer hover:text-efi-gold-light transition-colors"
                                        onClick={() => setEditingTask({ id: task.id, title: task.title, description: task.description ?? '' })}
                                      >
                                        {task.title}
                                      </span>
                                    ) : (
                                      <span className="text-efi-text-primary">{task.title}</span>
                                    )}
                                    {task.description && (
                                      <p className="text-xs text-efi-text-secondary mt-0.5 line-clamp-2">
                                        <Linkify text={task.description} />
                                      </p>
                                    )}
                                  </div>
                                  {pendingDelete?.id === task.id ? (
                                    <span className="flex items-center gap-1 text-xs shrink-0">
                                      <span className="text-efi-text-tertiary">Delete?</span>
                                      <button onClick={confirmDelete} className="text-efi-error hover:text-red-400 cursor-pointer rounded focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none">Yes</button>
                                      <button onClick={() => setPendingDelete(null)} className="text-efi-text-secondary hover:text-efi-text-primary cursor-pointer rounded focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none">No</button>
                                    </span>
                                  ) : (
                                    <button
                                      onClick={() => setPendingDelete({ type: 'task', id: task.id, name: task.title })}
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
                              const est = task.estimates.find((e) => e.participantNickname === name);
                              const isQuestion = est?.storyPoints === '?';
                              return (
                                <td key={name} className={`text-center py-2 px-2 font-medium ${isQuestion ? 'text-efi-warning bg-efi-warning/10' : 'text-efi-gold-light'
                                  }`}>
                                  {est ? est.storyPoints : '-'}
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
                        <p className="text-sm text-efi-text-tertiary mt-1">Add tasks above or import from a list.</p>
                      )}
                    </div>
                  )}
                </div>
                )}
              </div>
            ) : (
              <div className="flex items-center justify-center h-48">
                <p className="text-efi-text-secondary">
                  {roomList.length ? 'Select a room to view estimates' : 'Create a room to get started'}
                </p>
              </div>
            )}
          </div>

          {/* Right: participants sidebar */}
          <div className="lg:col-span-1">
            <aside className="lg:sticky lg:top-[var(--nav-total-height)] lg:self-start">
              <div className="glass-whisper rounded-2xl p-4 border border-efi-gold-light/10">
                <h2 className="text-sm font-semibold text-efi-text-primary mb-3">
                  Participants{participants && participants.length > 0 ? ` (${participants.length})` : ''}
                </h2>
                {participants && participants.length > 0 ? (
                  <div className="space-y-2">
                    {participants.map((p) => {
                      const hasVoted = (room?.tasks ?? []).some((t) =>
                        t.estimates.some((e) => e.participantId === p.id),
                      );
                      return (
                      <div key={p.id} className="flex items-center gap-2 bg-efi-well rounded-lg px-3 py-2 border border-efi-gold-light/10">
                        <span className={`text-xs shrink-0 ${hasVoted ? 'text-efi-success' : 'text-efi-text-tertiary'}`}>
                          {hasVoted ? '✓' : '○'}
                        </span>
                        <span className="text-sm text-efi-text-primary flex-1 min-w-0 truncate">{p.nickname}</span>
                        <button
                          type="button"
                          onClick={() => void handleCopyLink(p.id)}
                          title="Copy magic link"
                          className="shrink-0 p-2 text-efi-text-tertiary hover:text-efi-gold-light transition-colors cursor-pointer rounded hover:bg-white/5 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                        >
                          <Copy className="w-3.5 h-3.5" />
                        </button>
                        {pendingDelete?.id === p.id ? (
                          <span className="flex items-center gap-1 text-xs shrink-0">
                            <span className="text-efi-text-tertiary">Del?</span>
                            <button onClick={confirmDelete} className="text-efi-error hover:text-red-400 cursor-pointer rounded focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none">Yes</button>
                            <button onClick={() => setPendingDelete(null)} className="text-efi-text-secondary hover:text-efi-text-primary cursor-pointer rounded focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none">No</button>
                          </span>
                        ) : (
                          <button
                            type="button"
                            onClick={() => setPendingDelete({ type: 'participant', id: p.id, name: p.nickname })}
                            title="Remove participant"
                            className="shrink-0 p-2 text-efi-text-tertiary hover:text-efi-error transition-colors cursor-pointer rounded hover:bg-white/5 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                          >
                            <X className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                      );
                    })}
                  </div>
                ) : (
                  <p className="text-xs text-efi-text-tertiary">No participants yet. Share the join link to invite your team.</p>
                )}
              </div>
            </aside>
          </div>
        </div>

        {/* New Room Modal */}
        {showForm && (
          <div
            className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-[fade-in_0.2s_ease-out]"
            onClick={(e) => { if (e.target === e.currentTarget) setShowForm(false); }}
            onKeyDown={(e) => { if (e.key === 'Escape') setShowForm(false); }}
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
                        onClick={() => setRoomType('LIVE')}
                        className={`w-full px-3 py-2 rounded-lg text-sm font-medium border transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none ${roomType === 'LIVE'
                          ? roomTypeBadge('LIVE')
                          : 'border-white/12 text-efi-text-secondary hover:text-efi-text-primary hover:border-white/20'
                          }`}
                      >
                        Live
                      </button>
                      <p className="text-[11px] text-efi-text-tertiary mt-1 text-center">Real-time session - team votes together, results revealed after each round</p>
                    </div>
                    <div className="flex-1 flex flex-col">
                      <button
                        type="button"
                        onClick={() => setRoomType('ASYNC')}
                        className={`w-full px-3 py-2 rounded-lg text-sm font-medium border transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none ${roomType === 'ASYNC'
                          ? 'bg-efi-info/20 border-efi-info/40 text-efi-info'
                          : 'border-white/12 text-efi-text-secondary hover:text-efi-text-primary hover:border-white/20'
                          }`}
                      >
                        Async
                      </button>
                      <p className="text-[11px] text-efi-text-tertiary mt-1 text-center">Deadline-based - team votes independently before a set deadline</p>
                    </div>
                  </div>
                </div>
                {roomType === 'ASYNC' && (
                  <DeadlineInput value={deadline} onChange={setDeadline} />
                )}
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
                    disabled={createRoom.isPending || !title.trim() || (roomType === 'ASYNC' && !deadline)}
                    className="px-4 py-2 rounded-lg text-sm font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] flex items-center gap-2 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                  >
                    {createRoom.isPending ? <><ButtonSpinner /> Creating...</> : 'Create'}
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

        <div className="flex gap-3 mt-3 sm:mt-0">
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
          {!auth.participantId && slug && (
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
            onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') void navigate(`/p/${slug}/r/${r.id}`); }}
            className="block glass-frost rounded-xl p-4 hover:border-efi-gold/30 active:scale-[0.995] transition-all duration-150 cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between">
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="text-efi-text-primary font-medium">{r.title}</h3>
                  <span className="text-xs font-mono text-efi-text-secondary bg-white/8 px-1.5 py-0.5 rounded">{r.slug}</span>
                  <button
                    type="button"
                    onClick={(e) => { e.stopPropagation(); void handleCopyRoomLink(r.slug); }}
                    onKeyDown={(e) => e.stopPropagation()}
                    title="Copy room link"
                    className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-efi-text-secondary border border-white/10 rounded-lg hover:text-efi-text-primary hover:border-white/20 transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                  >
                    <Copy className="w-3 h-3" /> Share
                  </button>
                  <span className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${roomTypeBadge(r.roomType)}`}>
                    {r.roomType === 'LIVE' ? 'Live' : 'Async'}
                  </span>
                </div>
                {r.description && (
                  <p className="text-sm text-efi-text-secondary mt-1 line-clamp-2">{r.description}</p>
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
