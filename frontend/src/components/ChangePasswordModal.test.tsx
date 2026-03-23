import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/test/helpers';
import { ChangePasswordModal } from './ChangePasswordModal';

describe('ChangePasswordModal', () => {
  it('should not render when closed', () => {
    renderWithProviders(
      <ChangePasswordModal isOpen={false} onClose={() => {}} hasPassword={true} />,
    );
    expect(screen.queryByText('Change Password')).not.toBeInTheDocument();
  });

  it('should render change password form when user has password', () => {
    renderWithProviders(
      <ChangePasswordModal isOpen={true} onClose={() => {}} hasPassword={true} />,
    );
    expect(screen.getByRole('heading', { name: 'Change Password' })).toBeInTheDocument();
    expect(screen.getByLabelText('Current Password')).toBeInTheDocument();
    expect(screen.getByLabelText('New Password')).toBeInTheDocument();
    expect(screen.getByLabelText('Confirm Password')).toBeInTheDocument();
  });

  it('should render set password form when user has no password', () => {
    renderWithProviders(
      <ChangePasswordModal isOpen={true} onClose={() => {}} hasPassword={false} />,
    );
    expect(screen.getByRole('heading', { name: 'Set Password' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Current Password')).not.toBeInTheDocument();
    expect(screen.getByText(/Set a local password/)).toBeInTheDocument();
  });

  it('should disable submit when passwords do not match', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ChangePasswordModal isOpen={true} onClose={() => {}} hasPassword={false} />,
    );

    await user.type(screen.getByLabelText('New Password'), 'password123');
    await user.type(screen.getByLabelText('Confirm Password'), 'different12');

    expect(screen.getByRole('button', { name: 'Set Password' })).toBeDisabled();
    expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
  });

  it('should disable submit when new password is too short', async () => {
    const user = userEvent.setup();
    renderWithProviders(
      <ChangePasswordModal isOpen={true} onClose={() => {}} hasPassword={false} />,
    );

    await user.type(screen.getByLabelText('New Password'), 'short');
    await user.type(screen.getByLabelText('Confirm Password'), 'short');

    expect(screen.getByRole('button', { name: 'Set Password' })).toBeDisabled();
  });

  it('should call onClose when cancel is clicked', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    renderWithProviders(<ChangePasswordModal isOpen={true} onClose={onClose} hasPassword={true} />);

    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onClose).toHaveBeenCalled();
  });

  it('should call onClose on escape key', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    renderWithProviders(<ChangePasswordModal isOpen={true} onClose={onClose} hasPassword={true} />);

    await user.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalled();
  });
});
