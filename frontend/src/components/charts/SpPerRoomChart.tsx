import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import type { RoomStatsEntry } from '@/api/types';
import { TOOLTIP_STYLE, CURSOR_STYLE, GRID_STROKE, AXIS_TICK, AXIS_TICK_SMALL, AXIS_LINE, COLORS, truncateTitle } from './chartTheme';

interface SpPerRoomChartProps {
  rooms: RoomStatsEntry[];
}

export function SpPerRoomChart({ rooms }: SpPerRoomChartProps) {
  if (rooms.length === 0) {
    return (
      <div className="flex items-center justify-center h-[300px] text-efi-text-tertiary text-sm">
        No room data available
      </div>
    );
  }

  const data = rooms.map((r) => ({
    name: truncateTitle(r.title),
    sp: r.totalStoryPoints,
  }));

  return (
    <ResponsiveContainer width="100%" height={300}>
      <BarChart data={data} margin={{ top: 10, right: 20, left: 0, bottom: 5 }}>
        <CartesianGrid strokeDasharray="3 3" stroke={GRID_STROKE} />
        <XAxis
          dataKey="name"
          tick={AXIS_TICK_SMALL}
          axisLine={AXIS_LINE}
          tickLine={false}
        />
        <YAxis
          tick={AXIS_TICK}
          axisLine={false}
          tickLine={false}
          allowDecimals={false}
        />
        <Tooltip
          contentStyle={TOOLTIP_STYLE}
          cursor={CURSOR_STYLE}
          formatter={(value) => [value ?? 0, 'Story Points']}
        />
        <Bar dataKey="sp" name="Story Points" fill={COLORS.gold} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
