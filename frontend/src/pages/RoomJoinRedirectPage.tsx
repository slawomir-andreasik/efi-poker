import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom';
import { ApiError, api, getAuth, saveAuth } from '@/api/client';
import { roomApi } from '@/api/queries';
import { queryKeys } from '@/api/queryKeys';
import type { ParticipantResponse } from '@/api/types';
import { Spinner } from '@/components/Spinner';
import { TraceCopyButton } from '@/components/TraceCopyButton';
import { getErrorMessage } from '@/utils/error';
import { logger } from '@/utils/logger';

export function RoomJoinRedirectPage() {
  const { roomSlug } = useParams<{ roomSlug: string }>();
  const navigate = useNavigate();
  const [accumulating, setAccumulating] = useState(false);

  const { data, isLoading, error } = useQuery({
    queryKey: queryKeys.rooms.bySlug(roomSlug as string),
    queryFn: () => roomApi.bySlug(roomSlug as string),
    enabled: Boolean(roomSlug),
    retry: false,
  });

  useEffect(() => {
    if (!data || accumulating) return;

    const auth = getAuth(data.projectSlug);
    if (auth.nickname) {
      // Re-join to accumulate room access, then navigate
      setAccumulating(true);
      api<ParticipantResponse>(
        `/projects/${data.projectSlug}/participants`,
        { method: 'POST', body: { nickname: auth.nickname, roomId: data.roomId } },
        data.projectSlug,
      )
        .then((participant) => {
          if (participant.token) {
            saveAuth(data.projectSlug, {
              guestToken: participant.token,
              nickname: participant.nickname,
            });
          } else {
            saveAuth(data.projectSlug, { nickname: participant.nickname });
          }
          logger.debug(`Room redirect: accumulated room access for project=${data.projectSlug}`);
          void navigate(`/p/${data.projectSlug}/r/${data.roomId}`, { replace: true });
        })
        .catch((err) => {
          logger.warn('Room access accumulation failed:', getErrorMessage(err));
          void navigate(`/p/${data.projectSlug}/r/${data.roomId}`, { replace: true });
        });
    }
  }, [data, accumulating, navigate]);

  if (isLoading || accumulating) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <Spinner />
      </div>
    );
  }

  if (error) {
    if (error instanceof ApiError && error.status === 404) {
      return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
          <p className="text-efi-text-primary font-medium">Room not found</p>
          <Link
            to="/"
            className="text-sm text-efi-gold-light hover:text-efi-gold transition-colors no-underline hover:underline"
          >
            Back to Home
          </Link>
        </div>
      );
    }
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] gap-4">
        <p className="text-efi-error">{getErrorMessage(error)}</p>
        {error instanceof ApiError && error.traceId && <TraceCopyButton traceId={error.traceId} />}
        <Link to="/" className="text-sm text-efi-gold-light hover:text-white transition-colors">
          Back to Home
        </Link>
      </div>
    );
  }

  if (!data) return null;

  const auth = getAuth(data.projectSlug);
  if (auth.nickname) {
    // useEffect will handle re-join + navigate
    return null;
  }

  logger.debug(
    `Room redirect: no participant, redirecting to join for project=${data.projectSlug}`,
  );
  return <Navigate to={`/p/${data.projectSlug}/join?room=${data.roomId}`} replace />;
}
