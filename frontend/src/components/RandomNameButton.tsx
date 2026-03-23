import { Dices } from 'lucide-react';
import { generateNickname } from '@/utils/nameGenerator';

interface RandomNameButtonProps {
  onGenerate: (name: string) => void;
  generator?: () => string;
}

export function RandomNameButton({
  onGenerate,
  generator = generateNickname,
}: RandomNameButtonProps) {
  return (
    <button
      type="button"
      onClick={() => onGenerate(generator())}
      title="Generate random name"
      className="p-2 rounded-lg text-efi-text-tertiary hover:text-efi-text-primary hover:bg-white/8 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
    >
      <Dices className="w-4 h-4" />
    </button>
  );
}
