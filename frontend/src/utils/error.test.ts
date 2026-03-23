import { describe, expect, it } from 'vitest';
import { ApiError } from '@/api/client';
import { getErrorMessage } from './error';

describe('getErrorMessage', () => {
  it('should extract message from Error', () => {
    expect(getErrorMessage(new Error('something broke'))).toBe('something broke');
  });

  it('should extract message from ApiError', () => {
    const err = new ApiError('Room not found', 404, 'trace-123');
    expect(getErrorMessage(err)).toBe('Room not found');
  });

  it('should return "Unknown error" for non-Error objects', () => {
    expect(getErrorMessage('string error')).toBe('Unknown error');
    expect(getErrorMessage(null)).toBe('Unknown error');
    expect(getErrorMessage(undefined)).toBe('Unknown error');
    expect(getErrorMessage(42)).toBe('Unknown error');
  });
});
