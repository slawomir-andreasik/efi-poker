import { forwardRef } from 'react';

/** Strip C0 control characters (U+0000-U+001F) and DEL (U+007F). */
function sanitize(value: string): string {
  // eslint-disable-next-line no-control-regex
  return value.replace(/[\x00-\x1f\x7f]/g, '');
}

/** Strips control chars but keeps \t (0x09), \n (0x0A), \r (0x0D) for multiline content. */
function sanitizeMultiline(value: string): string {
  // eslint-disable-next-line no-control-regex
  return value.replace(/[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]/g, '');
}

/** Input wrapper that strips control characters on change. Drop-in replacement for <input>. */
export const TextInput = forwardRef<HTMLInputElement, React.ComponentProps<'input'>>(
  ({ onChange, ...props }, ref) => (
    <input
      ref={ref}
      {...props}
      onChange={
        onChange &&
        ((e) => {
          e.target.value = sanitize(e.target.value);
          onChange(e);
        })
      }
    />
  ),
);
TextInput.displayName = 'TextInput';

/** Textarea wrapper that strips control chars (keeps tab/newline). Drop-in replacement for <textarea>. */
export const TextArea = forwardRef<HTMLTextAreaElement, React.ComponentProps<'textarea'>>(
  ({ onChange, ...props }, ref) => (
    <textarea
      ref={ref}
      {...props}
      onChange={
        onChange &&
        ((e) => {
          e.target.value = sanitizeMultiline(e.target.value);
          onChange(e);
        })
      }
    />
  ),
);
TextArea.displayName = 'TextArea';
