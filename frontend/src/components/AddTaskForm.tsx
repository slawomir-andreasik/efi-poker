import { useState } from 'react';
import { TextArea, TextInput } from '@/components/TextInput';

interface AddTaskFormProps {
  onAdd: (title: string, description?: string) => Promise<void>;
  onImport?: () => void;
  inputBg?: string;
}

export function AddTaskForm({ onAdd, onImport, inputBg = 'bg-efi-well' }: AddTaskFormProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;

    const body = {
      title: title.trim(),
      description: description.trim() || undefined,
    };

    await onAdd(body.title, body.description);
    setTitle('');
    setDescription('');
  }

  return (
    <div className="mb-4 space-y-2">
      <div className="flex flex-col sm:flex-row gap-2">
        <form onSubmit={(e) => void handleSubmit(e)} className="flex-1 flex gap-2">
          <TextInput
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Add a task..."
            maxLength={255}
            className={`flex-1 rounded-lg ${inputBg} border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void`}
          />
          <button
            type="submit"
            disabled={!title.trim()}
            className="px-4 py-2 rounded-lg text-sm font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            Add
          </button>
        </form>
        {onImport && (
          <button
            type="button"
            onClick={onImport}
            className="w-full sm:w-auto px-4 py-2 rounded-lg text-sm font-medium border border-white/12 text-efi-text-secondary hover:border-white/20 hover:text-efi-text-primary transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            Import
          </button>
        )}
      </div>
      <TextArea
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        placeholder="Task description (optional)"
        maxLength={2000}
        rows={2}
        className={`w-full rounded-lg ${inputBg} border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold resize-none focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void`}
      />
    </div>
  );
}
