import { Link } from 'react-router-dom';

interface NotFoundStateProps {
  message: string;
  backTo?: string;
  backLabel?: string;
}

export function NotFoundState({
  message,
  backTo = '/',
  backLabel = 'Back to Home',
}: NotFoundStateProps) {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
      <p className="text-efi-text-primary font-medium">{message}</p>
      <Link
        to={backTo}
        className="text-sm text-efi-gold-light hover:text-efi-gold transition-colors no-underline hover:underline"
      >
        {backLabel}
      </Link>
    </div>
  );
}
