const btnBase =
  'text-sm font-medium border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-gold rounded-lg transition-colors active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none cursor-pointer';

export const ghostIconBtn = `inline-flex items-center gap-1.5 px-3 py-1.5 ${btnBase}`;

export const ghostLinkBtn = `px-3 py-1.5 no-underline ${btnBase}`;

export const outlineBtn = `px-4 py-2 hover:text-efi-text-primary ${btnBase}`;

export const primaryBase =
  'rounded-lg text-sm font-medium bg-gradient-to-r from-efi-gold to-efi-gold-muted text-efi-void hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity cursor-pointer active:scale-[0.98] focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void focus-visible:outline-none';

export const primaryBtn = `px-4 py-2 ${primaryBase}`;

export const primaryBtnLg = `w-full py-3 ${primaryBase}`;
