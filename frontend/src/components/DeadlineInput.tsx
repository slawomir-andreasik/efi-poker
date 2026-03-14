/**
 * Reusable deadline input with auto-default, min constraint, presets, and locale preview.
 */

interface DeadlineInputProps {
  value: string;
  onChange: (value: string) => void;
}

/** Returns YYYY-MM-DDTHH:MM string for now + daysFromNow, rounded to next full hour. */
export function getDefaultDeadline(daysFromNow: number): string {
  const d = new Date();
  d.setDate(d.getDate() + daysFromNow);
  // Round to next full hour
  if (d.getMinutes() > 0 || d.getSeconds() > 0 || d.getMilliseconds() > 0) {
    d.setHours(d.getHours() + 1, 0, 0, 0);
  }
  return toLocalDatetimeString(d);
}

/** Returns YYYY-MM-DDTHH:MM string for now + hoursFromNow, rounded to nearest 15 min. */
export function getDefaultDeadlineHours(hoursFromNow: number): string {
  const d = new Date();
  d.setTime(d.getTime() + hoursFromNow * 60 * 60 * 1000);
  // Round up to nearest 15 min
  const mins = d.getMinutes();
  const remainder = mins % 15;
  if (remainder > 0 || d.getSeconds() > 0 || d.getMilliseconds() > 0) {
    d.setMinutes(mins + (15 - remainder), 0, 0);
  }
  return toLocalDatetimeString(d);
}

export function toLocalDatetimeString(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function getNowMin(): string {
  return toLocalDatetimeString(new Date());
}

export function formatPreview(value: string): string {
  if (!value) return '';
  const d = new Date(value);
  if (isNaN(d.getTime())) return '';
  return new Intl.DateTimeFormat('pl-PL', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(d);
}

type Preset =
  | { label: string; type: 'hours'; hours: number }
  | { label: string; type: 'days'; days: number };

const presets: Preset[] = [
  { label: '+3h', type: 'hours', hours: 3 },
  { label: '+8h', type: 'hours', hours: 8 },
  { label: '+1d', type: 'days', days: 1 },
  { label: '+3d', type: 'days', days: 3 },
  { label: '+7d', type: 'days', days: 7 },
];

function applyPreset(p: Preset): string {
  return p.type === 'hours' ? getDefaultDeadlineHours(p.hours) : getDefaultDeadline(p.days);
}

export function DeadlineInput({ value, onChange }: DeadlineInputProps) {
  const preview = formatPreview(value);

  return (
    <div>
      <label className="block text-xs text-efi-text-secondary mb-1">
        Deadline <span className="text-efi-error">*</span>
      </label>
      <input
        type="datetime-local"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        min={getNowMin()}
        required
        className="w-full rounded-lg bg-efi-well border border-efi-gold-light/20 px-3 py-2 text-efi-text-primary text-base focus:outline-none focus:border-efi-gold focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
      />
      <div className="flex items-center gap-2 mt-1.5">
        {presets.map((p) => (
          <button
            key={p.label}
            type="button"
            onClick={() => onChange(applyPreset(p))}
            className="px-2 py-0.5 text-xs rounded border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold hover:text-efi-text-primary transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none"
          >
            {p.label}
          </button>
        ))}
        {preview && (
          <span className="text-xs text-efi-text-secondary ml-auto">{preview}</span>
        )}
      </div>
    </div>
  );
}
