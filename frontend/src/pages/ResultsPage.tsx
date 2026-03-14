import { useParams, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { getAuth, ApiError } from '@/api/client';
import { queryKeys } from '@/api/queryKeys';
import { roomApi } from '@/api/queries';
import { getErrorMessage } from '@/utils/error';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { Spinner } from '@/components/Spinner';
import { ResultsTable, getConsensusLevel, type TaskEstimate } from '@/components/ResultsTable';

function getTaskSp(task: TaskEstimate): number {
  if (task.finalEstimate) {
    const n = Number(task.finalEstimate);
    return isNaN(n) ? 0 : n;
  }
  return task.median ?? 0;
}

export function ResultsPage() {
  const { slug, roomId } = useParams<{ slug: string; roomId: string }>();
  const projectName = slug ? getAuth(slug).projectName ?? slug : slug ?? '';

  const { data: results, isLoading, error } = useQuery({
    queryKey: queryKeys.rooms.results(roomId!),
    queryFn: () => roomApi.results(roomId!, slug!),
    enabled: Boolean(roomId && slug),
    refetchInterval: (query) => (query.state.status === 'error' ? false : 10_000),
  });

  useDocumentTitle('Results', results?.title, projectName);

  if (isLoading && !results) {
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
            to={slug ? `/p/${slug}` : '/'}
            className="text-sm text-efi-gold-light hover:text-efi-gold transition-colors no-underline hover:underline"
          >
            {slug ? 'Back to Project' : 'Back to Projects'}
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

  // Derive unique participant names from all estimates
  const participantNames = Array.from(
    new Set(
      (results?.tasks ?? []).flatMap((task) => task.estimates.map((e) => e.participantNickname)),
    ),
  );

  // Transform API data to ResultsTable format
  const tableData: TaskEstimate[] = (results?.tasks ?? []).map((task) => {
    const estimates: Record<string, number | string> = {};
    const comments: Record<string, string> = {};
    for (const est of task.estimates) {
      const numeric = Number(est.storyPoints);
      estimates[est.participantNickname] = isNaN(numeric) ? est.storyPoints : numeric;
      if (est.comment) {
        comments[est.participantNickname] = est.comment;
      }
    }
    return {
      taskId: task.taskId,
      taskTitle: task.title,
      estimates,
      comments,
      average: task.averagePoints,
      median: task.medianPoints,
      finalEstimate: task.finalEstimate,
    };
  });

  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8 animate-[fade-in-up_0.6s_ease-out] motion-reduce:animate-none">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-efi-text-primary">Results</h1>
          <div className="flex items-center gap-2 mt-1">
            <p className="text-sm text-efi-text-secondary">{results?.title}</p>
            {results?.slug && <span className="text-xs font-mono text-efi-text-tertiary bg-white/5 px-1.5 py-0.5 rounded">{results.slug}</span>}
          </div>
        </div>
        <div className="mt-3 sm:mt-0 flex gap-2">
          <Link
            to={`/p/${slug}/r/${roomId}/analytics`}
            className="px-3 py-1.5 text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold rounded-lg transition-colors no-underline active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none cursor-pointer"
          >
            Analytics
          </Link>
          <Link
            to={`/p/${slug}/r/${roomId}`}
            className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold hover:text-efi-text-primary transition-colors no-underline active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none cursor-pointer"
          >
            Back to Room
          </Link>
        </div>
      </div>

      <div className="glass-frost rounded-xl sm:rounded-2xl p-3 sm:p-6">
        <ResultsTable
          tasks={tableData}
          participants={participantNames}
        />
      </div>

      {/* Summary cards */}
      {tableData.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4 mt-8">
          <div className="glass-whisper rounded-xl p-4 text-center">
            <p className="text-sm text-efi-text-secondary">Total Tasks</p>
            <p className="text-2xl font-bold text-efi-text-primary mt-1">{tableData.length}</p>
          </div>
          <div className="glass-whisper rounded-xl p-4 text-center">
            <p className="text-sm text-efi-text-secondary">Total SP</p>
            <p className="text-2xl font-bold text-efi-gold-light mt-1">
              {tableData.reduce((sum, t) => sum + getTaskSp(t), 0)}
            </p>
          </div>
          <div className="glass-whisper rounded-xl p-4 text-center">
            <p className="text-sm text-efi-text-secondary">Consensus</p>
            <p className="text-2xl font-bold text-efi-success mt-1">
              {tableData.filter((t) => getConsensusLevel(t.estimates) === 'consensus').length}/{tableData.length}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
