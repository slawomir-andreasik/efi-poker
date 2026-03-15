import { JoinByCodeForm } from '@/components/JoinByCodeForm';
import { Modal } from '@/components/Modal';

interface JoinByCodeModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export function JoinByCodeModal({ isOpen, onClose }: JoinByCodeModalProps) {
  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Join">
      <p className="text-sm text-efi-text-secondary mb-4">
        Enter a room code (ABC-123) or project slug to join.
      </p>
      <JoinByCodeForm />
    </Modal>
  );
}
