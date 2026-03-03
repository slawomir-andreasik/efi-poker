let currentTraceId = generateTraceId();

export function generateTraceId(): string {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

export function generateSpanId(): string {
  const bytes = new Uint8Array(8);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, (b) => b.toString(16).padStart(2, '0')).join('');
}

export function createTraceparent(traceId: string, spanId: string): string {
  return `00-${traceId}-${spanId}-01`;
}

export function getTracingHeaders(): { traceparent: string } {
  const spanId = generateSpanId();
  return { traceparent: createTraceparent(currentTraceId, spanId) };
}

export function resetTrace(): void {
  currentTraceId = generateTraceId();
}

export function getCurrentTraceId(): string {
  return currentTraceId;
}
