import { useCallback, useEffect, useRef, useState } from 'react';

export function useSaveIndicator(durationMs = 1500) {
  const [saving, setSaving] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  const showSaveIndicator = useCallback(() => {
    clearTimeout(timerRef.current);
    setSaving(true);
    timerRef.current = setTimeout(() => setSaving(false), durationMs);
  }, [durationMs]);

  const resetSaving = useCallback(() => {
    clearTimeout(timerRef.current);
    setSaving(false);
  }, []);

  useEffect(() => () => clearTimeout(timerRef.current), []);

  return { saving, showSaveIndicator, resetSaving };
}
