import { Copy } from 'lucide-react';
import { useToast } from '@/components/Toast';
import { copyRoomLink } from '@/utils/clipboard';

interface ShareButtonProps {
  roomSlug: string;
}

export function ShareButton({ roomSlug }: ShareButtonProps) {
  const { showToast } = useToast();

  return (
    <button
      type="button"
      onClick={() => void copyRoomLink(roomSlug, showToast)}
      title="Copy room join link"
      className="flex items-center gap-1 px-2.5 py-1 text-xs font-medium text-efi-text-secondary border border-white/10 rounded-lg hover:text-efi-text-primary hover:border-white/20 transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
    >
      <Copy className="w-3.5 h-3.5" />
      Share
    </button>
  );
}
