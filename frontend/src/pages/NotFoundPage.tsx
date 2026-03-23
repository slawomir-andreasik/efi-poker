import { Link } from 'react-router-dom';

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
        className="px-6 py-3 rounded-lg font-medium text-sm bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 transition-opacity no-underline active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none animate-[fade-in-up_0.6s_ease-out_0.3s_both] motion-reduce:animate-none"
      >
        Go Home
      </Link>
    </div>
  );
}
