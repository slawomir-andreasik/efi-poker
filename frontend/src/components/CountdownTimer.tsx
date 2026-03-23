import { useCountdown } from '@/hooks/useCountdown';

interface CountdownTimerProps {
  deadline: string;
}

export function CountdownTimer({ deadline }: CountdownTimerProps) {
  const { days, hours, minutes, seconds, isExpired } = useCountdown(deadline);

  if (isExpired) {
    return <span className="text-efi-error font-semibold">Expired</span>;
  }

  const parts: string[] = [];
  if (days > 0) parts.push(`${days}d`);
  if (hours > 0 || days > 0) parts.push(`${hours}h`);
  parts.push(`${minutes}m`);
  if (days === 0) parts.push(`${seconds}s`);

  return <span className="text-efi-champagne font-mono font-semibold">{parts.join(' ')}</span>;
}
