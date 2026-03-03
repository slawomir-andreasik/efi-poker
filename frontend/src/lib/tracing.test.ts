import { describe, it, expect } from 'vitest';
import {
  generateTraceId,
  generateSpanId,
  createTraceparent,
  getTracingHeaders,
  resetTrace,
  getCurrentTraceId,
} from './tracing';

describe('tracing', () => {
  describe('generateTraceId', () => {
    it('should generate a 32-character hex string', () => {
      const traceId = generateTraceId();
      expect(traceId).toMatch(/^[0-9a-f]{32}$/);
    });

    it('should generate unique IDs', () => {
      const ids = new Set(Array.from({ length: 10 }, () => generateTraceId()));
      expect(ids.size).toBe(10);
    });
  });

  describe('generateSpanId', () => {
    it('should generate a 16-character hex string', () => {
      const spanId = generateSpanId();
      expect(spanId).toMatch(/^[0-9a-f]{16}$/);
    });

    it('should generate unique IDs', () => {
      const ids = new Set(Array.from({ length: 10 }, () => generateSpanId()));
      expect(ids.size).toBe(10);
    });
  });

  describe('createTraceparent', () => {
    it('should format W3C traceparent correctly', () => {
      const result = createTraceparent('a'.repeat(32), 'b'.repeat(16));
      expect(result).toBe(`00-${'a'.repeat(32)}-${'b'.repeat(16)}-01`);
    });
  });

  describe('getTracingHeaders', () => {
    it('should return object with traceparent header', () => {
      const headers = getTracingHeaders();
      expect(headers).toHaveProperty('traceparent');
      expect(headers.traceparent).toMatch(/^00-[0-9a-f]{32}-[0-9a-f]{16}-01$/);
    });

    it('should use the same trace ID across calls', () => {
      const h1 = getTracingHeaders();
      const h2 = getTracingHeaders();
      const traceId1 = h1.traceparent.split('-')[1];
      const traceId2 = h2.traceparent.split('-')[1];
      expect(traceId1).toBe(traceId2);
    });

    it('should generate unique span IDs per call', () => {
      const h1 = getTracingHeaders();
      const h2 = getTracingHeaders();
      const spanId1 = h1.traceparent.split('-')[2];
      const spanId2 = h2.traceparent.split('-')[2];
      expect(spanId1).not.toBe(spanId2);
    });
  });

  describe('resetTrace', () => {
    it('should generate a new trace ID', () => {
      const before = getCurrentTraceId();
      resetTrace();
      const after = getCurrentTraceId();
      expect(before).not.toBe(after);
    });
  });
});
