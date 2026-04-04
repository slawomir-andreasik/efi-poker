import { ChevronDown } from 'lucide-react';
import { useMemo, useState } from 'react';
import type { RoundHistoryEntry } from '@/api/types';

interface RoundHistoryPanelProps {
  history: RoundHistoryEntry[];
}

export function RoundHistoryPanel({ history }: RoundHistoryPanelProps) {
  const [expandedRound, setExpandedRound] = useState<number | null>(null);
  const sorted = useMemo(() => [...history].reverse(), [history]);

  if (history.length === 0) {
    return (
      <div className="glass-frost rounded-xl p-4 border border-white/10">
        <h2 className="text-xs font-semibold text-efi-text-secondary uppercase tracking-wider mb-2">
          Round History
        </h2>
        <p className="text-sm text-efi-text-tertiary">No completed rounds yet.</p>
      </div>
    );
  }

  return (
    <div className="glass-frost rounded-xl p-4 border border-white/10">
      <h2 className="text-xs font-semibold text-efi-text-secondary uppercase tracking-wider mb-3">
        Round History
        <span className="ml-2 font-normal text-efi-text-tertiary normal-case">
          ({history.length} {history.length === 1 ? 'round' : 'rounds'})
        </span>
      </h2>
      <div className="space-y-1.5">
        {sorted.map((entry) => {
          const isOpen = expandedRound === entry.roundNumber;
          const hasStats = entry.averagePoints != null || entry.medianPoints != null;
          return (
            <div
              key={entry.roundNumber}
              className="rounded-lg border border-white/8 overflow-hidden"
            >
              <button
                type="button"
                onClick={() => setExpandedRound(isOpen ? null : entry.roundNumber)}
                className="w-full flex items-start justify-between gap-3 px-3 py-2.5 text-left hover:bg-white/4 transition-colors cursor-pointer focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:outline-none"
              >
                {/* Left: round label + topic + stats */}
                <div className="flex flex-col gap-0.5 min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-[10px] font-bold text-efi-text-tertiary uppercase tracking-widest shrink-0">
                      R{entry.roundNumber}
                    </span>
                    {entry.topic ? (
                      <span className="text-sm text-efi-text-primary font-medium truncate">
                        {entry.topic}
                      </span>
                    ) : (
                      <span className="text-xs text-efi-text-tertiary italic">no topic</span>
                    )}
                  </div>
                  {hasStats && (
                    <div className="flex items-center gap-3">
                      {entry.averagePoints != null && (
                        <span className="text-xs text-efi-text-tertiary">
                          avg{' '}
                          <span className="text-efi-gold-light font-semibold">
                            {entry.averagePoints.toFixed(1)}
                          </span>
                        </span>
                      )}
                      {entry.medianPoints != null && (
                        <span className="text-xs text-efi-text-tertiary">
                          med{' '}
                          <span className="text-efi-gold-light font-semibold">
                            {entry.medianPoints.toFixed(1)}
                          </span>
                        </span>
                      )}
                      <span className="text-xs text-efi-text-tertiary">
                        {entry.voteCount} {entry.voteCount === 1 ? 'vote' : 'votes'}
                      </span>
                    </div>
                  )}
                </div>
                {/* Chevron */}
                <ChevronDown
                  className={`w-3.5 h-3.5 text-efi-text-tertiary transition-transform mt-1 shrink-0 ${isOpen ? 'rotate-180' : ''}`}
                />
              </button>

              {/* Expanded: individual votes */}
              {isOpen && (
                <div className="px-3 pb-3 border-t border-white/8">
                  {entry.votes.length > 0 ? (
                    <div className="flex flex-wrap gap-1.5 pt-2.5">
                      {entry.votes.map((vote) => (
                        <span
                          key={`${vote.nickname}-${vote.storyPoints}`}
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs ${
                            vote.storyPoints === '?'
                              ? 'bg-efi-warning/15 text-efi-warning border border-efi-warning/25'
                              : 'bg-white/8 text-efi-text-primary border border-white/12'
                          }`}
                        >
                          <span className="text-efi-text-secondary">{vote.nickname}</span>
                          <span className="font-bold ml-0.5">{vote.storyPoints}</span>
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-efi-text-tertiary pt-2">No votes recorded.</p>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
