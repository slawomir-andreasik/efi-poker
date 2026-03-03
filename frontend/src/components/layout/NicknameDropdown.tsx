import { useEffect, useRef, useState } from 'react';
import { ChevronDown, ChevronUp, Pencil, LogOut } from 'lucide-react';
import { updateNickname } from '@/api/client';
import { useToast } from '@/components/Toast';
import { getErrorMessage } from '@/utils/error';

interface NicknameDropdownProps {
  displayName: string;
  slug: string | undefined;
  participantId: string | undefined;
  onNicknameChanged: (newNickname: string) => void;
  onLogout?: () => void;
}

export function NicknameDropdown({ displayName, slug, participantId, onNicknameChanged, onLogout }: NicknameDropdownProps) {
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState(false);
  const [newName, setNewName] = useState(displayName);
  const [saving, setSaving] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const { showToast } = useToast();

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setOpen(false);
        setEditing(false);
      }
    }
    if (open) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  // Focus input when editing starts
  useEffect(() => {
    if (editing && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [editing]);

  const canEditNickname = Boolean(slug && participantId);
  const canOpenDropdown = canEditNickname || Boolean(onLogout);

  function handleToggle() {
    if (!canOpenDropdown) return;
    setOpen(!open);
    if (open) setEditing(false);
  }

  function handleEditClick() {
    setNewName(displayName);
    setEditing(true);
  }

  async function handleSave() {
    if (!slug || !participantId) return;
    const trimmed = newName.trim();
    if (!trimmed || trimmed === displayName) {
      setEditing(false);
      return;
    }

    setSaving(true);
    try {
      const result = await updateNickname(slug, participantId, trimmed);
      onNicknameChanged(result.nickname);
      setOpen(false);
      setEditing(false);
      showToast(`Nickname changed to "${result.nickname}"`, 'success');
    } catch (err) {
      showToast(getErrorMessage(err), 'error');
    } finally {
      setSaving(false);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === 'Enter') {
      e.preventDefault();
      void handleSave();
    }
    if (e.key === 'Escape') {
      setEditing(false);
      setOpen(false);
    }
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={handleToggle}
        className={`flex items-center gap-2 text-xs transition-colors ${
          canOpenDropdown
            ? 'cursor-pointer hover:text-efi-text-primary'
            : 'cursor-default'
        }`}
      >
        <span className="text-efi-text-secondary max-w-32 sm:max-w-40 truncate">{displayName}</span>
        {canOpenDropdown && (
          open
            ? <ChevronUp className="w-3 h-3 text-efi-text-tertiary" />
            : <ChevronDown className="w-3 h-3 text-efi-text-tertiary" />
        )}
      </button>

      {open && (
        <div className="absolute right-0 top-full mt-1 glass-crystal rounded-lg shadow-xl min-w-48 z-50 overflow-hidden">
          {editing ? (
            <div className="p-3">
              <label className="text-[10px] text-efi-text-tertiary uppercase tracking-wider mb-1.5 block">
                New nickname
              </label>
              <input
                ref={inputRef}
                type="text"
                value={newName}
                onChange={e => setNewName(e.target.value)}
                onKeyDown={handleKeyDown}
                maxLength={100}
                disabled={saving}
                className="w-full px-2.5 py-1.5 rounded-md bg-white/6 border border-white/10 text-base text-efi-text-primary placeholder-efi-text-tertiary focus:outline-none focus:border-efi-gold/40 disabled:opacity-50"
                placeholder="Enter nickname"
              />
              <div className="flex gap-2 mt-2">
                <button
                  onClick={() => void handleSave()}
                  disabled={saving || !newName.trim()}
                  className="flex-1 px-2.5 py-1 rounded-md text-xs font-medium bg-efi-gold/15 text-efi-gold hover:bg-efi-gold/25 transition-colors disabled:opacity-50 cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                >
                  {saving ? 'Saving...' : 'Save'}
                </button>
                <button
                  onClick={() => setEditing(false)}
                  disabled={saving}
                  className="px-2.5 py-1 rounded-md text-xs text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/8 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                >
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            <div className="py-1">
              {canEditNickname && (
                <button
                  onClick={handleEditClick}
                  className="w-full text-left px-3 py-2 text-sm text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/6 transition-colors flex items-center gap-2 cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                >
                  <Pencil className="w-3.5 h-3.5" />
                  Change nickname
                </button>
              )}
              {onLogout && (
                <button
                  onClick={() => { setOpen(false); onLogout(); }}
                  className="w-full text-left px-3 py-2 text-sm text-efi-text-secondary hover:text-efi-error hover:bg-white/6 transition-colors flex items-center gap-2 cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  Log out
                </button>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
