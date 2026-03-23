import { useMemo } from 'react';
import type { TaskWithEstimateResponse } from '@/api/types';

interface ProgressStatsProps {
  tasks: TaskWithEstimateResponse[];
  isRevealed: boolean;
}

export function ProgressStats({ tasks, isRevealed }: ProgressStatsProps) {
  const totalTasks = tasks.length;
  const stats = useMemo(() => {
    let myVotes = 0,
      fullyVoted = 0,
      finalSet = 0;
    for (const t of tasks) {
      if (t.myEstimate != null) myVotes++;
      if (t.totalParticipants > 0 && t.votedCount === t.totalParticipants) fullyVoted++;
      if (t.finalEstimate != null) finalSet++;
    }
    return { myVotes, fullyVoted, finalSet };
  }, [tasks]);

  if (totalTasks === 0) return null;

  return (
    <div className="rounded-xl glass-whisper p-4 space-y-3">
      <h3 className="text-xs font-medium text-efi-text-secondary uppercase tracking-wider">
        Progress
      </h3>

      <div className="space-y-2.5">
        {/* Total tasks */}
        <div className="flex items-center justify-between text-sm">
          <span className="text-efi-text-secondary">Tasks</span>
          <span className="text-efi-text-primary font-medium">{totalTasks}</span>
        </div>

        {/* My votes - hidden on revealed rooms if user didn't participate */}
        {(!isRevealed || stats.myVotes > 0) && (
          <div>
            <div className="flex items-center justify-between text-sm mb-1">
              <span className="text-efi-text-secondary">My votes</span>
              <span className="text-efi-text-primary font-medium">
                {stats.myVotes} / {totalTasks}
              </span>
            </div>
            <MiniBar value={stats.myVotes} total={totalTasks} />
          </div>
        )}

        {/* Team progress */}
        <div>
          <div className="flex items-center justify-between text-sm mb-1">
            <span className="text-efi-text-secondary">Team complete</span>
            <span className="text-efi-text-primary font-medium">
              {stats.fullyVoted} / {totalTasks}
            </span>
          </div>
          <MiniBar value={stats.fullyVoted} total={totalTasks} />
        </div>

        {/* Final SP set (after reveal) */}
        {isRevealed && (
          <div>
            <div className="flex items-center justify-between text-sm mb-1">
              <span className="text-efi-text-secondary">Final SP set</span>
              <span className="text-efi-text-primary font-medium">
                {stats.finalSet} / {totalTasks}
              </span>
            </div>
            <MiniBar value={stats.finalSet} total={totalTasks} color="green" />
          </div>
        )}
      </div>
    </div>
  );
}

function MiniBar({
  value,
  total,
  color = 'gold',
}: {
  value: number;
  total: number;
  color?: 'gold' | 'green';
}) {
  const pct = total > 0 ? Math.round((value / total) * 100) : 0;
  const allDone = value === total && total > 0;
  const barColor = allDone
    ? 'bg-efi-success'
    : color === 'green'
      ? 'bg-efi-success/70'
      : 'bg-gradient-to-r from-efi-gold to-efi-gold-light';

  return (
    <div className="w-full h-1.5 bg-efi-well rounded-full overflow-hidden">
      <div
        className={`h-full rounded-full transition-all duration-500 ${barColor}`}
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}
