import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { App } from './App';
import { ApiError } from './api/client';
import { ToastProvider } from './components/Toast';
import { logger } from './utils/logger';
import './styles/theme.css';

window.addEventListener('unhandledrejection', (event) => {
  logger.error('Unhandled promise rejection:', String(event.reason));
});

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5_000,
      gcTime: 5 * 60_000,
      refetchOnWindowFocus: true,
      refetchIntervalInBackground: false,
      retry: (failureCount, error) => {
        // Don't retry client errors (4xx)
        if (error instanceof ApiError && error.status < 500) return false;
        // Retry up to 3 times for server errors and network failures
        return failureCount < 3;
      },
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 15000),
    },
  },
});

const root = document.getElementById('root');
if (!root) throw new Error('Root element not found');

createRoot(root).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <ToastProvider>
          <App />
        </ToastProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
