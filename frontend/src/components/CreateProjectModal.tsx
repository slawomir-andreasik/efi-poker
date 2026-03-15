import { useState, useEffect } from 'react';
import { RandomNameButton } from '@/components/RandomNameButton';
import { generateProjectName } from '@/utils/nameGenerator';
import { ButtonSpinner } from '@/components/Spinner';
import { TextInput } from '@/components/TextInput';
import { Modal } from '@/components/Modal';

interface CreateProjectModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (name: string) => Promise<void>;
  isPending: boolean;
  isGuest: boolean;
}

export function CreateProjectModal({ isOpen, onClose, onCreate, isPending, isGuest }: CreateProjectModalProps) {
  const [projectName, setProjectName] = useState('');

  useEffect(() => {
    if (!isOpen) {
      setProjectName('');
    }
  }, [isOpen]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!projectName.trim() || isPending) return;
    void onCreate(projectName.trim());
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="New Project">
      <form onSubmit={handleSubmit}>
        <label htmlFor="modal-project-name" className="block text-sm font-medium text-efi-text-secondary mb-1">
          Project Name
        </label>
        <div className="flex items-center gap-1">
          <TextInput
            id="modal-project-name"
            type="text"
            value={projectName}
            onChange={(e) => setProjectName(e.target.value)}
            placeholder="e.g. Sprint 42 - Backend"
            maxLength={255}
            autoFocus
            className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
          />
          <RandomNameButton onGenerate={setProjectName} generator={generateProjectName} />
        </div>

        {isGuest && (
          <p className="text-[11px] text-efi-text-tertiary mt-2">
            As a guest, admin access is stored in this browser only.
          </p>
        )}

        <div className="flex justify-end gap-3 mt-4">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-lg text-sm text-efi-text-secondary hover:text-efi-text-primary transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={isPending || !projectName.trim()}
            className="px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none flex items-center gap-2"
          >
            {isPending ? <><ButtonSpinner /> Creating...</> : 'Create Project'}
          </button>
        </div>
      </form>
    </Modal>
  );
}
