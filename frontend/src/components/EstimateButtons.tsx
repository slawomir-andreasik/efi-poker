import { memo } from 'react';
import { SP_VALUES } from '@/api/types';
import type { StoryPoints } from '@/api/types';

interface EstimateButtonsProps {
  selectedValue: StoryPoints | null;
  onSelect: (value: StoryPoints | null) => void;
  disabled?: boolean;
}

export const EstimateButtons = memo(function EstimateButtons({ selectedValue, onSelect, disabled = false }: EstimateButtonsProps) {
  return (
    <div className="flex flex-wrap gap-1.5 sm:gap-2">
      {SP_VALUES.map((value) => {
        const isSelected = selectedValue === value;
        return (
          <button
            key={value}
            onClick={() => onSelect(isSelected ? null : value)}
            disabled={disabled}
            aria-pressed={isSelected}
            className={`
              w-11 h-11 rounded-lg font-bold text-sm transition-all
              ${
                isSelected
                  ? 'bg-gradient-to-br from-efi-gold to-efi-gold-muted text-efi-void shadow-lg shadow-efi-gold/30 scale-105'
                  : 'bg-efi-obsidian border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold hover:text-efi-text-primary'
              }
              ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer active:scale-95 focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none'}
            `}
          >
            {value}
          </button>
        );
      })}
    </div>
  );
});
