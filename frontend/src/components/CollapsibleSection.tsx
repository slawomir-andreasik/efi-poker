import type { LucideIcon } from 'lucide-react';
import { type ReactNode, useState } from 'react';

interface CollapsibleSectionProps {
  icon: LucideIcon;
  label: string;
  defaultOpen?: boolean;
  children: ReactNode;
}

export function CollapsibleSection({
  icon: Icon,
  label,
  defaultOpen = false,
  children,
}: CollapsibleSectionProps) {
  const [open, setOpen] = useState(defaultOpen);

  return (
    <div className="glass-frost rounded-xl p-4 border border-efi-gold-light/10">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex items-center gap-2 text-xs font-semibold text-efi-text-secondary uppercase tracking-wider cursor-pointer hover:text-efi-text-primary transition-colors w-full focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none rounded"
      >
        <Icon className="w-3.5 h-3.5" />
        {label}
        <svg
          className={`w-3 h-3 ml-auto transition-transform ${open ? 'rotate-180' : ''}`}
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          aria-hidden="true"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {open && <div className="mt-4">{children}</div>}
    </div>
  );
}
