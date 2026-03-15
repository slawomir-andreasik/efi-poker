import { Copy } from 'lucide-react';
import { useToast } from '@/components/Toast';

interface TraceCopyButtonProps {
  traceId: string;
}

export function TraceCopyButton({ traceId }: TraceCopyButtonProps) {
  const { showToast } = useToast();

  function handleCopy() {
    void navigator.clipboard.writeText(traceId).then(
      () => showToast('Trace ID copied!', 'success'),
      () => showToast('Failed to copy trace ID'),
    );
  }

  return (
    <button
      type="button"
      onClick={handleCopy}
      title="Copy trace ID for support"
      className="flex items-center gap-1 text-xs text-efi-text-tertiary hover:text-efi-text-secondary font-mono transition-colors cursor-pointer rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
    >
      <Copy className="w-3 h-3 shrink-0" />
      Trace: {traceId.slice(0, 8)}…
    </button>
  );
}
