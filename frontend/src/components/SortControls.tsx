import { ArrowUp } from 'lucide-react';
import type { SortDirection, SortField } from '@/hooks/useSortedTasks';

interface SortControlsProps {
  sortField: SortField;
  sortDirection: SortDirection;
  onlyUnestimated: boolean;
  unestimatedCount: number;
  onSortFieldChange: (field: SortField) => void;
  onSortDirectionChange: (direction: SortDirection) => void;
  onOnlyUnestimatedChange: (value: boolean) => void;
}

const SORT_OPTIONS: { value: SortField; label: string }[] = [
  { value: 'title', label: 'Name' },
  { value: 'progress', label: 'Progress' },
];

export function SortControls({
  sortField,
  sortDirection,
  onlyUnestimated,
  unestimatedCount,
  onSortFieldChange,
  onSortDirectionChange,
  onOnlyUnestimatedChange,
}: SortControlsProps) {
  return (
    <div className="flex flex-wrap items-center gap-2 mb-4">
      {/* Segmented buttons */}
      <div className="flex rounded-lg glass-whisper overflow-hidden">
        {SORT_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            type="button"
            onClick={() => onSortFieldChange(opt.value)}
            className={`px-3 py-1.5 text-xs font-medium transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none ${
              sortField === opt.value
                ? 'bg-white/10 text-efi-text-primary'
                : 'text-efi-text-secondary hover:text-efi-text-primary hover:bg-white/8'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      {/* Direction toggle */}
      <button
        type="button"
        onClick={() => onSortDirectionChange(sortDirection === 'asc' ? 'desc' : 'asc')}
        className="flex items-center gap-1 px-2 py-1.5 text-xs text-efi-text-secondary hover:text-efi-text-primary rounded-lg border border-efi-gold-light/20 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
        title={sortDirection === 'asc' ? 'Ascending' : 'Descending'}
        aria-label={`Sort ${sortDirection === 'asc' ? 'ascending' : 'descending'}`}
      >
        <ArrowUp
          className={`w-3.5 h-3.5 transition-transform ${sortDirection === 'desc' ? 'rotate-180' : ''}`}
        />
      </button>

      {/* Only unestimated filter */}
      <button
        type="button"
        onClick={() => onOnlyUnestimatedChange(!onlyUnestimated)}
        className={`px-3 py-1.5 text-xs font-medium rounded-lg border transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none ${
          onlyUnestimated
            ? 'bg-white/10 text-efi-text-primary border-white/15'
            : 'text-efi-text-secondary border-white/10 hover:text-efi-text-primary hover:bg-white/8'
        }`}
      >
        Only unestimated{unestimatedCount > 0 ? ` (${unestimatedCount})` : ''}
      </button>
    </div>
  );
}
