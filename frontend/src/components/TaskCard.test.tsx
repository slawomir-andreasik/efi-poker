import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TaskCard } from './TaskCard';

const baseProps = {
  id: 'task-1',
  title: 'Login Page',
  votedCount: 0,
  totalParticipants: 3,
  selectedSp: null,
  onEstimate: vi.fn(),
} as const;

describe('TaskCard - comment feature', () => {
  it('should not show comment textarea when no template and not required', () => {
    render(<TaskCard {...baseProps} />);

    expect(screen.queryByPlaceholderText('Add your comment...')).toBeNull();
    expect(screen.queryByRole('textbox')).toBeNull();
  });

  it('should show comment textarea when commentTemplate is provided', () => {
    const template = 'Risks:\nAssumptions:';
    render(<TaskCard {...baseProps} commentTemplate={template} />);

    const textarea = screen.getByRole('textbox');
    expect(textarea).toBeDefined();
    expect((textarea as HTMLTextAreaElement).value).toBe(template);
  });

  it('should show comment textarea when commentRequired is true', () => {
    render(<TaskCard {...baseProps} commentRequired />);

    expect(screen.getByPlaceholderText('Add your comment...')).toBeDefined();
  });

  it('should not show comment textarea when revealed', () => {
    render(
      <TaskCard
        {...baseProps}
        revealed
        commentRequired
        allEstimates={[]}
      />,
    );

    expect(screen.queryByPlaceholderText('Add your comment...')).toBeNull();
  });

  it('should not show comment textarea when disabled', () => {
    render(<TaskCard {...baseProps} disabled commentRequired />);

    expect(screen.queryByPlaceholderText('Add your comment...')).toBeNull();
  });

  it('should submit SP with comment when not required (direct submit)', async () => {
    const user = userEvent.setup();
    const onEstimate = vi.fn();

    render(
      <TaskCard
        {...baseProps}
        onEstimate={onEstimate}
        commentTemplate="Risks:"
      />,
    );

    // Type a comment
    const textarea = screen.getByRole('textbox');
    await user.clear(textarea);
    await user.type(textarea, 'No risks identified');

    // Click SP button - should submit immediately with comment
    await user.click(screen.getByRole('button', { name: '5' }));

    expect(onEstimate).toHaveBeenCalledWith('task-1', '5', 'No risks identified');
  });

  it('should auto-submit when commentRequired and SP clicked', async () => {
    const user = userEvent.setup();
    const onEstimate = vi.fn();

    render(
      <TaskCard
        {...baseProps}
        onEstimate={onEstimate}
        commentRequired
      />,
    );

    // Click SP - should submit immediately (backend validates comment)
    await user.click(screen.getByRole('button', { name: '8' }));

    expect(onEstimate).toHaveBeenCalledWith('task-1', '8', undefined);
    // No Submit Vote button should exist
    expect(screen.queryByText(/Submit Vote/)).toBeNull();
  });

  it('should auto-save comment on blur when SP is selected', async () => {
    const user = userEvent.setup();
    const onEstimate = vi.fn();

    render(
      <TaskCard
        {...baseProps}
        onEstimate={onEstimate}
        commentRequired
        selectedSp="5"
      />,
    );

    // Type comment and blur
    const textarea = screen.getByPlaceholderText('Add your comment...');
    await user.type(textarea, 'Medium complexity');
    await user.tab(); // blur

    expect(onEstimate).toHaveBeenCalledWith('task-1', '5', 'Medium complexity');
  });

  it('should display comments in reveal mode when estimates have comments', () => {
    render(
      <TaskCard
        {...baseProps}
        revealed
        allEstimates={[
          { id: 'e1', participantId: 'p1', participantNickname: 'Alice', storyPoints: '5', comment: 'Low risk', createdAt: '' },
          { id: 'e2', participantId: 'p2', participantNickname: 'Bob', storyPoints: '8', createdAt: '' },
        ]}
        averagePoints={6.5}
        medianPoints={6.5}
      />,
    );

    expect(screen.getByText('Low risk')).toBeDefined();
    expect(screen.getByText('Alice')).toBeDefined();
    expect(screen.getByText('Bob')).toBeDefined();
  });

  it('should initialize comment from myComment prop', () => {
    render(
      <TaskCard
        {...baseProps}
        commentRequired
        myComment="Previously saved comment"
      />,
    );

    const textarea = screen.getByRole('textbox');
    expect((textarea as HTMLTextAreaElement).value).toBe('Previously saved comment');
  });
});
