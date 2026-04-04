import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it } from 'vitest';
import { mockEstimate, mockTask } from '@/test/fixtures';
import { isDefaultComment, useSortedTasks } from './useSortedTasks';

const ROOM_ID = 'room-test';

describe('useSortedTasks', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  describe('unestimatedCount', () => {
    it('counts task with null myEstimate as unestimated', () => {
      const tasks = [mockTask({ id: 't-1', myEstimate: null })];
      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID));
      expect(result.current.unestimatedCount).toBe(1);
    });

    it('counts task with storyPoints as estimated', () => {
      const tasks = [mockTask({ id: 't-1', myEstimate: mockEstimate({ storyPoints: '5' }) })];
      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID));
      expect(result.current.unestimatedCount).toBe(0);
    });

    it('counts task with comment-only estimate (null storyPoints) as unestimated', () => {
      const tasks = [
        mockTask({
          id: 't-1',
          myEstimate: mockEstimate({ storyPoints: null, comment: 'draft comment' }),
        }),
      ];
      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID));
      expect(result.current.unestimatedCount).toBe(1);
    });

    it('counts task with comment-only estimate (undefined storyPoints) as unestimated', () => {
      const tasks = [
        mockTask({
          id: 't-1',
          myEstimate: mockEstimate({ storyPoints: undefined, comment: 'draft comment' }),
        }),
      ];
      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID));
      expect(result.current.unestimatedCount).toBe(1);
    });

    it('counts mixed tasks correctly', () => {
      const tasks = [
        mockTask({ id: 't-1', myEstimate: null }),
        mockTask({ id: 't-2', myEstimate: mockEstimate({ storyPoints: '3' }) }),
        mockTask({
          id: 't-3',
          myEstimate: mockEstimate({ storyPoints: null, comment: 'thinking...' }),
        }),
        mockTask({ id: 't-4', myEstimate: mockEstimate({ storyPoints: '8' }) }),
      ];
      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID));
      expect(result.current.unestimatedCount).toBe(2);
    });
  });

  describe('onlyUnestimated filter', () => {
    it('includes comment-only estimates when filtering unestimated', () => {
      const tasks = [
        mockTask({ id: 't-1', title: 'No estimate', myEstimate: null }),
        mockTask({
          id: 't-2',
          title: 'Voted',
          myEstimate: mockEstimate({ storyPoints: '5' }),
        }),
        mockTask({
          id: 't-3',
          title: 'Comment only',
          myEstimate: mockEstimate({ storyPoints: null, comment: 'draft' }),
        }),
      ];

      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID));

      act(() => {
        result.current.setOnlyUnestimated(true);
      });

      expect(result.current.sortedTasks).toHaveLength(2);
      expect(result.current.sortedTasks.map((t) => t.title)).toEqual([
        'No estimate',
        'Comment only',
      ]);
    });

    it('shows all tasks when filter is off', () => {
      const tasks = [
        mockTask({ id: 't-1', myEstimate: null }),
        mockTask({ id: 't-2', myEstimate: mockEstimate({ storyPoints: '5' }) }),
        mockTask({
          id: 't-3',
          myEstimate: mockEstimate({ storyPoints: null, comment: 'draft' }),
        }),
      ];

      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID));
      expect(result.current.sortedTasks).toHaveLength(3);
    });
  });

  describe('isDefaultComment', () => {
    const TEMPLATE = 'Please explain your estimate';

    it('returns true for null/undefined comment', () => {
      expect(isDefaultComment(null, TEMPLATE)).toBe(true);
      expect(isDefaultComment(undefined, TEMPLATE)).toBe(true);
    });

    it('returns true for empty comment', () => {
      expect(isDefaultComment('', TEMPLATE)).toBe(true);
    });

    it('returns true for exact template match', () => {
      expect(isDefaultComment('Please explain your estimate', TEMPLATE)).toBe(true);
    });

    it('returns true for template match with whitespace difference', () => {
      expect(isDefaultComment('  Please explain your estimate  ', TEMPLATE)).toBe(true);
    });

    it('returns false for edited comment', () => {
      expect(isDefaultComment('Needs OAuth integration', TEMPLATE)).toBe(false);
    });

    it('returns false for partially edited template', () => {
      expect(isDefaultComment('Please explain your estimate - complex task', TEMPLATE)).toBe(false);
    });
  });

  describe('needsCommentCount', () => {
    const TEMPLATE = 'Default comment';

    it('returns 0 when no template provided', () => {
      const tasks = [mockTask({ id: 't-1', myEstimate: mockEstimate({ comment: undefined }) })];
      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID));
      expect(result.current.needsCommentCount).toBe(0);
    });

    it('counts tasks with default/missing comments', () => {
      const tasks = [
        mockTask({ id: 't-1', myEstimate: mockEstimate({ comment: 'Default comment' }) }),
        mockTask({ id: 't-2', myEstimate: mockEstimate({ comment: 'Custom comment' }) }),
        mockTask({ id: 't-3', myEstimate: mockEstimate({ comment: undefined }) }),
      ];
      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID, TEMPLATE));
      expect(result.current.needsCommentCount).toBe(2);
    });
  });

  describe('onlyNeedsComment filter', () => {
    const TEMPLATE = 'Default comment';

    it('filters to tasks with default or missing comments', () => {
      const tasks = [
        mockTask({ id: 't-1', title: 'Default', myEstimate: mockEstimate({ comment: TEMPLATE }) }),
        mockTask({
          id: 't-2',
          title: 'Custom',
          myEstimate: mockEstimate({ comment: 'My analysis' }),
        }),
        mockTask({
          id: 't-3',
          title: 'No comment',
          myEstimate: mockEstimate({ comment: undefined }),
        }),
      ];

      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID, TEMPLATE));

      act(() => {
        result.current.setOnlyNeedsComment(true);
      });

      expect(result.current.sortedTasks).toHaveLength(2);
      expect(result.current.sortedTasks.map((t) => t.title)).toEqual(['Default', 'No comment']);
    });

    it('does nothing when no template provided', () => {
      const tasks = [
        mockTask({ id: 't-1', myEstimate: mockEstimate({ comment: undefined }) }),
        mockTask({ id: 't-2', myEstimate: mockEstimate({ comment: 'Custom' }) }),
      ];

      const { result } = renderHook(() => useSortedTasks(tasks, ROOM_ID));

      act(() => {
        result.current.setOnlyNeedsComment(true);
      });

      expect(result.current.sortedTasks).toHaveLength(2);
    });
  });
});
