import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { TextInput } from '@/components/TextInput';
import { primaryBase } from '@/styles/buttons';
import { logger } from '@/utils/logger';

export function JoinByCodeForm() {
  const navigate = useNavigate();
  const [code, setCode] = useState('');

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = code.trim();
    if (!trimmed) return;

    if (/^[A-Z0-9]{3}-[A-Z0-9]{3}$/i.test(trimmed)) {
      logger.debug(`JoinByCodeForm: room code detected, navigating to /r/${trimmed.toUpperCase()}`);
      void navigate(`/r/${trimmed.toUpperCase()}`);
    } else {
      logger.debug(`JoinByCodeForm: project slug detected, navigating to /p/${trimmed}/join`);
      void navigate(`/p/${trimmed}/join`);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      <TextInput
        type="text"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        placeholder="Room code or project slug"
        className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-2.5 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
      />
      <button
        type="submit"
        disabled={!code.trim()}
        className={`${primaryBase} px-4 py-2.5 whitespace-nowrap`}
      >
        Join
      </button>
    </form>
  );
}
