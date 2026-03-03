import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { EstimateButtons } from './EstimateButtons';
import { SP_VALUES } from '@/api/types';
import type { StoryPoints } from '@/api/types';

describe('EstimateButtons', () => {
  it('should render all 10 story point values as strings', () => {
    render(<EstimateButtons selectedValue={null} onSelect={() => {}} />);

    const buttons = screen.getAllByRole('button');
    expect(buttons).toHaveLength(10);

    const labels = buttons.map((b) => b.textContent);
    expect(labels).toEqual(SP_VALUES);
  });

  it('should highlight selected value', () => {
    render(<EstimateButtons selectedValue="5" onSelect={() => {}} />);

    const selected = screen.getByRole('button', { name: '5' });
    expect(selected.className).toContain('from-efi-gold');
  });

  it('should call onSelect with string StoryPoints value', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn<(value: StoryPoints | null) => void>();

    render(<EstimateButtons selectedValue={null} onSelect={onSelect} />);

    await user.click(screen.getByRole('button', { name: '8' }));
    expect(onSelect).toHaveBeenCalledWith('8');
    expect(typeof onSelect.mock.calls[0]?.[0]).toBe('string');
  });

  it('should call onSelect with "?" for question mark', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn<(value: StoryPoints | null) => void>();

    render(<EstimateButtons selectedValue={null} onSelect={onSelect} />);

    await user.click(screen.getByRole('button', { name: '?' }));
    expect(onSelect).toHaveBeenCalledWith('?');
  });

  it('should call onSelect with null when clicking already selected value (unvote)', async () => {
    const user = userEvent.setup();
    const onSelect = vi.fn<(value: StoryPoints | null) => void>();

    render(<EstimateButtons selectedValue="5" onSelect={onSelect} />);

    await user.click(screen.getByRole('button', { name: '5' }));
    expect(onSelect).toHaveBeenCalledWith(null);
  });

  it('should disable all buttons when disabled prop is true', () => {
    render(<EstimateButtons selectedValue={null} onSelect={() => {}} disabled />);

    const buttons = screen.getAllByRole('button');
    buttons.forEach((button) => {
      expect(button).toBeDisabled();
    });
  });
});
