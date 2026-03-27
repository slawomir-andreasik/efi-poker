import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { ApiError, api, getAuth, getIdentity, saveAuth } from '@/api/client';
import { useJoinProject } from '@/api/mutations';
import type { ParticipantResponse } from '@/api/types';
import { RandomNameButton } from '@/components/RandomNameButton';
import { ButtonSpinner, Spinner } from '@/components/Spinner';
import { TextInput } from '@/components/TextInput';
import { useToast } from '@/components/Toast';
import { useDocumentTitle } from '@/hooks/useDocumentTitle';
import { primaryBtnLg } from '@/styles/buttons';
import { getErrorMessage } from '@/utils/error';
import { logger } from '@/utils/logger';

type Mode = 'loading' | 'welcome-back' | 'join-form';

export function JoinPage() {
  const { slug } = useParams<{ slug: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const projectName = slug ? (getAuth(slug).projectName ?? slug) : undefined;
  useDocumentTitle('Join', projectName);

  const roomParam = searchParams.get('room');
  const roomRedirect = roomParam ? `/r/${roomParam}` : '';

  const [mode, setMode] = useState<Mode>('loading');
  const [nickname, setNickname] = useState('');
  const [restoredNickname, setRestoredNickname] = useState('');

  const joinProject = useJoinProject(slug ?? '');

  useEffect(() => {
    if (!slug) {
      setMode('join-form');
      return;
    }

    let cancelled = false;

    const pid = searchParams.get('pid');
    const auth = getAuth(slug);
    const identity = getIdentity();

    const hasExistingAuth = pid || auth.nickname;
    if (!hasExistingAuth) {
      if (identity) {
        // Auto-join with stored identity name
        api<ParticipantResponse>(
          `/projects/${slug}/participants`,
          { method: 'POST', body: { nickname: identity, ...(roomParam && { roomId: roomParam }) } },
          slug,
        )
          .then((participant) => {
            if (cancelled) return;
            if (participant.token) {
              saveAuth(slug, { guestToken: participant.token, nickname: participant.nickname });
            } else {
              saveAuth(slug, { nickname: participant.nickname });
            }
            void navigate(`/p/${slug}${roomRedirect}`);
          })
          .catch((err) => {
            if (cancelled) return;
            logger.warn('Auto-join failed:', getErrorMessage(err));
            if (!(err instanceof ApiError)) {
              showToast(getErrorMessage(err));
            }
            setMode('join-form');
          });
        return () => {
          cancelled = true;
        };
      }
      setMode('join-form');
      return;
    }

    if (pid) {
      // Validate participant by ID from URL param
      api<ParticipantResponse>(`/projects/${slug}/participants/${pid}`, {}, slug)
        .then((participant) => {
          if (cancelled) return;
          saveAuth(slug, { nickname: participant.nickname });
          setRestoredNickname(participant.nickname);
          setMode('welcome-back');
        })
        .catch((err: unknown) => {
          if (cancelled) return;
          if (err instanceof ApiError && err.status === 404 && slug) {
            saveAuth(slug, { guestToken: undefined, nickname: undefined });
          }
          showToast('Invalid or expired link. Please join with your nickname.');
          setMode('join-form');
        });
    } else if (auth.nickname) {
      // Welcome back with stored nickname
      setRestoredNickname(auth.nickname);
      setMode('welcome-back');
    } else {
      setMode('join-form');
    }

    return () => {
      cancelled = true;
    };
    // biome-ignore lint/correctness/useExhaustiveDependencies: roomRedirect derived from roomParam (already in deps)
  }, [slug, searchParams, showToast, navigate, roomParam]);

  async function handleJoin(e: React.FormEvent) {
    e.preventDefault();
    if (!nickname.trim() || !slug) return;

    logger.info(`Joining project: slug=${slug}`);

    try {
      await joinProject.mutateAsync({ nickname: nickname.trim(), roomId: roomParam ?? undefined });
      navigate(`/p/${slug}${roomRedirect}`);
    } catch (err) {
      logger.warn('Failed to join project:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  if (mode === 'loading') {
    const identity = getIdentity();
    return (
      <div className="flex flex-col items-center justify-center min-h-[80vh] gap-3">
        <Spinner />
        {identity && (
          <p className="text-sm text-efi-text-secondary">
            Joining as <span className="text-efi-text-primary font-medium">{identity}</span>...
          </p>
        )}
      </div>
    );
  }

  if (mode === 'welcome-back') {
    return (
      <div className="flex flex-col items-center justify-center min-h-[80vh] px-4">
        <div className="text-center mb-6 sm:mb-8">
          <h1 className="text-2xl sm:text-3xl font-bold text-efi-text-primary mb-2">
            Welcome back!
          </h1>
          <p className="text-efi-text-secondary">
            Continuing as{' '}
            <span className="text-efi-gold-light font-medium">{restoredNickname}</span>
          </p>
        </div>

        <div className="w-full max-w-sm space-y-3">
          <button
            type="button"
            onClick={() => navigate(`/p/${slug}${roomRedirect}`)}
            className={primaryBtnLg}
          >
            Continue
          </button>
          <button
            type="button"
            onClick={() => setMode('join-form')}
            className="w-full py-2 text-sm text-efi-text-secondary hover:text-efi-text-primary transition-colors cursor-pointer"
          >
            Join with a different name
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center justify-center min-h-[80vh] px-4">
      <div className="text-center mb-6 sm:mb-8 animate-[fade-in-up_0.6s_ease-out] motion-reduce:animate-none">
        <h1 className="text-2xl sm:text-3xl font-bold text-efi-text-primary mb-2">Join Project</h1>
        <p className="text-efi-text-secondary">Enter your nickname to join the planning session</p>
      </div>

      {roomParam && (
        <div className="w-full max-w-sm mb-4 px-3 py-2.5 rounded-lg border border-efi-gold-light/15 bg-efi-gold/5 text-xs text-efi-text-secondary animate-[fade-in-up_0.6s_ease-out_0.1s_both] motion-reduce:animate-none">
          <span className="text-efi-gold-light font-medium">Room link</span> - you will only have
          access to this room, not the entire project.
        </div>
      )}

      <form
        onSubmit={(e) => void handleJoin(e)}
        className="w-full max-w-sm animate-[fade-in-up_0.6s_ease-out_0.15s_both] motion-reduce:animate-none"
      >
        <div className="glass-frost rounded-2xl p-4 sm:p-6">
          <label
            htmlFor="nickname"
            className="block text-sm font-medium text-efi-text-secondary mb-2"
          >
            Your Nickname
          </label>
          <div className="flex items-center gap-1">
            <TextInput
              id="nickname"
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="e.g. Alice"
              maxLength={100}
              className="flex-1 rounded-lg bg-efi-well border border-efi-gold-light/20 px-4 py-3 text-efi-text-primary placeholder-efi-text-tertiary text-base focus:outline-none focus:border-efi-gold transition-colors focus-visible:ring-2 focus-visible:ring-efi-gold focus-visible:ring-offset-2 focus-visible:ring-offset-efi-void"
            />
            <RandomNameButton onGenerate={setNickname} />
          </div>

          <button
            type="submit"
            disabled={joinProject.isPending || !nickname.trim()}
            className={`${primaryBtnLg} mt-4 flex items-center justify-center gap-2`}
          >
            {joinProject.isPending ? (
              <>
                <ButtonSpinner /> Joining...
              </>
            ) : (
              'Join'
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
