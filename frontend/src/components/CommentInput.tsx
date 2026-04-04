import { Check, Save } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { TextArea } from '@/components/TextInput';

interface CommentInputProps {
  comment: string;
  onCommentChange: (value: string) => void;
  hasTemplate: boolean;
  onCommentSave: (comment: string) => void;
  saving?: boolean;
  explicitSave?: boolean;
}

export function CommentInput({
  comment,
  onCommentChange,
  hasTemplate,
  onCommentSave,
  saving = false,
  explicitSave = false,
}: CommentInputProps) {
  const initialRef = useRef(comment);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const [dirty, setDirty] = useState(false);

  const autoResize = useCallback(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${el.scrollHeight}px`;
  }, []);

  useEffect(() => {
    autoResize();
  }, [autoResize]);

  useEffect(() => {
    setDirty(comment.trim() !== initialRef.current.trim());
  }, [comment]);

  function handleBlur() {
    if (explicitSave) return;
    const trimmed = comment.trim();
    if (trimmed !== initialRef.current.trim()) {
      onCommentSave(trimmed);
      initialRef.current = trimmed;
      setDirty(false);
    }
  }

  function handleExplicitSave() {
    const trimmed = comment.trim();
    onCommentSave(trimmed);
    initialRef.current = trimmed;
    setDirty(false);
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
      {explicitSave ? (
        <button
          type="button"
          onClick={handleExplicitSave}
          disabled={!dirty}
          className={`absolute top-2 right-2 p-1 rounded transition-colors ${
            dirty
              ? 'text-efi-success hover:bg-efi-success/10 cursor-pointer'
              : 'text-efi-text-tertiary cursor-default'
          }`}
          title={dirty ? 'Save comment' : 'No changes'}
          aria-label="Save comment"
        >
          {saving ? <Check className="w-4 h-4 animate-fade-in" /> : <Save className="w-4 h-4" />}
        </button>
      ) : (
        saving && (
          <span className="absolute top-2 right-2 text-efi-success animate-fade-in">
            <Check className="w-4 h-4" />
          </span>
        )
      )}
    </div>
  );
}
