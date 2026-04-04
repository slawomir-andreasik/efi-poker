import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { ApiError } from '@/api/client';
import { analyticsApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import { ConsensusRateChart } from '@/components/charts/ConsensusRateChart';
import { SpPerRoomChart } from '@/components/charts/SpPerRoomChart';
import { SummaryCard } from '@/components/charts/SummaryCard';
import { NotFoundState } from '@/components/NotFoundState';
import { PageSpinner } from '@/components/PageSpinner';
import { TraceCopyButton } from '@/components/TraceCopyButton';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { ghostLinkBtn } from '@/styles/buttons';
import { getErrorMessage } from '@/utils/error';

export function ProjectAnalyticsPage() {
  const { slug } = useParams<{ slug: string }>();

  const {
    data: analytics,
    isLoading,
    error,
  } = useQuery({
    queryKey: queryKeys.analytics.project(slug as string),
    queryFn: () => analyticsApi.project(slug as string),
    enabled: Boolean(slug),
  });

  useDocumentTitle('Analytics', analytics?.projectName, slug);

  if (isLoading && !analytics) {
    return <PageSpinner />;
  }

  if (error) {
    if (error instanceof ApiError && error.status === 404) {
      return <NotFoundState message="Project not found" backTo="/" backLabel="Back to Projects" />;
    }
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
        <p className="text-efi-error">{getErrorMessage(error)}</p>
        {error instanceof ApiError && error.traceId && <TraceCopyButton traceId={error.traceId} />}
        <Link
          to={slug ? `/p/${slug}` : '/'}
          className="text-sm text-efi-gold-light hover:text-efi-gold transition-colors no-underline hover:underline"
        >
          Back to Project
        </Link>
      </div>
    );
  }

  const summary = analytics?.summary;
  const avgConsensus = summary ? Math.round(summary.averageConsensusRate) : 0;

  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between animate-[fade-in-up_0.6s_ease-out] motion-reduce:animate-none">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-efi-text-primary">Project Analytics</h1>
          {analytics?.projectName && (
            <p className="text-sm text-efi-text-secondary mt-1">{analytics.projectName}</p>
          )}
        </div>
        <Link to={slug ? `/p/${slug}` : '/'} className={`mt-3 sm:mt-0 ${ghostLinkBtn}`}>
          Back to Project
        </Link>
      </div>

      {/* Summary cards */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <SummaryCard label="Total Rooms" value={summary.totalRooms} />
          <SummaryCard label="Total Tasks" value={summary.totalTasks} />
          <SummaryCard label="Total SP" value={summary.totalStoryPoints} />
          <SummaryCard label="Avg Consensus" value={`${avgConsensus}%`} />
        </div>
      )}

      {/* SP per room */}
      {(analytics?.roomStats ?? []).length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-efi-text-primary mb-4">
            Story Points per Room
          </h2>
          <div className="glass-frost rounded-xl p-4">
            <SpPerRoomChart rooms={analytics?.roomStats ?? []} />
          </div>
        </section>
      )}

      {/* Consensus rate per room */}
      {(analytics?.roomStats ?? []).length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-efi-text-primary mb-4">
            Consensus Rate per Room
          </h2>
          <div className="glass-frost rounded-xl p-4">
            <ConsensusRateChart rooms={analytics?.roomStats ?? []} />
          </div>
        </section>
      )}

      {/* Most contentious tasks */}
      {(analytics?.topContentiousTasks ?? []).length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-efi-text-primary mb-4">
            Most Contentious Tasks
          </h2>
          <div className="glass-frost rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/8">
                  <th className="text-left px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide">
                    Task
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide hidden sm:table-cell">
                    Room
                  </th>
                  <th className="text-right px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide">
                    Spread
                  </th>
                  <th className="text-right px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide">
                    Votes
                  </th>
                </tr>
              </thead>
              <tbody>
                {analytics?.topContentiousTasks.map((task, idx) => (
                  <tr key={task.taskId} className={idx % 2 === 0 ? 'bg-white/[0.02]' : ''}>
                    <td className="px-4 py-3 text-efi-text-primary font-medium">
                      {task.taskTitle}
                    </td>
                    <td className="px-4 py-3 text-efi-text-secondary hidden sm:table-cell">
                      {task.roomTitle}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <span className="inline-block px-2 py-0.5 rounded text-xs font-mono font-bold text-red-400 bg-red-500/10 border border-red-500/20">
                        {task.spread.toFixed(1)}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right text-efi-text-secondary">
                      {task.voteCount}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {/* Participation leaderboard */}
      {(analytics?.participantLeaderboard ?? []).length > 0 && (
        <section>
          <h2 className="text-base font-semibold text-efi-text-primary mb-4">
            Participation Leaderboard
          </h2>
          <div className="glass-frost rounded-xl overflow-hidden">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/8">
                  <th className="text-left px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide">
                    #
                  </th>
                  <th className="text-left px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide">
                    Participant
                  </th>
                  <th className="text-right px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide">
                    Voted
                  </th>
                  <th className="text-right px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide">
                    Rate
                  </th>
                </tr>
              </thead>
              <tbody>
                {analytics?.participantLeaderboard.map((entry, idx) => (
                  <tr key={entry.participantId} className={idx % 2 === 0 ? 'bg-white/[0.02]' : ''}>
                    <td className="px-4 py-3 text-efi-text-tertiary text-xs">{idx + 1}</td>
                    <td className="px-4 py-3 text-efi-text-primary font-medium">
                      {entry.nickname}
                    </td>
                    <td className="px-4 py-3 text-right text-efi-text-secondary">
                      {entry.tasksVoted}/{entry.totalTasks}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <span className="inline-block px-2 py-0.5 rounded text-xs font-bold text-efi-gold-light bg-efi-gold/10 border border-efi-gold/20">
                        {Math.round(entry.participationRate)}%
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}
