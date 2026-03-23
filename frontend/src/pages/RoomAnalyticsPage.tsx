import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '@/api/client';
import { analyticsApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import { AvgVsFinalChart } from '@/components/charts/AvgVsFinalChart';
import { ParticipationMatrix } from '@/components/charts/ParticipationMatrix';
import { SummaryCard } from '@/components/charts/SummaryCard';
import { VoteDistributionChart } from '@/components/charts/VoteDistributionChart';
import { NotFoundState } from '@/components/NotFoundState';
import { PageSpinner } from '@/components/PageSpinner';
import { TraceCopyButton } from '@/components/TraceCopyButton';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { getErrorMessage } from '@/utils/error';

export function RoomAnalyticsPage() {
  const { slug, roomId } = useParams<{ slug: string; roomId: string }>();
  const [selectedTaskId, setSelectedTaskId] = useState<string>('');

  const {
    data: analytics,
    isLoading,
    error,
  } = useQuery({
    queryKey: queryKeys.analytics.room(roomId!),
    queryFn: () => analyticsApi.room(roomId!, slug!),
    enabled: Boolean(roomId && slug),
  });

  useDocumentTitle('Analytics', analytics?.title, slug);

  // Set default selected task once data loads
  const tasks = analytics?.taskAnalytics ?? [];
  const effectiveTaskId = selectedTaskId || tasks[0]?.taskId || '';
  const selectedTask = tasks.find((t) => t.taskId === effectiveTaskId);

  if (isLoading && !analytics) {
    return <PageSpinner />;
  }

  if (error) {
    if (error instanceof ApiError && error.status === 404) {
      return (
        <NotFoundState
          message="Room not found"
          backTo={slug ? `/p/${slug}` : '/'}
          backLabel="Back to Project"
        />
      );
    }
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <p className="text-efi-error">{getErrorMessage(error)}</p>
        {error instanceof ApiError && error.traceId && <TraceCopyButton traceId={error.traceId} />}
        <Link
          to={slug && roomId ? `/p/${slug}/r/${roomId}/results` : '/'}
          className="text-sm text-efi-gold-light hover:text-efi-gold transition-colors no-underline hover:underline"
        >
          Back to Results
        </Link>
      </div>
    );
  }

  const summary = analytics?.summary;
  const participationRatePct = summary ? Math.round(summary.participationRate) : 0;

  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between animate-[fade-in-up_0.6s_ease-out] motion-reduce:animate-none">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-efi-text-primary">Room Analytics</h1>
          {analytics?.title && (
            <p className="text-sm text-efi-text-secondary mt-1">{analytics.title}</p>
          )}
        </div>
        <Link
          to={slug && roomId ? `/p/${slug}/r/${roomId}/results` : '/'}
          className="mt-3 sm:mt-0 px-3 py-1.5 text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold rounded-lg transition-colors no-underline active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none cursor-pointer"
        >
          Back to Results
        </Link>
      </div>

      {/* Summary cards */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <SummaryCard label="Total Tasks" value={summary.totalTasks} />
          <SummaryCard label="Total SP" value={summary.totalStoryPoints} />
          <SummaryCard
            label="Consensus"
            value={`${summary.consensusCount}/${summary.totalTasks}`}
          />
          <SummaryCard label="Participation" value={`${participationRatePct}%`} />
        </div>
      )}

      {/* Task vote distribution */}
      {tasks.length > 0 && (
        <section>
          <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-4">
            <h2 className="text-base font-semibold text-efi-text-primary">Vote Distribution</h2>
            <select
              value={effectiveTaskId}
              onChange={(e) => setSelectedTaskId(e.target.value)}
              className="text-base sm:text-sm bg-efi-well border border-white/10 rounded-lg px-3 py-1.5 text-efi-text-primary focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void cursor-pointer max-w-xs"
            >
              {tasks.map((t) => (
                <option key={t.taskId} value={t.taskId}>
                  {t.title}
                </option>
              ))}
            </select>
          </div>
          <div className="glass-frost rounded-xl p-4">
            <VoteDistributionChart distribution={selectedTask?.voteDistribution ?? {}} />
          </div>
        </section>
      )}

      {/* Average vs Final */}
      {tasks.length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-efi-text-primary mb-4">
            Average vs Final Estimate
          </h2>
          <div className="glass-frost rounded-xl p-4">
            <AvgVsFinalChart tasks={tasks} />
          </div>
        </section>
      )}

      {/* Participation Matrix */}
      {(analytics?.participationMatrix ?? []).length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-efi-text-primary mb-4">
            Participation Matrix
          </h2>
          <ParticipationMatrix matrix={analytics?.participationMatrix ?? []} tasks={tasks} />
        </section>
      )}
    </div>
  );
}
