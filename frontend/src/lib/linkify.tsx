import type { ReactNode } from 'react';
import { memo } from 'react';

const URL_REGEX = /https?:\/\/[^\s<>"'()]+/g;
const MAX_URL_DISPLAY = 45;

/** Validate protocol + compute display label in a single URL parse. */
function parseUrl(raw: string): { displayUrl: string } | null {
  try {
    const { protocol, hostname, pathname } = new URL(raw);
    if (protocol !== 'http:' && protocol !== 'https:') return null;
    if (raw.length <= MAX_URL_DISPLAY) return { displayUrl: raw };
    const pathHint = pathname.length > 1 ? `${pathname.slice(0, 15)}...` : '';
    return { displayUrl: hostname + pathHint };
  } catch {
    return null;
  }
}

export const Linkify = memo(function Linkify({ text }: { text: string }) {
  const parts: ReactNode[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  URL_REGEX.lastIndex = 0;

  while ((match = URL_REGEX.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.slice(lastIndex, match.index));
    }
    const url = match[0];
    const parsed = parseUrl(url);
    if (parsed) {
      parts.push(
        <a
          key={match.index}
          href={url}
          target="_blank"
          rel="noopener noreferrer"
          title={parsed.displayUrl !== url ? url : undefined}
          className="text-efi-gold-light hover:text-white underline"
        >
          {parsed.displayUrl}
        </a>,
      );
    } else {
      parts.push(url);
    }
    lastIndex = match.index + url.length;
  }

  if (lastIndex < text.length) {
    parts.push(text.slice(lastIndex));
  }

  return <>{parts}</>;
});
