import { useState, useEffect } from 'react';
import { useChangePassword } from '@/api/mutations';
import { useToast } from '@/components/Toast';
import { getErrorMessage } from '@/utils/error';
import { logger } from '@/utils/logger';
import { ButtonSpinner } from '@/components/Spinner';
import { TextInput } from '@/components/TextInput';
import { Modal } from '@/components/Modal';

interface ChangePasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
  hasPassword: boolean;
}

export function ChangePasswordModal({ isOpen, onClose, hasPassword }: ChangePasswordModalProps) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const changePassword = useChangePassword();
  const { showToast } = useToast();

  useEffect(() => {
    if (!isOpen) {
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    }
  }, [isOpen]);

  const passwordsMatch = newPassword === confirmPassword;
  const isValid = newPassword.length >= 8 && passwordsMatch && (!hasPassword || currentPassword.length >= 8);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!isValid || changePassword.isPending) return;

    try {
      await changePassword.mutateAsync({
        currentPassword: hasPassword ? currentPassword : undefined,
        newPassword,
      });
      showToast(hasPassword ? 'Password changed' : 'Password set', 'success');
      onClose();
    } catch (err) {
      logger.warn('Failed to change password:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={hasPassword ? 'Change Password' : 'Set Password'}>
      {!hasPassword && (
        <p className="text-xs text-efi-text-tertiary mb-4">
          Set a local password to log in with username and password in addition to Auth0.
        </p>
      )}

      <form onSubmit={(e) => void handleSubmit(e)} className="space-y-3">
        {hasPassword && (
          <div>
            <label htmlFor="current-password" className="block text-sm font-medium text-efi-text-secondary mb-1">
              Current Password
            </label>
            <TextInput
              id="current-password"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              maxLength={128}
              autoComplete="current-password"
              autoFocus
              className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
            />
          </div>
        )}

        <div>
          <label htmlFor="new-password" className="block text-sm font-medium text-efi-text-secondary mb-1">
            New Password
          </label>
          <TextInput
            id="new-password"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="Min. 8 characters"
            maxLength={128}
            autoComplete="new-password"
            autoFocus={!hasPassword}
            className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
          />
        </div>

        <div>
          <label htmlFor="confirm-password" className="block text-sm font-medium text-efi-text-secondary mb-1">
            Confirm Password
          </label>
          <TextInput
            id="confirm-password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Repeat new password"
            maxLength={128}
            autoComplete="new-password"
            className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
          />
          {confirmPassword && !passwordsMatch && (
            <p className="text-xs text-efi-error mt-1">Passwords do not match</p>
          )}
        </div>

        <div className="flex justify-end gap-3 pt-1">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-lg text-sm text-efi-text-secondary hover:text-efi-text-primary transition-colors cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={!isValid || changePassword.isPending}
            className="px-4 py-2 rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none flex items-center gap-2"
          >
            {changePassword.isPending ? <><ButtonSpinner /> Saving...</> : (hasPassword ? 'Change Password' : 'Set Password')}
          </button>
        </div>
      </form>
    </Modal>
  );
}
