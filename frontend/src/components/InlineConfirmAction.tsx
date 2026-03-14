import { useState } from 'react';
import { ButtonSpinner } from '@/components/Spinner';

interface InlineConfirmActionProps {
  label: string;
  onConfirm: () => void;
  isLoading: boolean;
  icon: React.ReactNode;
  title: string;
}

export function InlineConfirmAction({ label, onConfirm, isLoading, icon, title }: InlineConfirmActionProps) {
  const [pending, setPending] = useState(false);

  return pending ? (
    <span className="flex items-center gap-2 text-sm">
      <span className="text-efi-text-tertiary">{label}</span>
      <button
        type="button"
        onClick={onConfirm}
        disabled={isLoading}
        className="text-efi-error hover:text-red-400 font-medium cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none rounded"
      >
        {isLoading ? <ButtonSpinner /> : 'Yes'}
      </button>
      <button
        type="button"
        onClick={() => setPending(false)}
        className="text-efi-text-secondary hover:text-efi-text-primary cursor-pointer transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none rounded"
      >
        No
      </button>
    </span>
  ) : (
    <button
      type="button"
      onClick={() => setPending(true)}
      title={title}
      className="p-2 rounded-lg text-efi-text-tertiary hover:text-red-400 transition-colors cursor-pointer hover:bg-white/5 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
    >
      {icon}
    </button>
  );
}
