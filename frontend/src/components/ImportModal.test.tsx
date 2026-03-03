import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ImportModal } from './ImportModal';

describe('ImportModal', () => {
  it('should not render when closed', () => {
    render(<ImportModal isOpen={false} onClose={() => {}} onImport={() => {}} />);
    expect(screen.queryByText('Import Tasks')).not.toBeInTheDocument();
  });

  it('should render when open', () => {
    render(<ImportModal isOpen={true} onClose={() => {}} onImport={() => {}} />);
    expect(screen.getByText('Import Tasks')).toBeInTheDocument();
  });

  it('should disable import button when textarea is empty', () => {
    render(<ImportModal isOpen={true} onClose={() => {}} onImport={() => {}} />);
    expect(screen.getByRole('button', { name: 'Import' })).toBeDisabled();
  });

  it('should show validation error when importing blank text', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn();

    render(<ImportModal isOpen={true} onClose={() => {}} onImport={onImport} />);

    const textarea = screen.getByPlaceholderText('Paste task titles, one per line...');
    await user.type(textarea, '   ');
    // Button should still be disabled since trim() is empty
    expect(screen.getByRole('button', { name: 'Import' })).toBeDisabled();
    expect(onImport).not.toHaveBeenCalled();
  });

  it('should call onImport with parsed titles and close', async () => {
    const user = userEvent.setup();
    const onImport = vi.fn();
    const onClose = vi.fn();

    render(<ImportModal isOpen={true} onClose={onClose} onImport={onImport} />);

    const textarea = screen.getByPlaceholderText('Paste task titles, one per line...');
    await user.type(textarea, 'Task Alpha\nTask Beta\n\nTask Gamma');
    await user.click(screen.getByRole('button', { name: 'Import' }));

    expect(onImport).toHaveBeenCalledWith(['Task Alpha', 'Task Beta', 'Task Gamma']);
    expect(onClose).toHaveBeenCalled();
  });

  it('should call onClose when cancel is clicked', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    render(<ImportModal isOpen={true} onClose={onClose} onImport={() => {}} />);
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onClose).toHaveBeenCalled();
  });
});
