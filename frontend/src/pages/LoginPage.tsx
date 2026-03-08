import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useLogin } from '@/api/mutations';
import { useAuthConfig } from '@/hooks/useAuthConfig';
import { useToast } from '@/components/Toast';
import { getErrorMessage } from '@/utils/error';
import { ButtonSpinner } from '@/components/Spinner';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { TextInput } from '@/components/TextInput';

export function LoginPage() {
  useDocumentTitle('Login');
  const navigate = useNavigate();
  const { showToast } = useToast();
  const { auth0Enabled, registrationEnabled, ldapEnabled } = useAuthConfig();
  const login = useLogin();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!username.trim() || !password) return;

    try {
      await login.mutateAsync({ username: username.trim(), password });
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
          <h2 className={`text-lg font-semibold text-efi-text-primary ${ldapEnabled ? 'mb-1' : 'mb-4'}`}>Log in</h2>
          {ldapEnabled && (
            <p className="text-xs text-efi-text-tertiary mb-3">Use your corporate credentials</p>
          )}

          <form onSubmit={(e) => void handleSubmit(e)} className="space-y-3">
            <div>
              <label htmlFor="login-username" className="block text-sm font-medium text-efi-text-secondary mb-1">
                Username
              </label>
              <TextInput
                id="login-username"
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder={ldapEnabled ? 'Corporate username (uid)' : 'e.g. alice'}
                maxLength={100}
                autoFocus
                autoComplete="username"
                className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
              />
            </div>

            <div>
              <label htmlFor="login-password" className="block text-sm font-medium text-efi-text-secondary mb-1">
                Password
              </label>
              <input
                id="login-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Your password"
                maxLength={128}
                autoComplete="current-password"
                className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
              />
            </div>

            <button
              type="submit"
              disabled={login.isPending || !username.trim() || !password}
              className="w-full py-3 rounded-lg font-medium text-sm bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none flex items-center justify-center gap-2"
            >
              {login.isPending ? <><ButtonSpinner /> Logging in...</> : 'Log in'}
            </button>
          </form>

          {auth0Enabled && (
            <>
              <div className="flex items-center gap-3 my-4">
                <div className="flex-1 h-px bg-white/10" />
                <span className="text-xs text-efi-text-tertiary">or</span>
                <div className="flex-1 h-px bg-white/10" />
              </div>

              <a
                href="/api/v1/auth/oauth2/authorize/auth0"
                className="flex items-center justify-center gap-2 w-full py-3 rounded-lg text-sm font-medium border border-efi-gold-light/20 text-efi-text-primary hover:border-efi-gold/30 hover:bg-white/5 transition-colors no-underline focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
              >
                Continue with Auth0
              </a>
            </>
          )}
        </div>

        <div className="flex items-center justify-center gap-4 mt-4 text-sm">
          {registrationEnabled && (
            <Link
              to="/register"
              className="text-efi-gold hover:text-efi-gold-light transition-colors no-underline"
            >
              Create account
            </Link>
          )}
          <Link
            to="/"
            className="text-efi-text-tertiary hover:text-efi-text-secondary transition-colors no-underline"
          >
            Continue as guest
          </Link>
        </div>
      </div>
    </div>
  );
}
