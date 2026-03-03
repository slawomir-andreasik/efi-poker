import { useState, useEffect, useRef } from 'react';
import { Spinner } from '@/components/Spinner';
import { logger } from '@/utils/logger';

export function BackendGate({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<'checking' | 'up' | 'down'>('checking');
  const timerRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;

    async function probe() {
      logger.debug('Backend probe...');
      try {
        const res = await fetch('/api/v1/health', { signal: AbortSignal.timeout(3000) });
        if (mountedRef.current) {
          if (res.ok) {
            logger.info('Backend is up');
            setStatus('up');
          } else {
            logger.warn(`Backend returned ${res.status}, retrying in 4s`);
            setStatus('down');
            timerRef.current = setTimeout(probe, 4000);
          }
        }
      } catch {
        if (mountedRef.current) {
          logger.warn('Backend unreachable, retrying in 4s');
          setStatus('down');
          timerRef.current = setTimeout(probe, 4000);
        }
      }
    }

    void probe();
    return () => {
      mountedRef.current = false;
      clearTimeout(timerRef.current);
    };
  }, []);

  if (status === 'up') return <>{children}</>;

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3">
      <Spinner />
      {status === 'down' && (
        <>
          <p className="text-efi-text-secondary text-sm">Backend is starting up...</p>
          <p className="text-efi-text-tertiary text-xs">Connecting to server, please wait</p>
        </>
      )}
    </div>
  );
}
