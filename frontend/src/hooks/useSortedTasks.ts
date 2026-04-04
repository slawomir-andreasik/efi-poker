import { useCallback, useMemo } from 'react';
import type { TaskWithEstimateResponse } from '@/api/types';
import { useLocalStorage } from './useLocalStorage';

export type SortField = 'default' | 'title' | 'progress';
export type SortDirection = 'asc' | 'desc';

interface SortState {
  field: SortField;
  direction: SortDirection;
  onlyUnestimated: boolean;
  onlyNeedsComment: boolean;
}

const DEFAULT_SORT: SortState = {
  field: 'default',
  direction: 'asc',
  onlyUnestimated: false,
  onlyNeedsComment: false,
};

function getProgressRatio(task: TaskWithEstimateResponse): number {
  if (task.totalParticipants === 0) return 0;
  return task.votedCount / task.totalParticipants;
}

function compareTasks(
  a: TaskWithEstimateResponse,
  b: TaskWithEstimateResponse,
  field: SortField,
  direction: SortDirection,
): number {
  let result: number;

  switch (field) {
    case 'title':
      result = a.title.localeCompare(b.title);
      break;
    case 'progress':
      result = getProgressRatio(a) - getProgressRatio(b);
      break;
    default:
      result = a.sortOrder - b.sortOrder;
      break;
  }

  return direction === 'desc' ? -result : result;
}

const isUnestimated = (t: TaskWithEstimateResponse) => t.myEstimate?.storyPoints == null;

export function isDefaultComment(comment: string | undefined | null, template: string): boolean {
  if (!comment) return true;
  return comment.trim() === template.trim();
}

export function useSortedTasks(
  tasks: TaskWithEstimateResponse[],
  roomId: string,
  commentTemplate?: string,
) {
  const [sortState, setSortState] = useLocalStorage<SortState>(`efi-sort-${roomId}`, DEFAULT_SORT);

  const unestimatedCount = useMemo(() => tasks.filter(isUnestimated).length, [tasks]);

  const needsCommentCount = useMemo(() => {
    if (!commentTemplate) return 0;
    return tasks.filter((t) => isDefaultComment(t.myEstimate?.comment, commentTemplate)).length;
  }, [tasks, commentTemplate]);

  const sortedTasks = useMemo(() => {
    let filtered = tasks;

    if (sortState.onlyUnestimated) {
      filtered = filtered.filter(isUnestimated);
    }

    if (sortState.onlyNeedsComment && commentTemplate) {
      filtered = filtered.filter((t) => isDefaultComment(t.myEstimate?.comment, commentTemplate));
    }

    const sorted = [...filtered];
    sorted.sort((a, b) => compareTasks(a, b, sortState.field, sortState.direction));
    return sorted;
  }, [tasks, sortState, commentTemplate]);

  const setSortField = useCallback(
    (field: SortField) =>
      setSortState((prev) => {
        if (prev.field === field) {
          return { ...prev, direction: prev.direction === 'asc' ? 'desc' : 'asc' };
        }
        return { ...prev, field, direction: 'asc' };
      }),
    [setSortState],
  );

  const setSortDirection = useCallback(
    (direction: SortDirection) => setSortState((prev) => ({ ...prev, direction })),
    [setSortState],
  );

  const setOnlyUnestimated = useCallback(
    (onlyUnestimated: boolean) => setSortState((prev) => ({ ...prev, onlyUnestimated })),
    [setSortState],
  );

  const setOnlyNeedsComment = useCallback(
    (onlyNeedsComment: boolean) => setSortState((prev) => ({ ...prev, onlyNeedsComment })),
    [setSortState],
  );

  return {
    sortedTasks,
    sortField: sortState.field,
    sortDirection: sortState.direction,
    onlyUnestimated: sortState.onlyUnestimated,
    onlyNeedsComment: sortState.onlyNeedsComment,
    unestimatedCount,
    needsCommentCount,
    setSortField,
    setSortDirection,
    setOnlyUnestimated,
    setOnlyNeedsComment,
  };
}
