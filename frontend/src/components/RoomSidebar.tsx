import { CountdownTimer } from './CountdownTimer';
import { ProgressStats } from './ProgressStats';
import type { TaskWithEstimateResponse, RoomType } from '@/api/types';

interface RoomSidebarProps {
  deadline?: string;
  roomType: RoomType;
  tasks: TaskWithEstimateResponse[];
  isRevealed: boolean;
}

export function RoomSidebar({ deadline, roomType, tasks, isRevealed }: RoomSidebarProps) {
  return (
    <aside className="lg:sticky lg:top-[var(--nav-total-height)] lg:self-start space-y-3">
      {/* Deadline card - visible only on desktop for Async rooms (mobile shows it in the header) */}
      {roomType === 'ASYNC' && deadline && (
        <div className="hidden lg:block rounded-xl glass-whisper p-4">
          <h3 className="text-xs font-medium text-efi-text-secondary uppercase tracking-wider mb-2">Deadline</h3>
          <CountdownTimer deadline={deadline} />
        </div>
      )}

      {/* Live session indicator */}
      {roomType === 'LIVE' && (
        <div className="hidden lg:block rounded-xl glass-whisper p-4">
          <div className="flex items-center gap-2">
            <span className="relative flex h-2.5 w-2.5">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-efi-live opacity-75" />
              <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-efi-live" />
            </span>
            <span className="text-sm font-medium text-efi-live">Live Session</span>
          </div>
        </div>
      )}

      <ProgressStats tasks={tasks} isRevealed={isRevealed} />
    </aside>
  );
}
