import { useQuery } from '@tanstack/react-query';
import { Copy, Eye } from 'lucide-react';
import { useCallback, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError, getAuth } from '@/api/client';
import { roomApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import { SummaryCard } from '@/components/charts/SummaryCard';
import { MarkdownPreviewModal } from '@/components/MarkdownPreviewModal';
import { NotFoundState } from '@/components/NotFoundState';
import { PageSpinner } from '@/components/PageSpinner';
import {
  getConsensusLevel,
  getTaskSp,
  ResultsTable,
  type TaskEstimate,
} from '@/components/ResultsTable';
import { useToast } from '@/components/Toast';
import { TraceCopyButton } from '@/components/TraceCopyButton';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { ghostIconBtn, ghostLinkBtn, outlineBtn } from '@/styles/buttons';
import { getErrorMessage } from '@/utils/error';
import { formatResultsAsMarkdown } from '@/utils/markdown';

export function ResultsPage() {
  const { slug, roomId } = useParams<{ slug: string; roomId: string }>();
  const projectName = slug ? (getAuth(slug).projectName ?? slug) : (slug ?? '');
  const { showToast } = useToast();

  const {
    data: results,
    isLoading,
    error,
  } = useQuery({
    queryKey: queryKeys.rooms.results(roomId!),
    queryFn: () => roomApi.results(roomId!, slug!),
    enabled: Boolean(roomId && slug),
    refetchInterval: (query) => {
      const status = query.state.data?.status;
      if (status === 'CLOSED') return false;
      if (query.state.status === 'error') return false;
      return 10_000;
    },
  });

  useDocumentTitle('Results', results?.title, projectName);

  const participantNames = useMemo(
    () =>
      Array.from(
        new Set(
          (results?.tasks ?? []).flatMap((task) =>
            task.estimates.map((e) => e.participantNickname),
          ),
        ),
      ),
    [results],
  );

  const tableData = useMemo<TaskEstimate[]>(
    () =>
      (results?.tasks ?? []).map((task) => {
        const estimates: Record<string, number | string> = {};
        const comments: Record<string, string> = {};
        for (const est of task.estimates) {
          if (!est.storyPoints) continue;
          const numeric = Number(est.storyPoints);
          estimates[est.participantNickname] = Number.isNaN(numeric) ? est.storyPoints : numeric;
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
      }),
    [results],
  );

  const [previewOpen, setPreviewOpen] = useState(false);

  const markdown = useMemo(
    () => formatResultsAsMarkdown(results?.title ?? '', tableData, participantNames),
    [results?.title, tableData, participantNames],
  );

  const handleCopyMarkdown = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(markdown);
      showToast('Results copied as Markdown!', 'success');
    } catch (err) {
      showToast(getErrorMessage(err));
    }
  }, [markdown, showToast]);

  if (isLoading && !results) {
    return <PageSpinner />;
  }

  if (error) {
    if (error instanceof ApiError && error.status === 404) {
      return (
        <NotFoundState
          message="Room not found"
          backTo={slug ? `/p/${slug}` : '/'}
          backLabel={slug ? 'Back to Project' : 'Back to Projects'}
        />
      );
    }
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <p className="text-efi-error">{getErrorMessage(error)}</p>
        {error instanceof ApiError && error.traceId && <TraceCopyButton traceId={error.traceId} />}
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8 animate-[fade-in-up_0.6s_ease-out] motion-reduce:animate-none">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-efi-text-primary">Results</h1>
          <div className="flex items-center gap-2 mt-1">
            <p className="text-sm text-efi-text-secondary">{results?.title}</p>
            {results?.slug && (
              <span className="text-xs font-mono text-efi-text-tertiary bg-white/5 px-1.5 py-0.5 rounded">
                {results.slug}
              </span>
            )}
          </div>
        </div>
        <div className="mt-3 sm:mt-0 flex gap-2">
          {tableData.length > 0 && (
            <>
              <button type="button" onClick={() => setPreviewOpen(true)} className={ghostIconBtn}>
                <Eye className="w-4 h-4" />
                Preview
              </button>
              <button type="button" onClick={handleCopyMarkdown} className={ghostIconBtn}>
                <Copy className="w-4 h-4" />
                Copy Markdown
              </button>
            </>
          )}
          <Link to={`/p/${slug}/r/${roomId}/analytics`} className={ghostLinkBtn}>
            Analytics
          </Link>
          <Link to={`/p/${slug}/r/${roomId}`} className={`${outlineBtn} no-underline`}>
            Back to Room
          </Link>
        </div>
      </div>

      <div className="glass-frost rounded-xl sm:rounded-2xl p-3 sm:p-6">
        <ResultsTable tasks={tableData} participants={participantNames} />
      </div>

      {/* Summary cards */}
      {tableData.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4 mt-8">
          <SummaryCard label="Total Tasks" value={tableData.length} />
          <SummaryCard
            label="Total SP"
            value={tableData.reduce((sum, t) => sum + getTaskSp(t), 0)}
          />
          <SummaryCard
            label="Consensus"
            value={`${tableData.filter((t) => getConsensusLevel(t.estimates) === 'consensus').length}/${tableData.length}`}
          />
        </div>
      )}
      <MarkdownPreviewModal
        isOpen={previewOpen}
        onClose={() => setPreviewOpen(false)}
        markdown={markdown}
        onCopy={handleCopyMarkdown}
      />
    </div>
  );
}
