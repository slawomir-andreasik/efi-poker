interface ProgressBarProps {
  voted: number;
  total: number;
}

export function ProgressBar({ voted, total }: ProgressBarProps) {
  const percentage = total > 0 ? Math.round((voted / total) * 100) : 0;
  const allVoted = voted === total && total > 0;

  return (
    <div className="flex items-center gap-2">
      <div className="w-20 h-2 bg-efi-well rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-500 ${allVoted ? 'bg-efi-success' : 'bg-gradient-to-r from-efi-gold to-efi-gold-light'}`}
          style={{ width: `${percentage}%` }}
        />
      </div>
      <span className={`text-xs font-medium whitespace-nowrap ${allVoted ? 'text-efi-success' : 'text-efi-text-secondary'}`}>
        {voted}/{total}
      </span>
    </div>
  );
}
