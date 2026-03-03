import { useEffect } from 'react';

export function useDocumentTitle(...parts: (string | undefined | null)[]) {
  const key = parts.join('|');
  useEffect(() => {
    const filtered = parts.filter(Boolean) as string[];
    document.title = filtered.length
      ? `${filtered.join(' - ')} - EFI Poker`
      : 'EFI Poker';
    return () => { document.title = 'EFI Poker'; };
  }, [key]); // eslint-disable-line react-hooks/exhaustive-deps
}
