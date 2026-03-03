import { useEffect } from 'react';
import { JoinByCodeForm } from '@/components/JoinByCodeForm';

interface JoinByCodeModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function JoinByCodeModal({ isOpen, onClose }: JoinByCodeModalProps) {
  useEffect(() => {
    if (!isOpen) return;
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-[fade-in_0.2s_ease-out]"
      onClick={onClose}
    >
      <div
        className="glass-crystal rounded-xl sm:rounded-2xl p-4 sm:p-6 w-full max-w-md animate-[fade-in-scale_0.2s_ease-out]"
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="text-lg font-semibold text-efi-text-primary mb-4">Join</h2>
        <p className="text-sm text-efi-text-secondary mb-4">
          Enter a room code (ABC-123) or project slug to join.
        </p>
        <JoinByCodeForm />
      </div>
    </div>
  );
}
