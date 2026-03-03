import { useEffect } from 'react';

/**
 * Suppress CSS transitions on bfcache restore (back-navigation).
 * Without this, the frozen DOM flashes old state with visible
 * transition animations before React re-renders with current state.
 */
export function usePageRestore(): void {
  useEffect(() => {
    function handlePageShow(event: PageTransitionEvent) {
      if (!event.persisted) return;
      document.documentElement.classList.add('no-transitions');
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          document.documentElement.classList.remove('no-transitions');
        });
      });
    }
    window.addEventListener('pageshow', handlePageShow);
    return () => window.removeEventListener('pageshow', handlePageShow);
  }, []);
}
