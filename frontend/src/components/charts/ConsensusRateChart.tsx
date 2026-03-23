import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { RoomStatsEntry } from '@/api/types';
import {
  AXIS_LINE,
  AXIS_TICK,
  AXIS_TICK_SMALL,
  COLORS,
  CURSOR_STYLE,
  GRID_STROKE,
  TOOLTIP_STYLE,
  truncateTitle,
} from './chartTheme';

interface ConsensusRateChartProps {
  rooms: RoomStatsEntry[];
}

export function ConsensusRateChart({ rooms }: ConsensusRateChartProps) {
  if (rooms.length === 0) {
    return (
      <div className="flex items-center justify-center h-[300px] text-efi-text-tertiary text-sm">
        No room data available
      </div>
    );
  }

  const data = rooms.map((r) => ({
    name: truncateTitle(r.title),
    rate: Math.round(r.consensusRate),
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE} />
        <XAxis dataKey="name" tick={AXIS_TICK_SMALL} axisLine={AXIS_LINE} tickLine={false} />
        <YAxis
          domain={[0, 100]}
          tick={AXIS_TICK}
          axisLine={false}
          tickLine={false}
          tickFormatter={(v: number) => `${v}%`}
        />
        <Tooltip
          contentStyle={TOOLTIP_STYLE}
          cursor={CURSOR_STYLE}
          formatter={(value) => [`${value ?? 0}%`, 'Consensus Rate']}
        />
        <Bar dataKey="rate" name="Consensus Rate" fill={COLORS.blue} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
