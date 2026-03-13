import { useEffect, useRef } from 'react';
import { APP_VERSION } from '@/version';
import { logger } from '@/utils/logger';

const POLL_INTERVAL_MS = 30_000;
const RELOAD_DELAY_MS = 1_500;
const FETCH_TIMEOUT_MS = 5_000;

interface UseVersionCheckOptions {
  showToast: (message: string, type?: 'error' | 'success' | 'info') => void;
}

export function useVersionCheck({ showToast }: UseVersionCheckOptions) {
  const reloadingRef = useRef(false);
  const mismatchCountRef = useRef(0);

  useEffect(() => {
    if (APP_VERSION === 'dev') return;

    async function checkVersion() {
      if (reloadingRef.current) return;
      try {
        const res = await fetch('/api/v1/health', {
          signal: AbortSignal.timeout(FETCH_TIMEOUT_MS),
        });
        if (!res.ok) return;
        const data = (await res.json()) as { version?: string };
        if (!data.version) return;

        // Strip -SNAPSHOT suffix from both sides for comparison
        const serverVersion = data.version.replace(/-SNAPSHOT$/, '');
        const clientVersion = APP_VERSION.replace(/-SNAPSHOT$/, '');
        if (serverVersion !== clientVersion) {
          mismatchCountRef.current += 1;
          logger.debug(
            `Version mismatch: client=${APP_VERSION}, server=${serverVersion} (count=${mismatchCountRef.current})`,
          );

          // Require 2 consecutive mismatches - backend must be stable for at least one poll cycle
          if (mismatchCountRef.current >= 2) {
            reloadingRef.current = true;
            showToast(`Updating to v${serverVersion}...`, 'info');

            // Unregister SW so reload fetches fresh assets from network
            if ('serviceWorker' in navigator) {
              const registrations = await navigator.serviceWorker.getRegistrations();
              await Promise.all(registrations.map((r) => r.unregister()));
            }

            setTimeout(() => location.reload(), RELOAD_DELAY_MS);
          }
        } else {
          mismatchCountRef.current = 0;
        }
      } catch {
        // Silently ignore - server may be restarting during deploy
      }
    }

    // Initial check after short delay (let the app settle)
    const initialTimeout = setTimeout(checkVersion, 5_000);
    const interval = setInterval(checkVersion, POLL_INTERVAL_MS);

    function onVisibilityChange() {
      if (document.visibilityState === 'visible') {
        checkVersion();
      }
    }
    document.addEventListener('visibilitychange', onVisibilityChange);

    return () => {
      clearTimeout(initialTimeout);
      clearInterval(interval);
      document.removeEventListener('visibilitychange', onVisibilityChange);
    };
  }, [showToast]);
}
