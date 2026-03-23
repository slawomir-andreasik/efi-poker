import type { ParticipationMatrixEntry, TaskAnalyticsEntry } from '@/api/types';

interface ParticipationMatrixProps {
  matrix: ParticipationMatrixEntry[];
  tasks: TaskAnalyticsEntry[];
}

export function ParticipationMatrix({ matrix, tasks }: ParticipationMatrixProps) {
  if (matrix.length === 0 || tasks.length === 0) {
    return (
      <div className="flex items-center justify-center py-8 text-efi-text-tertiary text-sm">
        No participation data available
      </div>
    );
  }

  return (
    <div className="glass-frost rounded-xl overflow-x-auto">
      <table className="w-full text-sm min-w-[500px]">
        <thead>
          <tr className="border-b border-white/8">
            <th className="text-left px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide whitespace-nowrap">
              Participant
            </th>
            {tasks.map((task) => (
              <th
                key={task.taskId}
                className="px-3 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wide whitespace-nowrap max-w-[120px]"
                title={task.title}
              >
                <span className="block truncate max-w-[100px]">{task.title}</span>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {matrix.map((row, idx) => (
            <tr key={row.participantId} className={idx % 2 === 0 ? 'bg-white/[0.02]' : ''}>
              <td className="px-4 py-2.5 text-efi-text-primary font-medium whitespace-nowrap">
                {row.nickname}
              </td>
              {tasks.map((task) => {
                const vote = row.taskVotes[task.taskId];
                return (
                  <td key={task.taskId} className="px-3 py-2.5 text-center">
                    {vote != null ? (
                      <span className="inline-block px-2 py-0.5 rounded text-xs font-mono font-bold text-efi-gold-light bg-efi-gold/10 border border-efi-gold/20">
                        {vote}
                      </span>
                    ) : (
                      <span className="text-efi-text-tertiary text-xs">-</span>
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
