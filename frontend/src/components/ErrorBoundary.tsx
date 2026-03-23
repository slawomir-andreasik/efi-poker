import { Component, type ErrorInfo, type ReactNode } from 'react';
import { getCurrentTraceId } from '@/lib/tracing';
import { logger } from '@/utils/logger';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    logger.error('ErrorBoundary caught:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      const traceId = getCurrentTraceId();
      return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] px-4">
          <h1 className="text-2xl font-bold text-efi-text-primary mb-2">Something went wrong</h1>
          <p className="text-efi-text-secondary mb-4">An unexpected error occurred.</p>
          <button
            type="button"
            onClick={() => void navigator.clipboard.writeText(traceId).catch(() => {})}
            title="Copy trace ID for support"
            className="text-xs text-efi-text-tertiary hover:text-efi-text-secondary font-mono transition-colors cursor-pointer mb-6 rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
          >
            Trace: {traceId.slice(0, 8)}… [copy]
          </button>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="px-6 py-3 rounded-lg font-medium text-sm bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 transition-opacity cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            Refresh Page
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
