import { useState } from 'react';
import { Modal } from '@/components/Modal';
import { TextArea } from '@/components/TextInput';

interface ImportModalProps {
  isOpen: boolean;
  onClose: () => void;
  onImport: (titles: string[]) => void;
}

export function ImportModal({ isOpen, onClose, onImport }: ImportModalProps) {
  const [text, setText] = useState('');
  const [validationError, setValidationError] = useState<string | null>(null);

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
    <Modal isOpen={isOpen} onClose={onClose} title="Import Tasks" maxWidth="max-w-lg">
      <TextArea
        value={text}
        onChange={(e) => {
          setText(e.target.value);
          setValidationError(null);
        }}
        placeholder="Paste task titles, one per line..."
        rows={8}
        className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 p-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold resize-none focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-obsidian"
      />

      {validationError && <p className="text-efi-error text-sm mt-1">{validationError}</p>}

      <div className="flex justify-end gap-3 mt-4">
        <button
          type="button"
          onClick={onClose}
          className="px-4 py-2 rounded-lg text-sm text-efi-text-secondary hover:text-efi-text-primary transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-obsidian focus-visible:outline-none"
        >
          Cancel
        </button>
        <button
          type="button"
          onClick={handleImport}
          disabled={text.trim().length === 0}
          className="px-4 py-2 rounded-lg text-sm font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-obsidian focus-visible:outline-none"
        >
          Import
        </button>
      </div>
    </Modal>
  );
}
