import { Github } from 'lucide-react';
import { APP_VERSION } from '@/version';

export function Footer() {
  return (
    <footer className="border-t border-white/6 glass-whisper py-3">
      <div className="max-w-6xl mx-auto px-3 sm:px-4 flex items-center justify-between">
        <span className="hidden sm:block text-xs text-efi-text-secondary">
          Estimate. Focus. Improve.
        </span>

        <span className="text-xs text-efi-text-secondary">
          &copy; 2026 <span className="font-semibold">EFI Poker</span> &middot; v{APP_VERSION}
        </span>

        <a
          href="https://github.com/slawomir-andreasik/efi-poker"
          target="_blank"
          rel="noopener noreferrer"
          title="GitHub repository"
          className="inline-flex items-center gap-1.5 text-xs text-efi-text-secondary hover:text-efi-text-primary transition-colors"
        >
          <Github className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">GitHub</span>
        </a>
      </div>
    </footer>
  );
}
