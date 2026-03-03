import { useState } from 'react';

interface ImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImport: (titles: string[]) => void;
}

export function ImportModal({ isOpen, onClose, onImport }: ImportModalProps) {
  const [text, setText] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

  if (!isOpen) return null;

  function handleImport() {
    const titles = text
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.length > 0);

    if (titles.length === 0) {
      setValidationError('Enter at least one task title');
      return;
    }

    setValidationError(null);
    onImport(titles);
    setText('');
    onClose();
  }

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-[fade-in_0.2s_ease-out]">
      <div className="glass-crystal rounded-xl sm:rounded-2xl p-4 sm:p-6 w-full max-w-lg animate-[fade-in-scale_0.2s_ease-out]">
        <h2 className="text-lg font-semibold text-efi-text-primary mb-4">Import Tasks</h2>

        <textarea
          value={text}
          onChange={(e) => { setText(e.target.value); setValidationError(null); }}
          placeholder="Paste task titles, one per line..."
          rows={8}
          className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 p-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold resize-none focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-obsidian"
        />

        {validationError && (
          <p className="text-efi-error text-sm mt-1">{validationError}</p>
        )}

        <div className="flex justify-end gap-3 mt-4">
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-lg text-sm text-efi-text-secondary hover:text-efi-text-primary transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-obsidian focus-visible:outline-none"
          >
            Cancel
          </button>
          <button
            onClick={handleImport}
            disabled={text.trim().length === 0}
            className="px-4 py-2 rounded-lg text-sm font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-obsidian focus-visible:outline-none"
          >
            Import
          </button>
        </div>
      </div>
    </div>
  );
}
