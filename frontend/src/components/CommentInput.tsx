import { useRef, useEffect, useCallback } from 'react';
import { TextArea } from '@/components/TextInput';
import { Check } from 'lucide-react';

interface CommentInputProps {
  comment: string;
  onCommentChange: (value: string) => void;
  hasTemplate: boolean;
  onCommentSave: (comment: string) => void;
  saving?: boolean;
}

export function CommentInput({
  comment,
  onCommentChange,
  hasTemplate,
  onCommentSave,
  saving = false,
}: CommentInputProps) {
  const initialRef = useRef(comment);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const autoResize = useCallback(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = el.scrollHeight + 'px';
  }, []);

  useEffect(() => {
    autoResize();
  }, [comment, autoResize]);

  function handleBlur() {
    const trimmed = comment.trim();
    if (trimmed !== initialRef.current.trim()) {
      onCommentSave(trimmed);
      initialRef.current = trimmed;
    }
  }

  return (
    <div className="mt-3 relative">
      <TextArea
        ref={textareaRef}
        value={comment}
        onChange={(e) => onCommentChange(e.target.value)}
        onBlur={handleBlur}
        placeholder={hasTemplate ? undefined : 'Add your comment...'}
        maxLength={2000}
        rows={3}
        className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold resize-y max-h-80 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
      />
      {saving && (
        <span className="absolute top-2 right-2 text-efi-success animate-fade-in">
          <Check className="w-4 h-4" />
        </span>
      )}
    </div>
  );
}
