import { useState, useRef } from 'react';
import { EstimateButtons } from './EstimateButtons';
import { ProgressBar } from './ProgressBar';
import { Linkify } from '@/lib/linkify';
import { SP_VALUES } from '@/api/types';
import { TextArea } from '@/components/TextInput';
import { CommentInput } from '@/components/CommentInput';
import { useSaveIndicator } from '@/hooks/useSaveIndicator';
import type { StoryPoints, EstimateResponse } from '@/api/types';

interface TaskCardProps {
  id: string;
  title: string;
  description?: string;
  votedCount: number;
  questionVotesCount?: number;
  totalParticipants: number;
  selectedSp: StoryPoints | null;
  onEstimate: (taskId: string, value: StoryPoints | null, comment?: string) => void;
  disabled?: boolean;
  revealed?: boolean;
  allEstimates?: EstimateResponse[] | null;
  averagePoints?: number | null;
  medianPoints?: number | null;
  finalEstimate?: string | null;
  isAdmin?: boolean;
  onSetFinalEstimate?: (taskId: string, value: StoryPoints) => void;
  onUpdateDescription?: (taskId: string, description: string) => void;
  commentTemplate?: string;
  commentRequired?: boolean;
  myComment?: string;
}

export function TaskCard({
  id,
  title,
  description,
  votedCount,
  questionVotesCount,
  totalParticipants,
  selectedSp,
  onEstimate,
  disabled = false,
  revealed = false,
  allEstimates,
  averagePoints,
  medianPoints,
  finalEstimate,
  isAdmin = false,
  onSetFinalEstimate,
  onUpdateDescription,
  commentTemplate,
  commentRequired = false,
  myComment,
}: TaskCardProps) {
  const hasVoted = selectedSp !== null;
  const showCommentBox = !revealed && !disabled && (commentTemplate || commentRequired);
  const [comment, setComment] = useState(myComment ?? commentTemplate ?? '');
  const { saving, showSaveIndicator } = useSaveIndicator();

  return (
    <div
      className={`
        rounded-xl p-2.5 sm:p-3 transition-all
        ${hasVoted ? 'glass-gold border-efi-gold/30' : 'glass-frost hover:border-efi-gold-light/20'}
      `}
    >
      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between mb-3">
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-efi-text-primary">{title}</h3>
          {isAdmin && !revealed && onUpdateDescription ? (
            <InlineDescription key={description ?? ''} taskId={id} value={description ?? ''} onSave={onUpdateDescription} />
          ) : (
            description && <p className="text-sm text-efi-text-secondary mt-1"><Linkify text={description} /></p>
          )}
        </div>
        <div className="flex items-center gap-3 sm:ml-4 mt-1 sm:mt-0">
          {totalParticipants > 0 && (
            <div className="flex items-center gap-2">
              {questionVotesCount != null && questionVotesCount > 0 && !revealed && (
                <div
                  className="flex items-center justify-center w-6 h-6 rounded-full bg-efi-warning/20 text-efi-warning border border-efi-warning/30 text-xs font-bold mr-1 cursor-help"
                  title={`${questionVotesCount} person${questionVotesCount > 1 ? 's' : ''} have questions/doubts. Please clarify.`}
                >
                  ?
                </div>
              )}
              <ProgressBar voted={votedCount} total={totalParticipants} />
            </div>
          )}
        </div>
      </div>

      {!revealed && (
        <EstimateButtons
          selectedValue={selectedSp}
          onSelect={(value) => {
            onEstimate(id, value, value !== null ? (comment.trim() || undefined) : undefined);
            if (value !== null) showSaveIndicator();
          }}
          disabled={disabled}
        />
      )}

      {/* Comment textarea */}
      {showCommentBox && (
        <CommentInput
          comment={comment}
          onCommentChange={setComment}
          hasTemplate={Boolean(commentTemplate)}
          selectedSp={selectedSp}
          onCommentSave={(newComment) => {
            if (selectedSp === null) return;
            onEstimate(id, selectedSp, newComment || undefined);
            showSaveIndicator();
          }}
          saving={saving}
        />
      )}

      {/* Stats + individual votes after reveal */}
      {revealed && allEstimates && allEstimates.length > 0 && (
        <div className="mt-4 pt-4 border-t border-efi-gold-light/10">
          {/* Stats row */}
          <div className="flex flex-wrap gap-4 mb-3">
            {averagePoints != null && (
              <span className="text-sm text-efi-text-secondary">
                Avg: <span className="text-efi-gold-light font-semibold">{averagePoints.toFixed(1)}</span>
              </span>
            )}
            {medianPoints != null && (
              <span className="text-sm text-efi-text-secondary">
                Med: <span className="text-efi-gold-light font-semibold">{medianPoints}</span>
              </span>
            )}
            {finalEstimate && (
              <span className="text-sm text-efi-text-secondary">
                Final: <span className="text-efi-success font-bold">{finalEstimate}</span>
              </span>
            )}
          </div>

          {/* Individual estimates */}
          {allEstimates.some((e) => e.comment) ? (
            <div className="space-y-2 mb-3">
              {allEstimates.map((est) => {
                const isQuestion = est.storyPoints === '?';
                return (
                  <div
                    key={est.id}
                    className={`px-2.5 py-1.5 rounded-md text-xs ${isQuestion
                        ? 'bg-efi-warning/20 border border-efi-warning/30'
                        : 'bg-white/6'
                      }`}
                  >
                    <div className="flex items-center gap-1.5">
                      <span className={isQuestion ? 'text-efi-warning/70' : 'text-efi-text-secondary'}>{est.participantNickname}</span>
                      <span className={`font-bold ${isQuestion ? 'text-efi-warning' : 'text-efi-text-primary'}`}>{est.storyPoints}</span>
                    </div>
                    {est.comment && (
                      <p className="text-efi-text-tertiary mt-1 whitespace-pre-line break-words">{est.comment}</p>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="flex flex-wrap gap-2 mb-3">
              {allEstimates.map((est) => {
                const isQuestion = est.storyPoints === '?';
                return (
                  <span
                    key={est.id}
                    className={`inline-flex items-center gap-1.5 px-2 py-1 rounded-md text-xs ${isQuestion
                        ? 'bg-efi-warning/20 border border-efi-warning/30 text-efi-warning'
                        : 'bg-white/6 text-efi-text-primary'
                      }`}
                  >
                    <span className={isQuestion ? 'text-efi-warning/70' : 'text-efi-text-secondary'}>{est.participantNickname}</span>
                    <span className="font-bold">{est.storyPoints}</span>
                  </span>
                );
              })}
            </div>
          )}

          {/* Admin: final SP selector */}
          {isAdmin && onSetFinalEstimate && (
            <div className="mt-3 pt-3 border-t border-efi-gold-light/10">
              <p className="text-xs text-efi-text-secondary mb-2">Set final SP:</p>
              <div className="flex flex-wrap gap-1.5">
                {SP_VALUES.map((value) => {
                  const isSelected = finalEstimate === value;
                  return (
                    <button
                      key={value}
                      onClick={() => onSetFinalEstimate(id, value)}
                      className={`
                        w-9 h-9 rounded-md font-bold text-xs transition-all cursor-pointer
                        ${isSelected
                          ? 'bg-efi-success text-white shadow-lg shadow-efi-success/30'
                          : 'bg-efi-well border border-efi-gold-light/20 text-efi-gold-light hover:border-efi-success hover:text-white'
                        }
                        active:scale-95 focus-visible:ring-2 focus-visible:ring-efi-success focus-visible:outline-none
                      `}
                    >
                      {value}
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function InlineDescription({
  taskId,
  value,
  onSave,
}: {
  taskId: string;
  value: string;
  onSave: (taskId: string, description: string) => void;
}) {
  const [text, setText] = useState(value);
  const initialRef = useRef(value);

  function handleBlur() {
    const trimmed = text.trim();
    if (trimmed !== initialRef.current) {
      onSave(taskId, trimmed);
      initialRef.current = trimmed;
    }
  }

  return (
    <TextArea
      value={text}
      onChange={(e) => setText(e.target.value)}
      onBlur={handleBlur}
      placeholder="Add description..."
      maxLength={2000}
      rows={1}
      className="w-full mt-1 bg-transparent border-b border-dashed border-efi-gold-light/20 text-base text-efi-text-secondary placeholder-efi-text-tertiary px-0 py-1 resize-none focus:outline-none focus:border-efi-gold"
    />
  );
}
