import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { Linkify } from './linkify';

describe('Linkify', () => {
  it('renders https URLs as links', () => {
    render(<Linkify text="Visit https://example.com for details" />);
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', 'https://example.com');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('renders http URLs as links', () => {
    render(<Linkify text="See http://example.com/page" />);
    const link = screen.getByRole('link');
    expect(link).toHaveAttribute('href', 'http://example.com/page');
  });

  it('renders plain text without URLs as text only', () => {
    render(<Linkify text="No links here, just text" />);
    expect(screen.queryByRole('link')).toBeNull();
    expect(screen.getByText('No links here, just text')).toBeTruthy();
  });

  it('renders multiple URLs as separate links', () => {
    render(
      <Linkify text="First https://a.com then https://b.com" />,
    );
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(2);
    expect(links[0]).toHaveAttribute('href', 'https://a.com');
    expect(links[1]).toHaveAttribute('href', 'https://b.com');
  });

  it('does not linkify javascript: protocol', () => {
    render(<Linkify text="Try javascript:alert(1) here" />);
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('does not linkify data: protocol', () => {
    render(<Linkify text="Try data:text/html,test here" />);
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('validates URLs with new URL() constructor - malformed https:// rejected', () => {
    render(<Linkify text="See https:// broken" />);
    expect(screen.queryByRole('link')).toBeNull();
  });

  it('renders malformed URL as plain text', () => {
    render(<Linkify text="Check https://[invalid for info" />);
    expect(screen.queryByRole('link')).toBeNull();
  });
});
