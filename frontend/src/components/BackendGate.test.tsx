import { act, screen, waitFor } from '@testing-library/react';
import { renderWithProviders } from '@/test/helpers';
import { BackendGate } from './BackendGate';

describe('BackendGate', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('renders children when backend responds', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true }));

    renderWithProviders(
      <BackendGate>
        <p>App content</p>
      </BackendGate>
    );

    await waitFor(() => {
      expect(screen.getByText('App content')).toBeInTheDocument();
    });
  });

  it('shows starting up message when backend is unreachable', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    renderWithProviders(
      <BackendGate>
        <p>App content</p>
      </BackendGate>
    );

    await waitFor(() => {
      expect(screen.getByText('Backend is starting up...')).toBeInTheDocument();
    });
    expect(screen.queryByText('App content')).not.toBeInTheDocument();
  });

  it('does not show status text during initial check', () => {
    vi.stubGlobal('fetch', vi.fn().mockImplementation(() => new Promise(() => {})));

    renderWithProviders(
      <BackendGate>
        <p>App content</p>
      </BackendGate>
    );

    expect(screen.queryByText('Backend is starting up...')).not.toBeInTheDocument();
    expect(screen.queryByText('App content')).not.toBeInTheDocument();
  });

  it('recovers and renders children after backend becomes available', async () => {
    vi.useFakeTimers();

    const fetchMock = vi.fn()
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValue({ ok: true });
    vi.stubGlobal('fetch', fetchMock);

    renderWithProviders(
      <BackendGate>
        <p>App content</p>
      </BackendGate>
    );

    // Run all timers: initial probe fails, 4s retry fires, retry succeeds
    await act(async () => {
      await vi.runAllTimersAsync();
    });

    expect(screen.getByText('App content')).toBeInTheDocument();
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });
});
