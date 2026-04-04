import { useQuery } from '@tanstack/react-query';
import { Users } from 'lucide-react';
import { memo, useMemo } from 'react';
import { roomApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import type { ParticipantProgressEntry } from '@/api/types';
import { CollapsibleSection } from '@/components/CollapsibleSection';

interface ParticipantProgressProps {
  roomId: string;
  slug: string;
}

export function ParticipantProgress({ roomId, slug }: ParticipantProgressProps) {
  const { data } = useQuery({
    queryKey: queryKeys.rooms.participantProgress(roomId),
    queryFn: () => roomApi.participantProgress(roomId, slug),
    refetchInterval: (query) => {
      if (query.state.status === 'error') return false;
      return 10_000;
    },
  });

  const participants = data?.participants;
  const sortedParticipants = useMemo(() => {
    if (!participants) return [];
    return [...participants].sort((a, b) => a.votedCount - b.votedCount);
  }, [participants]);

  if (!data || sortedParticipants.length === 0) return null;

  return (
    <CollapsibleSection icon={Users} label="Participant Progress" defaultOpen>
      <div className="space-y-2">
        {sortedParticipants.map((p) => (
          <ParticipantRow key={p.nickname} participant={p} />
        ))}
      </div>
    </CollapsibleSection>
  );
}

const ParticipantRow = memo(function ParticipantRow({
  participant,
}: {
  participant: ParticipantProgressEntry;
}) {
  const { nickname, votedCount, totalTasks, hasCommentedAll } = participant;
  const pct = totalTasks > 0 ? (votedCount / totalTasks) * 100 : 0;

  const barColor =
    pct >= 100
      ? 'bg-green-500'
      : pct >= 50
        ? 'bg-amber-500'
        : pct > 0
          ? 'bg-red-400'
          : 'bg-white/10';

  return (
    <div className="flex items-center gap-3">
      <span className="text-sm text-efi-text-primary truncate w-28 shrink-0">{nickname}</span>
      <div className="flex-1 h-2 rounded-full bg-white/5 overflow-hidden">
        <div
          className={`h-full rounded-full transition-all duration-300 ${barColor}`}
          style={{ width: `${pct}%` }}
        />
      </div>
      <span className="text-xs text-efi-text-secondary tabular-nums shrink-0 w-14 text-right">
        {votedCount}/{totalTasks}
      </span>
      {hasCommentedAll && votedCount > 0 && (
        <span className="text-green-400 text-xs shrink-0" title="Commented on all voted tasks">
          &#10003;
        </span>
      )}
    </div>
  );
});
