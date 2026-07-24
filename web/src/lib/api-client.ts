import { ApiError, NetworkError, TimeoutError } from '@/lib/types';

/**
 * Returns the base URL for API requests.
 * Reads from NEXT_PUBLIC_API_URL env var, falls back to http://localhost:3001.
 */
function getBaseUrl(): string {
  return process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:3001';
}

interface FetchConfig {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  params?: Record<string, string | number | boolean | undefined>;
  headers?: Record<string, string>;
  timeoutMs?: number;
}

/**
 * Generic fetch wrapper with timeout, error handling, and query params.
 */
export async function apiFetch<T = unknown>(
  endpoint: string,
  config: FetchConfig = {},
): Promise<T> {
  const { method = 'GET', body, params, headers, timeoutMs = 30000 } = config;
  const baseUrl = getBaseUrl();

  // Build URL with query params
  const url = new URL(endpoint, baseUrl);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    }
  }

  // Setup abort controller for timeout
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  const requestHeaders: Record<string, string> = {
    'Content-Type': 'application/json',
    ...headers,
  };

  try {
    const response = await fetch(url.toString(), {
      method,
      headers: requestHeaders,
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });

    if (!response.ok) {
      let message = `HTTP ${response.status}: ${response.statusText}`;
      try {
        const errorBody = await response.json();
        if (errorBody.message) {
          message = errorBody.message;
        }
      } catch {
        // use default message
      }
      throw new ApiError(response.status, message);
    }

    // Handle 204 No Content
    if (response.status === 204) {
      return undefined as T;
    }

    return (await response.json()) as T;
  } catch (error) {
    if (error instanceof ApiError) {
      throw error;
    }
    if ((error as Error).name === 'AbortError') {
      throw new TimeoutError(`Request to ${endpoint} timed out after ${timeoutMs}ms`);
    }
    if (error instanceof TypeError) {
      throw new NetworkError(
        error.message || `Network error while fetching ${endpoint}`,
      );
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

/**
 * Convenience methods for common HTTP verbs.
 */
export const api = {
  get<T = unknown>(endpoint: string, config?: Omit<FetchConfig, 'method' | 'body'>) {
    return apiFetch<T>(endpoint, { ...config, method: 'GET' });
  },
  post<T = unknown>(endpoint: string, body: unknown, config?: Omit<FetchConfig, 'method' | 'body'>) {
    return apiFetch<T>(endpoint, { ...config, method: 'POST', body });
  },
  put<T = unknown>(endpoint: string, body: unknown, config?: Omit<FetchConfig, 'method' | 'body'>) {
    return apiFetch<T>(endpoint, { ...config, method: 'PUT', body });
  },
  patch<T = unknown>(endpoint: string, body: unknown, config?: Omit<FetchConfig, 'method' | 'body'>) {
    return apiFetch<T>(endpoint, { ...config, method: 'PATCH', body });
  },
  delete<T = unknown>(endpoint: string, config?: Omit<FetchConfig, 'method' | 'body'>) {
    return apiFetch<T>(endpoint, { ...config, method: 'DELETE' });
  },
};
