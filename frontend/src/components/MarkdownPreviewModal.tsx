import { Copy } from 'lucide-react';
import { Modal } from '@/components/Modal';
import { ghostIconBtn } from '@/styles/buttons';

interface MarkdownPreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  markdown: string;
  onCopy: () => void;
}

export function MarkdownPreviewModal({
  isOpen,
  onClose,
  markdown,
  onCopy,
}: MarkdownPreviewModalProps) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Markdown Preview" maxWidth="max-w-3xl">
      <div className="flex justify-end mb-3">
        <button type="button" onClick={onCopy} className={ghostIconBtn}>
          <Copy className="w-4 h-4" />
          Copy
        </button>
      </div>
      <pre className="text-sm text-efi-text-secondary font-mono bg-efi-well rounded-lg p-4 overflow-auto max-h-[70vh] whitespace-pre-wrap break-words border border-white/5">
        {markdown}
      </pre>
    </Modal>
  );
}
