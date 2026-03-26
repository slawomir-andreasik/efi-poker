import { useQuery } from '@tanstack/react-query';
import { KeyRound, Pencil, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import {
  useAdminCreateUser,
  useAdminDeleteUser,
  useAdminResetPassword,
  useAdminUpdateUser,
} from '@/api/mutations';
import { adminApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import type { AdminUserResponse, UserRole } from '@/api/types';
import { ButtonSpinner, Spinner } from '@/components/Spinner';
import { TextInput } from '@/components/TextInput';
import { useToast } from '@/components/Toast';
import { useCurrentUser } from '@/hooks/useCurrentUser';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { primaryBase, primaryBtn } from '@/styles/buttons';
import { inputSm } from '@/styles/inputs';
import { getErrorMessage } from '@/utils/error';
import { logger } from '@/utils/logger';

const PAGE_SIZE = 20;

export function AdminUsersPage() {
  useDocumentTitle('Admin - Users');
  const { showToast } = useToast();
  const { isAdmin, isLoading: userLoading } = useCurrentUser();

  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');

  // Create user form
  const [showCreate, setShowCreate] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [newRole, setNewRole] = useState<UserRole>('USER');

  // Edit state
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editRole, setEditRole] = useState<UserRole>('USER');
  const [editEmail, setEditEmail] = useState('');

  // Password reset
  const [resetPasswordId, setResetPasswordId] = useState<string | null>(null);
  const [resetPasswordValue, setResetPasswordValue] = useState('');

  // Delete confirm
  const [deletingId, setDeletingId] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: queryKeys.admin.users(page, PAGE_SIZE, search || undefined),
    queryFn: () => adminApi.users(page, PAGE_SIZE, search || undefined),
    enabled: isAdmin,
  });

  const createUser = useAdminCreateUser();
  const updateUser = useAdminUpdateUser();
  const deleteUser = useAdminDeleteUser();
  const resetPassword = useAdminResetPassword();

  if (userLoading) {
    return (
      <div className="flex justify-center py-20">
        <Spinner className="h-8 w-8" />
      </div>
    );
  }

  if (!isAdmin) {
    return <Navigate to="/login" replace />;
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    setSearch(searchInput.trim());
    setPage(0);
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    try {
      await createUser.mutateAsync({
        username: newUsername.trim(),
        password: newPassword,
        email: newEmail.trim() || undefined,
        role: newRole,
      });
      showToast(`User "${newUsername.trim()}" created`, 'success');
      setShowCreate(false);
      setNewUsername('');
      setNewPassword('');
      setNewEmail('');
      setNewRole('USER');
    } catch (err) {
      logger.warn('Failed to create user:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  function startEdit(user: AdminUserResponse) {
    setEditingId(user.id);
    setEditRole(user.role);
    setEditEmail(user.email ?? '');
  }

  async function handleUpdate(id: string) {
    try {
      await updateUser.mutateAsync({
        id,
        body: {
          role: editRole,
          email: editEmail.trim() || undefined,
        },
      });
      setEditingId(null);
      showToast('User updated', 'success');
    } catch (err) {
      logger.warn('Failed to update user:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleResetPassword(id: string) {
    if (resetPasswordValue.length < 8) return;
    try {
      await resetPassword.mutateAsync({ id, body: { newPassword: resetPasswordValue } });
      setResetPasswordId(null);
      setResetPasswordValue('');
      showToast('Password reset', 'success');
    } catch (err) {
      logger.warn('Failed to reset password:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  async function handleDelete(id: string) {
    try {
      await deleteUser.mutateAsync(id);
      setDeletingId(null);
      showToast('User deleted', 'success');
    } catch (err) {
      logger.warn('Failed to delete user:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  const users = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 py-4 sm:py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-xl font-bold text-efi-text-primary">User Management</h1>
        <button type="button" onClick={() => setShowCreate(!showCreate)} className={primaryBtn}>
          {showCreate ? 'Cancel' : '+ New User'}
        </button>
      </div>

      {/* Create user form */}
      {showCreate && (
        <form
          onSubmit={(e) => void handleCreate(e)}
          className="glass-frost rounded-2xl p-4 sm:p-6 mb-6"
        >
          <h2 className="text-base font-semibold text-efi-text-primary mb-3">New User</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label
                htmlFor="new-username"
                className="block text-sm font-medium text-efi-text-secondary mb-1"
              >
                Username
              </label>
              <TextInput
                id="new-username"
                type="text"
                value={newUsername}
                onChange={(e) => setNewUsername(e.target.value)}
                placeholder="username"
                maxLength={100}
                minLength={3}
                autoFocus
                className={inputSm}
              />
            </div>
            <div>
              <label
                htmlFor="new-password"
                className="block text-sm font-medium text-efi-text-secondary mb-1"
              >
                Password
              </label>
              <input
                id="new-password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Min. 8 characters"
                maxLength={128}
                autoComplete="new-password"
                className={inputSm}
              />
            </div>
            <div>
              <label
                htmlFor="new-email"
                className="block text-sm font-medium text-efi-text-secondary mb-1"
              >
                Email <span className="text-efi-text-tertiary">(optional)</span>
              </label>
              <TextInput
                id="new-email"
                type="email"
                value={newEmail}
                onChange={(e) => setNewEmail(e.target.value)}
                placeholder="user@example.com"
                maxLength={254}
                className={inputSm}
              />
            </div>
            <div>
              <label
                htmlFor="new-role"
                className="block text-sm font-medium text-efi-text-secondary mb-1"
              >
                Role
              </label>
              <select
                id="new-role"
                value={newRole}
                onChange={(e) => setNewRole(e.target.value as UserRole)}
                className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary text-base focus:outline-none focus:border-efi-gold transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
              >
                <option value="USER">USER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>
          </div>
          <button
            type="submit"
            disabled={createUser.isPending || !newUsername.trim() || newPassword.length < 8}
            className={`${primaryBase} mt-4 px-6 py-2 flex items-center gap-2`}
          >
            {createUser.isPending ? (
              <>
                <ButtonSpinner /> Creating...
              </>
            ) : (
              'Create User'
            )}
          </button>
        </form>
      )}

      {/* Search */}
      <form onSubmit={handleSearch} className="flex gap-2 mb-4">
        <TextInput
          type="text"
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          placeholder="Search by username or email..."
          maxLength={100}
          className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
        />
        <button
          type="submit"
          className="px-4 py-2 rounded-lg text-sm font-medium border border-efi-gold-light/20 text-efi-text-primary hover:border-efi-gold/30 hover:bg-white/5 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
        >
          Search
        </button>
        {search && (
          <button
            type="button"
            onClick={() => {
              setSearch('');
              setSearchInput('');
              setPage(0);
            }}
            className="px-3 py-2 rounded-lg text-sm text-efi-text-tertiary hover:text-efi-text-secondary transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            Clear
          </button>
        )}
      </form>

      {/* Users table */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <Spinner className="h-6 w-6" />
        </div>
      ) : users.length === 0 ? (
        <div className="text-center py-12 bg-white/3 rounded-2xl border border-dashed border-white/8">
          <p className="text-efi-text-secondary">
            {search ? 'No users match your search.' : 'No users found.'}
          </p>
        </div>
      ) : (
        <div className="glass-frost rounded-2xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-white/10 text-left">
                  <th className="px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wider">
                    Username
                  </th>
                  <th className="px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wider hidden sm:table-cell">
                    Email
                  </th>
                  <th className="px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wider">
                    Role
                  </th>
                  <th className="px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wider hidden sm:table-cell">
                    Provider
                  </th>
                  <th className="px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wider hidden md:table-cell">
                    Created
                  </th>
                  <th className="px-4 py-3 text-xs font-semibold text-efi-text-secondary uppercase tracking-wider w-28">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.id} className="border-b border-white/5 hover:bg-white/3">
                    <td className="px-4 py-3 text-efi-text-primary font-medium">{user.username}</td>
                    <td className="px-4 py-3 text-efi-text-secondary hidden sm:table-cell">
                      {editingId === user.id ? (
                        <TextInput
                          type="email"
                          value={editEmail}
                          onChange={(e) => setEditEmail(e.target.value)}
                          className="rounded bg-efi-well border border-efi-gold-light/20 px-2 py-1 text-efi-text-primary text-base w-full focus:outline-none focus:border-efi-gold"
                        />
                      ) : (
                        (user.email ?? <span className="text-efi-text-tertiary">-</span>)
                      )}
                    </td>
                    <td className="px-4 py-3">
                      {editingId === user.id ? (
                        <select
                          value={editRole}
                          onChange={(e) => setEditRole(e.target.value as UserRole)}
                          className="rounded bg-efi-well border border-efi-gold-light/20 px-2 py-1 text-efi-text-primary text-base cursor-pointer focus:outline-none focus:border-efi-gold"
                        >
                          <option value="USER">USER</option>
                          <option value="ADMIN">ADMIN</option>
                        </select>
                      ) : (
                        <span
                          className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${
                            user.role === 'ADMIN'
                              ? 'bg-efi-gold/20 text-efi-gold-light border-efi-gold/30'
                              : 'bg-white/8 text-efi-text-secondary border-white/10'
                          }`}
                        >
                          {user.role}
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-efi-text-tertiary text-xs hidden sm:table-cell">
                      {user.authProvider}
                    </td>
                    <td className="px-4 py-3 text-efi-text-tertiary text-xs hidden md:table-cell">
                      {new Date(user.createdAt).toLocaleDateString()}
                    </td>
                    <td className="px-4 py-3">
                      {editingId === user.id ? (
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() => void handleUpdate(user.id)}
                            disabled={updateUser.isPending}
                            className="text-xs px-2 py-1 rounded text-efi-success hover:bg-efi-success/10 transition-colors cursor-pointer disabled:opacity-50 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            {updateUser.isPending ? 'Saving...' : 'Save'}
                          </button>
                          <button
                            type="button"
                            onClick={() => setEditingId(null)}
                            className="text-xs px-2 py-1 rounded text-efi-text-tertiary hover:text-efi-text-secondary transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            Cancel
                          </button>
                        </div>
                      ) : deletingId === user.id ? (
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() => void handleDelete(user.id)}
                            disabled={deleteUser.isPending}
                            className="text-xs px-2 py-1 rounded text-efi-error hover:bg-efi-error/10 transition-colors cursor-pointer disabled:opacity-50 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            {deleteUser.isPending ? 'Deleting...' : 'Confirm'}
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingId(null)}
                            className="text-xs px-2 py-1 rounded text-efi-text-tertiary hover:text-efi-text-secondary transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            Cancel
                          </button>
                        </div>
                      ) : resetPasswordId === user.id ? (
                        <div className="flex items-center gap-1">
                          <TextInput
                            type="password"
                            value={resetPasswordValue}
                            onChange={(e) => setResetPasswordValue(e.target.value)}
                            placeholder="New password"
                            maxLength={128}
                            autoComplete="new-password"
                            className="rounded bg-efi-well border border-efi-gold-light/20 px-2 py-1 text-efi-text-primary text-base w-28 focus:outline-none focus:border-efi-gold"
                          />
                          <button
                            type="button"
                            onClick={() => void handleResetPassword(user.id)}
                            disabled={resetPassword.isPending || resetPasswordValue.length < 8}
                            className="text-xs px-2 py-1 rounded text-efi-success hover:bg-efi-success/10 transition-colors cursor-pointer disabled:opacity-50 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            {resetPassword.isPending ? 'Saving...' : 'Set'}
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              setResetPasswordId(null);
                              setResetPasswordValue('');
                            }}
                            className="text-xs px-2 py-1 rounded text-efi-text-tertiary hover:text-efi-text-secondary transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            Cancel
                          </button>
                        </div>
                      ) : (
                        <div className="flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() => startEdit(user)}
                            title="Edit user"
                            className="p-1.5 rounded text-efi-text-tertiary hover:text-efi-text-primary hover:bg-white/5 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            <Pencil className="w-3.5 h-3.5" />
                          </button>
                          {user.authProvider !== 'LDAP' && (
                            <button
                              type="button"
                              onClick={() => setResetPasswordId(user.id)}
                              title="Reset password"
                              className="p-1.5 rounded text-efi-text-tertiary hover:text-efi-text-primary hover:bg-white/5 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                            >
                              <KeyRound className="w-3.5 h-3.5" />
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => setDeletingId(user.id)}
                            title="Delete user"
                            className="p-1.5 rounded text-efi-text-tertiary hover:text-red-400 hover:bg-white/5 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between px-4 py-3 border-t border-white/10">
              <span className="text-xs text-efi-text-tertiary">
                Page {page + 1} of {totalPages} ({data?.totalElements ?? 0} users)
              </span>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="text-xs px-3 py-1 rounded border border-white/10 text-efi-text-secondary hover:text-efi-text-primary hover:border-white/20 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                >
                  Previous
                </button>
                <button
                  type="button"
                  onClick={() => setPage((p) => p + 1)}
                  disabled={page >= totalPages - 1}
                  className="text-xs px-3 py-1 rounded border border-white/10 text-efi-text-secondary hover:text-efi-text-primary hover:border-white/20 disabled:opacity-50 disabled:cursor-not-allowed transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
