import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import {
  AXIS_LINE,
  AXIS_TICK,
  COLORS,
  CURSOR_STYLE,
  GRID_STROKE,
  TOOLTIP_STYLE,
} from './chartTheme';

interface VoteDistributionChartProps {
  distribution: Record<string, number>;
}

export function VoteDistributionChart({ distribution }: VoteDistributionChartProps) {
  const entries = Object.entries(distribution);
  if (entries.length === 0) {
    return (
      <div className="flex items-center justify-center h-[300px] text-efi-text-tertiary text-sm">
        No votes recorded
      </div>
    );
  }

  const data = entries.map(([value, count]) => ({ value, count }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE} />
        <XAxis dataKey="value" tick={AXIS_TICK} axisLine={AXIS_LINE} tickLine={false} />
        <YAxis tick={AXIS_TICK} axisLine={false} tickLine={false} allowDecimals={false} />
        <Tooltip
          contentStyle={TOOLTIP_STYLE}
          cursor={CURSOR_STYLE}
          formatter={(value) => [value ?? 0, 'Votes']}
        />
        <Bar dataKey="count" fill={COLORS.gold} radius={[4, 4, 0, 0]} name="Votes" />
      </BarChart>
    </ResponsiveContainer>
  );
}
