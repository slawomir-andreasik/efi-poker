import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { useRegister } from '@/api/mutations';
import { ButtonSpinner } from '@/components/Spinner';
import { TextInput } from '@/components/TextInput';
import { useToast } from '@/components/Toast';
import { useAuthConfig } from '@/hooks/useAuthConfig';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { getErrorMessage } from '@/utils/error';

export function RegisterPage() {
  useDocumentTitle('Register');
  const { registrationEnabled } = useAuthConfig();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const register = useRegister();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');

  if (!registrationEnabled) {
    return <Navigate to="/login" replace />;
  }

  const passwordTooShort = password.length > 0 && password.length < 8;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!username.trim() || !password || password.length < 8) return;

    try {
      await register.mutateAsync({
        username: username.trim(),
        password,
        email: email.trim() || undefined,
      });
      navigate('/', { replace: true });
    } catch (err) {
      showToast(getErrorMessage(err));
    }
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] px-4">
      <div className="text-center mb-8 animate-[fade-in-up_0.6s_ease-out] motion-reduce:animate-none">
        <h1 className="text-3xl sm:text-4xl font-bold mb-1 bg-gradient-to-r from-efi-gold via-efi-gold-light to-efi-gold-muted bg-clip-text text-transparent">
          EFI Poker
        </h1>
        <p className="text-xs font-bold text-efi-gold-light/40 uppercase tracking-[0.2em]">
          Estimate. Focus. Improve.
        </p>
      </div>

      <div className="w-full max-w-sm animate-[fade-in-up_0.6s_ease-out_0.15s_both] motion-reduce:animate-none">
        <div className="glass-frost rounded-2xl p-4 sm:p-6">
          <h2 className="text-lg font-semibold text-efi-text-primary mb-4">Create account</h2>

          <form onSubmit={(e) => void handleSubmit(e)} className="space-y-3">
            <div>
              <label
                htmlFor="reg-username"
                className="block text-sm font-medium text-efi-text-secondary mb-1"
              >
                Username
              </label>
              <TextInput
                id="reg-username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. alice"
                maxLength={100}
                minLength={3}
                autoFocus
                autoComplete="username"
                className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
              />
            </div>

            <div>
              <label
                htmlFor="reg-password"
                className="block text-sm font-medium text-efi-text-secondary mb-1"
              >
                Password
              </label>
              <input
                id="reg-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Min. 8 characters"
                maxLength={128}
                autoComplete="new-password"
                className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
              />
              {passwordTooShort && (
                <p className="text-xs text-efi-error mt-1">
                  Password must be at least 8 characters
                </p>
              )}
            </div>

            <div>
              <label
                htmlFor="reg-email"
                className="block text-sm font-medium text-efi-text-secondary mb-1"
              >
                Email <span className="text-efi-text-tertiary">(optional)</span>
              </label>
              <TextInput
                id="reg-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="alice@example.com"
                maxLength={254}
                autoComplete="email"
                className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
              />
            </div>

            <button
              type="submit"
              disabled={register.isPending || !username.trim() || !password || password.length < 8}
              className="w-full py-3 rounded-lg font-medium text-sm bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none flex items-center justify-center gap-2"
            >
              {register.isPending ? (
                <>
                  <ButtonSpinner /> Creating account...
                </>
              ) : (
                'Create account'
              )}
            </button>
          </form>
        </div>

        <div className="flex items-center justify-center gap-4 mt-4 text-sm">
          <Link
            to="/login"
            className="text-efi-gold hover:text-efi-gold-light transition-colors no-underline"
          >
            Already have an account? Log in
          </Link>
        </div>
      </div>
    </div>
  );
}
