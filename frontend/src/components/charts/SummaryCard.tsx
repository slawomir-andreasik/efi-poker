export function SummaryCard({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="glass-whisper rounded-xl p-4 text-center">
      <p className="text-xs text-efi-text-tertiary uppercase tracking-wide font-medium">{label}</p>
      <p className="text-2xl font-bold text-efi-text-primary mt-1">{value}</p>
    </div>
  );
}
