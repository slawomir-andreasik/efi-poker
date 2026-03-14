import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import type { TaskAnalyticsEntry } from '@/api/types';
import { TOOLTIP_STYLE, CURSOR_STYLE, GRID_STROKE, AXIS_TICK, AXIS_TICK_SMALL, AXIS_LINE, LEGEND_STYLE, COLORS, truncateTitle } from './chartTheme';

interface AvgVsFinalChartProps {
  tasks: TaskAnalyticsEntry[];
}

export function AvgVsFinalChart({ tasks }: AvgVsFinalChartProps) {
  if (tasks.length === 0) {
    return (
      <div className="flex items-center justify-center h-[300px] text-efi-text-tertiary text-sm">
        No task data available
      </div>
    );
  }

  const data = tasks.map((t) => {
    const finalNum = t.finalEstimate != null ? Number(t.finalEstimate) : null;
    return {
      name: truncateTitle(t.title, 16),
      average: t.averagePoints ?? null,
      final: !isNaN(finalNum as number) && finalNum != null ? finalNum : null,
    };
  });

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
        />
        <Legend wrapperStyle={LEGEND_STYLE} />
        <Bar dataKey="average" name="Average" fill={COLORS.gold} radius={[4, 4, 0, 0]} />
        <Bar dataKey="final" name="Final" fill={COLORS.green} radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
