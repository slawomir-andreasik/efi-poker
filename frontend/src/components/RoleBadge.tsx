interface RoleBadgeProps {
  isAdmin: boolean;
}

export function RoleBadge({ isAdmin }: RoleBadgeProps) {
  return (
    <span
      className={`text-[10px] font-bold uppercase px-1.5 py-0.5 rounded border ${
        isAdmin
          ? 'bg-efi-gold/20 text-efi-gold-light border-efi-gold/30'
          : 'bg-white/8 text-efi-text-secondary border-white/10'
      }`}
    >
      {isAdmin ? 'Admin' : 'Voter'}
    </span>
  );
}
