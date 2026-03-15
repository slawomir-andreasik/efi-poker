import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ParticipantProgress } from './ParticipantProgress';
import { mockParticipantProgress } from '@/test/fixtures';

vi.mock('@/api/queries', () => ({
  roomApi: {
    participantProgress: vi.fn(),
  },
}));

vi.mock('@tanstack/react-query', async () => {
  const actual = await vi.importActual('@tanstack/react-query');
  return {
    ...actual,
    useQuery: vi.fn(),
  };
});

import { useQuery } from '@tanstack/react-query';

const mockUseQuery = vi.mocked(useQuery);

function renderComponent() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <ParticipantProgress roomId="room-1" slug="test-slug" />
    </QueryClientProvider>,
  );
}

describe('ParticipantProgress', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should render nothing when no data', () => {
    mockUseQuery.mockReturnValue({ data: undefined } as ReturnType<typeof useQuery>);
    const { container } = renderComponent();
    expect(container.firstChild).toBeNull();
  });

  it('should render participant list sorted by votedCount ascending', () => {
    mockUseQuery.mockReturnValue({ data: mockParticipantProgress() } as ReturnType<typeof useQuery>);
    renderComponent();

    expect(screen.getByText('Participant Progress')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('Charlie')).toBeInTheDocument();

    // Charlie (0) should appear before Bob (2) who appears before Alice (5)
    const names = screen.getAllByText(/Alice|Bob|Charlie/).map((el) => el.textContent);
    expect(names).toEqual(['Charlie', 'Bob', 'Alice']);
  });

  it('should show progress counts', () => {
    mockUseQuery.mockReturnValue({ data: mockParticipantProgress() } as ReturnType<typeof useQuery>);
    renderComponent();

    expect(screen.getByText('5/5')).toBeInTheDocument();
    expect(screen.getByText('2/5')).toBeInTheDocument();
    expect(screen.getByText('0/5')).toBeInTheDocument();
  });

  it('should toggle visibility on header click', async () => {
    const user = userEvent.setup();
    mockUseQuery.mockReturnValue({ data: mockParticipantProgress() } as ReturnType<typeof useQuery>);
    renderComponent();

    // Initially open
    expect(screen.getByText('Alice')).toBeInTheDocument();

    // Click to collapse
    await user.click(screen.getByText('Participant Progress'));
    expect(screen.queryByText('Alice')).not.toBeInTheDocument();

    // Click to expand
    await user.click(screen.getByText('Participant Progress'));
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });

  it('should render nothing when participants array is empty', () => {
    mockUseQuery.mockReturnValue({
      data: mockParticipantProgress({ participants: [] }),
    } as ReturnType<typeof useQuery>);
    const { container } = renderComponent();
    expect(container.firstChild).toBeNull();
  });
});
