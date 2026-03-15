import { useMemo, useCallback } from 'react';
import { useLocalStorage } from './useLocalStorage';
import type { TaskWithEstimateResponse } from '@/api/types';

export type SortField = 'default' | 'title' | 'progress';
export type SortDirection = 'asc' | 'desc';

interface SortState {
  field: SortField;
  direction: SortDirection;
  onlyUnestimated: boolean;
}

const DEFAULT_SORT: SortState = {
  field: 'default',
  direction: 'asc',
  onlyUnestimated: false,
};

function getProgressRatio(task: TaskWithEstimateResponse): number {
  if (task.totalParticipants === 0) return 0;
  return task.votedCount / task.totalParticipants;
}

function compareTasks(a: TaskWithEstimateResponse, b: TaskWithEstimateResponse, field: SortField, direction: SortDirection): number {
  let result: number;

  switch (field) {
    case 'title':
      result = a.title.localeCompare(b.title);
      break;
    case 'progress':
      result = getProgressRatio(a) - getProgressRatio(b);
      break;
    case 'default':
    default:
      result = a.sortOrder - b.sortOrder;
      break;
  }

  return direction === 'desc' ? -result : result;
}

export function useSortedTasks(tasks: TaskWithEstimateResponse[], roomId: string) {
  const [sortState, setSortState] = useLocalStorage<SortState>(
    `efi-sort-${roomId}`,
    DEFAULT_SORT,
  );

  const unestimatedCount = useMemo(
    () => tasks.filter((t) => t.myEstimate == null).length,
    [tasks],
  );

  const sortedTasks = useMemo(() => {
    let filtered = tasks;

    if (sortState.onlyUnestimated) {
      filtered = tasks.filter((t) => t.myEstimate == null);
    }

    const sorted = [...filtered];
    sorted.sort((a, b) => compareTasks(a, b, sortState.field, sortState.direction));
    return sorted;
  }, [tasks, sortState]);

  const setSortField = useCallback(
    (field: SortField) => setSortState((prev) => {
      // Re-click active field: toggle direction instead of reverting to default
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

  return {
    sortedTasks,
    sortField: sortState.field,
    sortDirection: sortState.direction,
    onlyUnestimated: sortState.onlyUnestimated,
    unestimatedCount,
    setSortField,
    setSortDirection,
    setOnlyUnestimated,
  };
}
