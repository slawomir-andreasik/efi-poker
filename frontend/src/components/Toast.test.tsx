import { act, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AUTO_DISMISS_MS, ToastProvider, useToast } from './Toast';

function TestTrigger({ message, type }: { message: string; type?: 'error' | 'success' | 'info' }) {
  const { showToast } = useToast();
  return (
    <button type="button" onClick={() => showToast(message, type)}>
      trigger
    </button>
  );
}

describe('Toast', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('should show toast message when triggered', () => {
    render(
      <ToastProvider>
        <TestTrigger message="Something went wrong" />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByText('trigger'));
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('should auto-dismiss after timeout', () => {
    vi.useFakeTimers();

    render(
      <ToastProvider>
        <TestTrigger message="Temporary" />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByText('trigger'));
    expect(screen.getByText('Temporary')).toBeInTheDocument();

    act(() => {
      vi.advanceTimersByTime(AUTO_DISMISS_MS + 100);
    });

    expect(screen.queryByText('Temporary')).not.toBeInTheDocument();
  });

  it('should limit visible toasts to 3', () => {
    function MultiTrigger() {
      const { showToast } = useToast();
      return (
        <div>
          <button type="button" onClick={() => showToast('Toast 1')}>
            t1
          </button>
          <button type="button" onClick={() => showToast('Toast 2')}>
            t2
          </button>
          <button type="button" onClick={() => showToast('Toast 3')}>
            t3
          </button>
          <button type="button" onClick={() => showToast('Toast 4')}>
            t4
          </button>
        </div>
      );
    }

    render(
      <ToastProvider>
        <MultiTrigger />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByText('t1'));
    fireEvent.click(screen.getByText('t2'));
    fireEvent.click(screen.getByText('t3'));
    fireEvent.click(screen.getByText('t4'));

    // Only last 3 should be visible
    expect(screen.queryByText('Toast 1')).not.toBeInTheDocument();
    expect(screen.getByText('Toast 2')).toBeInTheDocument();
    expect(screen.getByText('Toast 3')).toBeInTheDocument();
    expect(screen.getByText('Toast 4')).toBeInTheDocument();
  });

  it('should dismiss toast on close button click', () => {
    render(
      <ToastProvider>
        <TestTrigger message="Closeable" />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByText('trigger'));
    expect(screen.getByText('Closeable')).toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('Dismiss'));
    expect(screen.queryByText('Closeable')).not.toBeInTheDocument();
  });

  it('should apply glass-crystal styling with error icon by default', () => {
    render(
      <ToastProvider>
        <TestTrigger message="Error toast" />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByText('trigger'));
    const toastContainer = screen.getByText('Error toast').closest('div')?.parentElement;
    expect(toastContainer?.className).toContain('glass-crystal');
  });

  it('should apply glass-crystal styling with success icon', () => {
    render(
      <ToastProvider>
        <TestTrigger message="Success toast" type="success" />
      </ToastProvider>,
    );

    fireEvent.click(screen.getByText('trigger'));
    const toastContainer = screen.getByText('Success toast').closest('div')?.parentElement;
    expect(toastContainer?.className).toContain('glass-crystal');
  });

  it('should throw when useToast is used outside provider', () => {
    // Suppress console.error from React error boundary
    const spy = vi.spyOn(console, 'error').mockImplementation(() => {});

    function BadComponent() {
      useToast();
      return null;
    }

    expect(() => render(<BadComponent />)).toThrow('useToast must be used within ToastProvider');
    spy.mockRestore();
  });
});
