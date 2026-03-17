import { describe, it, expect } from 'vitest';
import { formatResultsAsMarkdown } from './markdown';
import type { TaskEstimate } from '@/components/ResultsTable';

function task(overrides: Partial<TaskEstimate> = {}): TaskEstimate {
  return {
    taskId: '1',
    taskTitle: 'Login Page',
    estimates: { Alice: 5, Bob: 3 },
    comments: {},
    average: 4.0,
    median: 4,
    finalEstimate: null,
    ...overrides,
  };
}

describe('formatResultsAsMarkdown', () => {
  it('should render room title as H1 and tasks as H2', () => {
    const md = formatResultsAsMarkdown('Sprint 42', [task()], ['Alice', 'Bob']);

    expect(md).toContain('# Results: Sprint 42');
    expect(md).toContain('## Login Page');
  });

  it('should render participants with story points', () => {
    const md = formatResultsAsMarkdown('Sprint', [task()], ['Alice', 'Bob']);

    expect(md).toContain('- Alice: **5**');
    expect(md).toContain('- Bob: **3**');
  });

  it('should render comments after story points', () => {
    const md = formatResultsAsMarkdown(
      'Sprint',
      [task({ comments: { Alice: 'Too complex' } })],
      ['Alice', 'Bob'],
    );

    expect(md).toContain('- Alice: **5** - Too complex');
    expect(md).not.toContain('- Bob: **3** -');
  });

  it('should render dash for missing votes', () => {
    const md = formatResultsAsMarkdown(
      'Sprint',
      [task({ estimates: { Alice: 5 } })],
      ['Alice', 'Bob'],
    );

    expect(md).toContain('- Bob: -');
  });

  it('should render avg and median in blockquote', () => {
    const md = formatResultsAsMarkdown('Sprint', [task()], ['Alice', 'Bob']);

    expect(md).toContain('> Avg: 4.0 | Med: 4');
  });

  it('should include final estimate when present', () => {
    const md = formatResultsAsMarkdown(
      'Sprint',
      [task({ finalEstimate: '5' })],
      ['Alice', 'Bob'],
    );

    expect(md).toContain('> Avg: 4.0 | Med: 4 | Final: 5');
  });

  it('should omit final estimate when absent', () => {
    const md = formatResultsAsMarkdown('Sprint', [task()], ['Alice', 'Bob']);

    expect(md).not.toContain('Final');
  });

  it('should return just H1 for empty tasks', () => {
    const md = formatResultsAsMarkdown('Sprint', [], []);

    expect(md).toBe('# Results: Sprint\n');
  });

  it('should escape pipe characters in comments', () => {
    const md = formatResultsAsMarkdown(
      'Sprint',
      [task({ comments: { Alice: 'A | B' } })],
      ['Alice', 'Bob'],
    );

    expect(md).toContain('- Alice: **5** - A \\| B');
  });

  it('should include final estimate of zero', () => {
    const md = formatResultsAsMarkdown(
      'Sprint',
      [task({ finalEstimate: '0' })],
      ['Alice', 'Bob'],
    );

    expect(md).toContain('Final: 0');
  });

  it('should render summary footer with totals', () => {
    const md = formatResultsAsMarkdown(
      'Sprint',
      [task({ finalEstimate: '5' }), task({ taskTitle: 'Dashboard', median: 8 })],
      ['Alice', 'Bob'],
    );

    expect(md).toContain('---');
    expect(md).toContain('**Summary:** 2 tasks | 13 SP | Consensus: 0/2');
  });

  it('should count consensus in summary', () => {
    const md = formatResultsAsMarkdown(
      'Sprint',
      [task({ estimates: { Alice: 5, Bob: 5 } })],
      ['Alice', 'Bob'],
    );

    expect(md).toContain('Consensus: 1/1');
  });

  it('should handle question mark votes', () => {
    const md = formatResultsAsMarkdown(
      'Sprint',
      [task({ estimates: { Alice: '?' } })],
      ['Alice'],
    );

    expect(md).toContain('- Alice: **?**');
  });
});
