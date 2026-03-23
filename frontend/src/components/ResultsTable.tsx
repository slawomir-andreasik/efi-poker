import { useState } from 'react';

interface TaskEstimate {
  taskId: string;
  taskTitle: string;
  estimates: Record<string, number | string>;
  comments: Record<string, string>;
  average: number | null;
  median: number | null;
  finalEstimate?: string | null;
}

interface ResultsTableProps {
  tasks: TaskEstimate[];
  participants: string[];
}

export function getTaskSp(task: TaskEstimate): number {
  if (task.finalEstimate != null) {
    const n = Number(task.finalEstimate);
    return Number.isNaN(n) ? 0 : n;
  }
  return task.median ?? 0;
}

export function getConsensusLevel(
  estimates: Record<string, number | string>,
): 'consensus' | 'close' | 'divergent' {
  const numericValues = Object.values(estimates).filter((v): v is number => typeof v === 'number');
  if (numericValues.length === 0) return 'divergent';

  const unique = new Set(numericValues);
  if (unique.size === 1) return 'consensus';
  if (unique.size <= 2) return 'close';
  return 'divergent';
}

function consensusColor(level: 'consensus' | 'close' | 'divergent'): string {
  switch (level) {
    case 'consensus':
      return 'bg-efi-success/10 border-efi-success/30';
    case 'close':
      return 'bg-efi-warning/10 border-efi-warning/30';
    case 'divergent':
      return 'bg-efi-error/10 border-efi-error/30';
  }
}

export function ResultsTable({ tasks, participants }: ResultsTableProps) {
  const [showComments, setShowComments] = useState(false);

  if (tasks.length === 0) {
    return <p className="text-efi-text-secondary text-center py-8">No results to display yet.</p>;
  }

  const hasFinalEstimates = tasks.some((t) => t.finalEstimate);
  const hasAnyComments = tasks.some((t) => Object.keys(t.comments).length > 0);

  return (
    <div className="relative">
      {hasAnyComments && (
        <div className="flex justify-end mb-2">
          <button
            type="button"
            onClick={() => setShowComments((v) => !v)}
            aria-pressed={showComments}
            className="text-xs text-efi-text-tertiary hover:text-efi-text-secondary transition-colors cursor-pointer"
          >
            {showComments ? 'Hide comments' : 'Show comments'}
          </button>
        </div>
      )}
      <div className="overflow-x-auto">
        <table className="w-full text-sm min-w-[600px]">
          <thead>
            <tr className="border-b border-white/8">
              <th className="text-left py-3 px-2 text-efi-text-secondary font-medium">Task</th>
              {participants.map((name) => (
                <th
                  key={name}
                  className="text-center py-3 px-2 text-efi-text-secondary font-medium"
                >
                  {name}
                </th>
              ))}
              <th className="text-center py-3 px-2 text-efi-gold-light font-medium">Avg</th>
              <th className="text-center py-3 px-2 text-efi-gold-light font-medium">Med</th>
              {hasFinalEstimates && (
                <th className="text-center py-3 px-2 text-efi-success font-medium">Final</th>
              )}
            </tr>
          </thead>
          <tbody>
            {tasks.map((task) => {
              const level = getConsensusLevel(task.estimates);
              return (
                <tr
                  key={task.taskId}
                  className={`border border-transparent rounded-lg ${consensusColor(level)} hover:bg-white/5 transition-colors`}
                >
                  <td className="py-2 px-2 text-efi-text-primary font-medium">{task.taskTitle}</td>
                  {participants.map((name) => {
                    const value = task.estimates[name];
                    const taskComment = task.comments[name];
                    const isQuestion = value === '?';
                    return (
                      <td
                        key={name}
                        className={`text-center py-2 px-2 font-medium ${isQuestion ? 'text-efi-warning bg-efi-warning/10 rounded' : 'text-efi-text-secondary'}`}
                      >
                        {value ?? '-'}
                        {showComments && taskComment && (
                          <p className="text-[10px] text-efi-text-tertiary font-normal mt-0.5 whitespace-pre-line break-words max-w-[150px] mx-auto">
                            {taskComment}
                          </p>
                        )}
                      </td>
                    );
                  })}
                  <td className="text-center py-2 px-2 text-efi-gold-light font-semibold">
                    {task.average != null ? task.average.toFixed(1) : '-'}
                  </td>
                  <td className="text-center py-2 px-2 text-efi-gold-light font-semibold">
                    {task.median != null ? task.median : '-'}
                  </td>
                  {hasFinalEstimates && (
                    <td className="text-center py-2 px-2 text-efi-success font-bold">
                      {task.finalEstimate ?? '-'}
                    </td>
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {/* Scroll hint gradient on mobile when table overflows */}
      <div className="absolute right-0 top-0 bottom-0 w-8 bg-gradient-to-l from-efi-void to-transparent pointer-events-none sm:hidden" />
    </div>
  );
}

export type { TaskEstimate };
