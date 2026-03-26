import { Link } from 'react-router-dom';
import { primaryBase } from '@/styles/buttons';

export function NotFoundPage() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] px-4">
      <h1 className="text-6xl sm:text-8xl font-bold bg-gradient-to-r from-efi-gold via-efi-gold-light to-efi-gold-muted bg-clip-text text-transparent mb-4 animate-[fade-in-up_0.6s_ease-out] motion-reduce:animate-none">
        404
      </h1>
      <p className="text-xl text-efi-text-primary mb-2 animate-[fade-in-up_0.6s_ease-out_0.1s_both] motion-reduce:animate-none">
        Page Not Found
      </p>
      <p className="text-efi-text-secondary mb-8 animate-[fade-in-up_0.6s_ease-out_0.2s_both] motion-reduce:animate-none">
        The page you are looking for does not exist.
      </p>
      <Link
        to="/"
        className={`${primaryBase} px-6 py-3 no-underline animate-[fade-in-up_0.6s_ease-out_0.3s_both] motion-reduce:animate-none`}
      >
        Go Home
      </Link>
    </div>
  );
}
