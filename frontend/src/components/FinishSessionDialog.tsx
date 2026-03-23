import { Eye, Square } from 'lucide-react';
import type { AutoAssignedEstimate } from '@/api/types';
import { Modal } from '@/components/Modal';
import { ButtonSpinner } from '@/components/Spinner';

interface FinishSessionDialogProps {
  isOpen: boolean;
  isPending: boolean;
  onFinish: (revealVotes: boolean) => void;
  onCancel: () => void;
  autoAssigned: AutoAssignedEstimate[] | null;
  onDismissAutoAssigned: () => void;
}

export function FinishSessionDialog({
  isOpen,
  isPending,
  onFinish,
  onCancel,
  autoAssigned,
  onDismissAutoAssigned,
}: FinishSessionDialogProps) {
  if (autoAssigned && autoAssigned.length > 0) {
    return (
      <Modal
        isOpen={isOpen}
        onClose={onDismissAutoAssigned}
        title="Session Finished"
        maxWidth="max-w-lg"
      >
        <p className="text-sm text-efi-text-secondary mb-4">
          Final estimates were auto-assigned from median for {autoAssigned.length} task
          {autoAssigned.length > 1 ? 's' : ''}:
        </p>
        <div className="space-y-2 mb-6 max-h-60 overflow-y-auto">
          {autoAssigned.map((a) => (
            <div
              key={a.taskId}
              className="flex items-center justify-between bg-efi-well rounded-lg px-3 py-2"
            >
              <span className="text-sm text-efi-text-primary truncate mr-3">{a.taskTitle}</span>
              <span className="text-sm font-bold text-efi-gold shrink-0">{a.finalEstimate} SP</span>
            </div>
          ))}
        </div>
        <button
          type="button"
          onClick={onDismissAutoAssigned}
          className="w-full px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 transition-colors cursor-pointer"
        >
          OK
        </button>
      </Modal>
    );
  }

  return (
    <Modal isOpen={isOpen} onClose={onCancel} title="Finish Session?">
      <p className="text-sm text-efi-text-secondary mb-6">
        This will end the session. Voting will be disabled and final estimates will be auto-assigned
        from median where missing.
      </p>
      <div className="flex flex-col gap-2">
        <button
          type="button"
          onClick={() => onFinish(true)}
          disabled={isPending}
          className="w-full px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer flex items-center justify-center gap-2"
        >
          {isPending ? (
            <>
              <ButtonSpinner /> Finishing...
            </>
          ) : (
            <>
              <Eye className="w-4 h-4" /> Reveal & End
            </>
          )}
        </button>
        <button
          type="button"
          onClick={() => onFinish(false)}
          disabled={isPending}
          className="w-full px-4 py-2 rounded-lg text-sm font-medium border border-efi-text-secondary/30 text-efi-text-secondary hover:bg-white/5 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer flex items-center justify-center gap-2"
        >
          {isPending ? (
            <>
              <ButtonSpinner /> Finishing...
            </>
          ) : (
            <>
              <Square className="w-4 h-4" /> End Without Revealing
            </>
          )}
        </button>
        <button
          type="button"
          onClick={onCancel}
          disabled={isPending}
          className="w-full px-4 py-2 rounded-lg text-sm font-medium text-efi-text-tertiary hover:text-efi-text-secondary transition-colors cursor-pointer"
        >
          Cancel
        </button>
      </div>
    </Modal>
  );
}
