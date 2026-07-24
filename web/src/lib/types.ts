// ============================================================
// Core — Shared Error Types & Generic Response Types
// ============================================================

/**
 * Represents an HTTP API error with a non-2xx status code.
 */
export class ApiError extends Error {
  public readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/**
 * Represents a network connectivity error (e.g., offline, DNS failure).
 */
export class NetworkError extends Error {
  constructor(message = 'Network request failed') {
    super(message);
    this.name = 'NetworkError';
  }
}

/**
 * Represents a request timeout.
 */
export class TimeoutError extends Error {
  constructor(message = 'Request timed out') {
    super(message);
    this.name = 'TimeoutError';
  }
}

// ── Pagination Types ────────────────────────────────────────

export interface PaginationMeta {
  page: number;
  pageSize: number;
  total: number;
  totalPages: number;
}

export interface PaginatedResponse<T> {
  data: T[];
  meta: PaginationMeta;
}
