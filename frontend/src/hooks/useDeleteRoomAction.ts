import { useNavigate } from 'react-router-dom';
import { useDeleteRoom } from '@/api/mutations';
import { useToast } from '@/components/Toast';
import { getErrorMessage } from '@/utils/error';
import { logger } from '@/utils/logger';

export function useDeleteRoomAction(slug: string, roomId: string) {
  const deleteRoomMutation = useDeleteRoom(slug);
  const navigate = useNavigate();
  const { showToast } = useToast();

  async function handleDeleteRoom() {
    try {
      await deleteRoomMutation.mutateAsync(roomId);
      showToast('Room deleted', 'success');
      void navigate(`/p/${slug}`);
    } catch (err) {
      logger.warn('Failed to delete room:', getErrorMessage(err));
      showToast(getErrorMessage(err));
    }
  }

  return { handleDeleteRoom, isPending: deleteRoomMutation.isPending };
}
