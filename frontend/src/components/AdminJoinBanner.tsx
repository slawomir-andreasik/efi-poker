import { useState } from 'react';
import { useAdminJoinMutation } from '@/api/mutations';
import { ButtonSpinner } from '@/components/Spinner';
import { TextInput } from '@/components/TextInput';
import { useToast } from '@/components/Toast';
import { primaryBtn } from '@/styles/buttons';
import { getErrorMessage } from '@/utils/error';
import { logger } from '@/utils/logger';

interface AdminJoinBannerProps {
  slug: string;
  onJoined: () => void;
}

export function AdminJoinBanner({ slug, onJoined }: AdminJoinBannerProps) {
  const [joinNickname, setJoinNickname] = useState('');
  const adminJoin = useAdminJoinMutation(slug);
  const { showToast } = useToast();

  async function handleAdminJoin(e: React.FormEvent) {
    e.preventDefault();
    if (!joinNickname.trim()) return;
    logger.info('Admin joining as voter');
    try {
      await adminJoin.mutateAsync(joinNickname.trim());
      onJoined();
    } catch (err) {
      logger.warn('Failed to join as voter:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  return (
    <form
      onSubmit={(e) => void handleAdminJoin(e)}
      className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 glass-gold rounded-xl p-4 mb-6"
    >
      <p className="text-sm text-efi-gold-light font-medium sm:mr-2">Join as voter to estimate</p>
      <TextInput
        type="text"
        value={joinNickname}
        onChange={(e) => setJoinNickname(e.target.value)}
        placeholder="Your nickname"
        maxLength={100}
        className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
      />
      <button
        type="submit"
        disabled={adminJoin.isPending || !joinNickname.trim()}
        className={`${primaryBtn} flex items-center justify-center gap-2 whitespace-nowrap`}
      >
        {adminJoin.isPending ? (
          <>
            <ButtonSpinner /> Joining...
          </>
        ) : (
          'Join & Vote'
        )}
      </button>
    </form>
  );
}
