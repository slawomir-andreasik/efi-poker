import { Fragment, useState } from 'react';
import { isDefaultComment } from '@/hooks/useSortedTasks';

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
  commentTemplate?: string;
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

export function ResultsTable({ tasks, participants, commentTemplate }: ResultsTableProps) {
  const [expandedTaskId, setExpandedTaskId] = useState<string | null>(null);

  if (tasks.length === 0) {
    return <p className="text-efi-text-secondary text-center py-8">No results to display yet.</p>;
  }

  const hasFinalEstimates = tasks.some((t) => t.finalEstimate);
  const colSpan = participants.length + 3 + (hasFinalEstimates ? 1 : 0);

  return (
    <div className="relative">
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
              const hasComments = Object.values(task.comments).some(
                (c) => !isDefaultComment(c, commentTemplate ?? ''),
              );
              const isExpanded = expandedTaskId === task.taskId;

              return (
                <Fragment key={task.taskId}>
                  <tr
                    className={`border border-transparent rounded-lg ${consensusColor(level)} hover:bg-white/5 transition-colors ${hasComments ? 'cursor-pointer' : ''}`}
                    onClick={() =>
                      hasComments && setExpandedTaskId(isExpanded ? null : task.taskId)
                    }
                  >
                    <td className="py-2 px-2 text-efi-text-primary font-medium">
                      {task.taskTitle}
                    </td>
                    {participants.map((name) => {
                      const value = task.estimates[name];
                      const taskComment = task.comments[name];
                      const isQuestion = value === '?';
                      const hasCustomComment =
                        taskComment && !isDefaultComment(taskComment, commentTemplate ?? '');
                      return (
                        <td
                          key={name}
                          className={`text-center py-2 px-2 font-medium ${isQuestion ? 'text-efi-warning bg-efi-warning/10 rounded' : 'text-efi-text-secondary'}`}
                        >
                          {value ?? '-'}
                          {hasCustomComment && (
                            <span className="inline-block w-1.5 h-1.5 rounded-full bg-efi-gold-light/50 ml-1 align-middle" />
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
                  {isExpanded && (
                    <tr>
                      <td colSpan={colSpan} className="px-4 py-3 border-t border-white/8">
                        <div className="space-y-1.5 text-xs">
                          {participants.map((name) => {
                            const comment = task.comments[name];
                            if (!comment) return null;
                            const isDefault = isDefaultComment(comment, commentTemplate ?? '');
                            if (isDefault) {
                              return (
                                <div key={name}>
                                  <span className="text-efi-text-secondary font-medium">
                                    {name}:
                                  </span>
                                  <span className="text-efi-text-tertiary/50 ml-2 italic">
                                    (default)
                                  </span>
                                </div>
                              );
                            }
                            return (
                              <div key={name}>
                                <span className="text-efi-text-secondary font-medium">{name}:</span>
                                <span className="text-efi-text-tertiary ml-2 whitespace-pre-line">
                                  {comment}
                                </span>
                              </div>
                            );
                          })}
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
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
