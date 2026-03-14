export const TOOLTIP_STYLE = {
  backgroundColor: 'rgba(20,20,24,0.95)',
  border: '1px solid rgba(255,255,255,0.1)',
  borderRadius: '8px',
  color: '#fff',
} as const;

export const CURSOR_STYLE = { fill: 'rgba(255,255,255,0.04)' } as const;

export const GRID_STROKE = 'rgba(255,255,255,0.05)';

export const AXIS_TICK = { fill: 'rgba(255,255,255,0.5)', fontSize: 12 } as const;
export const AXIS_TICK_SMALL = { fill: 'rgba(255,255,255,0.5)', fontSize: 11 } as const;
export const AXIS_LINE = { stroke: 'rgba(255,255,255,0.1)' } as const;

export const LEGEND_STYLE = { fontSize: '12px', color: 'rgba(255,255,255,0.5)' } as const;

export const COLORS = {
  gold: '#d4a843',
  green: '#4ade80',
  blue: '#60a5fa',
} as const;

export function truncateTitle(title: string, maxLen = 14): string {
  return title.length > maxLen ? title.slice(0, maxLen) + '\u2026' : title;
}
