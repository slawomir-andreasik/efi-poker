import { type ReactNode, useEffect } from 'react';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  maxWidth?: string;
  children: ReactNode;
}

export function Modal({ isOpen, onClose, title, maxWidth = 'max-w-md', children }: ModalProps) {
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
    // biome-ignore lint/a11y/noStaticElementInteractions: modal backdrop click-to-close is a standard a11y pattern
    // biome-ignore lint/a11y/useKeyWithClickEvents: Escape key handled via document-level listener in useEffect
    <div
      className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center z-50 p-4 animate-[fade-in_0.2s_ease-out]"
      onClick={onClose}
    >
      {/* biome-ignore lint/a11y/useKeyWithClickEvents: click only prevents event bubbling to backdrop */}
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`glass-crystal rounded-xl sm:rounded-2xl p-4 sm:p-6 w-full ${maxWidth} animate-[fade-in-scale_0.2s_ease-out]`}
        onClick={(e) => e.stopPropagation()}
      >
        {title && <h2 className="text-lg font-semibold text-efi-text-primary mb-4">{title}</h2>}
        {children}
      </div>
    </div>
  );
}
