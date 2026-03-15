import { useState, useRef } from 'react';
import { Settings } from 'lucide-react';
import { useUpdateRoom } from '@/api/mutations';
import { useToast } from '@/components/Toast';
import { CollapsibleSection } from '@/components/CollapsibleSection';
import { DeadlineInput, formatPreview, toLocalDatetimeString } from '@/components/DeadlineInput';
import { TextInput, TextArea } from '@/components/TextInput';
import { logger } from '@/utils/logger';
import { getErrorMessage } from '@/utils/error';
import type { RoomDetailResponse } from '@/api/types';

export const DEFAULT_COMMENT_TEMPLATE = `Assumptions:
- all clear / ...
Risks:
- no risks / ...
Open questions:
- no questions / ...`;

type RoomSettingsRoom = Pick<RoomDetailResponse, 'id' | 'title' | 'description' | 'deadline' | 'topic' | 'roomType' | 'autoRevealOnDeadline' | 'commentTemplate' | 'commentRequired'>;

interface RoomSettingsProps {
  slug: string;
  room: RoomSettingsRoom;
}

export function RoomSettings({ slug, room }: RoomSettingsProps) {
  const { showToast } = useToast();
  const updateRoom = useUpdateRoom(slug);

  function saveField(field: string, value: unknown) {
    updateRoom.mutate(
      { roomId: room.id, body: { [field]: value } },
      {
        onError: (err) => {
          logger.warn(`Failed to update ${field}:`, getErrorMessage(err));
          showToast(getErrorMessage(err));
        },
      },
    );
  }

  const isAsync = room.roomType === 'ASYNC';

  return (
    <CollapsibleSection icon={Settings} label="Room Settings">
      <div className="space-y-4">
          <BlurSaveInput
            label="Title"
            initialValue={room.title}
            onSave={(v) => saveField('title', v)}
          />

          <BlurSaveInput
            label="Description"
            initialValue={room.description ?? ''}
            onSave={(v) => saveField('description', v || null)}
            placeholder="Room description (optional)"
            multiline
          />

          {isAsync && (
            <BlurSaveInput
              label="Topic"
              initialValue={room.topic ?? ''}
              onSave={(v) => saveField('topic', v || null)}
              placeholder="Current estimation topic (optional)"
            />
          )}

          <CommentTemplateField
            initialValue={room.commentTemplate ?? ''}
            onSave={(v) => saveField('commentTemplate', v || null)}
          />

          <label className="flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={room.commentRequired ?? false}
              onChange={(e) => saveField('commentRequired', e.target.checked)}
              className="w-4 h-4 accent-efi-gold cursor-pointer text-base"
            />
            <span className="text-sm text-efi-text-secondary">Require comments when voting</span>
          </label>

          {isAsync && (
            <label className="flex items-center gap-2 cursor-pointer select-none">
              <input
                type="checkbox"
                checked={room.autoRevealOnDeadline ?? true}
                onChange={(e) => saveField('autoRevealOnDeadline', e.target.checked)}
                className="w-4 h-4 accent-efi-gold cursor-pointer text-base"
              />
              <span className="text-sm text-efi-text-secondary">Auto-reveal when deadline passes</span>
            </label>
          )}

          {isAsync && <DeadlineEditor room={room} onSave={(v) => saveField('deadline', v)} />}
        </div>
    </CollapsibleSection>
  );
}

function BlurSaveInput({
  label,
  initialValue,
  onSave,
  placeholder,
  multiline = false,
}: {
  label: string;
  initialValue: string;
  onSave: (value: string) => void;
  placeholder?: string;
  multiline?: boolean;
}) {
  const [value, setValue] = useState(initialValue);
  const initialRef = useRef(initialValue);

  function handleBlur() {
    const trimmed = value.trim();
    if (trimmed !== initialRef.current.trim()) {
      onSave(trimmed);
      initialRef.current = trimmed;
    }
  }

  const inputClass = 'w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void';

  return (
    <div>
      <label className="block text-xs text-efi-text-secondary mb-1">{label}</label>
      {multiline ? (
        <TextArea
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onBlur={handleBlur}
          placeholder={placeholder}
          maxLength={2000}
          rows={2}
          className={`${inputClass} resize-y max-h-40`}
        />
      ) : (
        <TextInput
          type="text"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onBlur={handleBlur}
          onKeyDown={(e) => { if (e.key === 'Enter') e.currentTarget.blur(); }}
          placeholder={placeholder}
          maxLength={255}
          className={inputClass}
        />
      )}
    </div>
  );
}

function CommentTemplateField({
  initialValue,
  onSave,
}: {
  initialValue: string;
  onSave: (value: string) => void;
}) {
  const [value, setValue] = useState(initialValue);
  const initialRef = useRef(initialValue);

  function handleBlur() {
    const trimmed = value.trim();
    if (trimmed !== initialRef.current.trim()) {
      onSave(trimmed);
      initialRef.current = trimmed;
    }
  }

  function handleSet(newValue: string) {
    const trimmed = newValue.trim();
    setValue(newValue);
    if (trimmed !== initialRef.current.trim()) {
      onSave(trimmed || '');
      initialRef.current = trimmed;
    }
  }

  const inputClass = 'w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold resize-y max-h-40 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void';

  return (
    <div>
      <div className="flex items-center justify-between mb-1">
        <label className="text-xs text-efi-text-secondary">Comment Template</label>
        <div className="flex gap-2">
          {value && (
            <button
              type="button"
              onClick={() => handleSet('')}
              className="text-[11px] text-efi-text-tertiary hover:text-red-400 transition-colors cursor-pointer"
            >
              Clear
            </button>
          )}
          {value !== DEFAULT_COMMENT_TEMPLATE && (
            <button
              type="button"
              onClick={() => handleSet(DEFAULT_COMMENT_TEMPLATE)}
              className="text-[11px] text-efi-text-tertiary hover:text-efi-gold-light transition-colors cursor-pointer"
            >
              Restore default
            </button>
          )}
        </div>
      </div>
      <TextArea
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onBlur={handleBlur}
        placeholder="Paste your team's comment template..."
        maxLength={2000}
        rows={2}
        className={inputClass}
      />
    </div>
  );
}

function DeadlineEditor({ room, onSave }: { room: { deadline: string }; onSave: (value: string) => void }) {
  const [editing, setEditing] = useState(false);
  const [value, setValue] = useState('');

  function startEdit() {
    if (!room.deadline) return;
    setValue(toLocalDatetimeString(new Date(room.deadline)));
    setEditing(true);
  }

  function handleSave() {
    if (!value) return;
    onSave(new Date(value).toISOString());
    setEditing(false);
  }

  if (editing) {
    return (
      <div className="space-y-2">
        <DeadlineInput value={value} onChange={setValue} />
        <div className="flex gap-2">
          <button
            type="button"
            onClick={handleSave}
            disabled={!value}
            className="px-3 py-1 rounded-lg text-xs font-medium bg-efi-gold text-efi-void hover:bg-efi-gold/80 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            Save
          </button>
          <button
            type="button"
            onClick={() => setEditing(false)}
            className="px-3 py-1 rounded-lg text-xs font-medium text-efi-text-secondary hover:text-efi-text-primary transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex items-center gap-2 text-sm">
      <span className="text-efi-text-secondary">Deadline:</span>
      <span className="text-efi-text-primary">{formatPreview(room.deadline)}</span>
      <button
        type="button"
        onClick={startEdit}
        className="text-xs text-efi-gold-light hover:text-efi-text-primary transition-colors cursor-pointer"
      >
        Edit
      </button>
    </div>
  );
}
