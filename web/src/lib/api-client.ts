import { ApiError, NetworkError, TimeoutError } from '@/lib/types';

function getBaseUrl(): string {
  const configuredBaseUrl = process.env.NEXT_PUBLIC_API_URL?.trim();

  if (
    configuredBaseUrl &&
    configuredBaseUrl !== 'undefined' &&
    configuredBaseUrl !== 'null'
  ) {
    return configuredBaseUrl;
  }

  if (typeof window !== 'undefined' && window.location.origin !== 'null') {
    return window.location.origin;
  }

  throw new Error('NEXT_PUBLIC_API_URL is required when window origin is unavailable');
}

interface FetchConfig {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  params?: Record<string, string | number | boolean | undefined>;
  headers?: Record<string, string>;
  timeoutMs?: number;
}

function extractErrorMessage(errorBody: unknown): string | null {
  if (!errorBody || typeof errorBody !== 'object') {
    return null;
  }

  if ('message' in errorBody && typeof errorBody.message === 'string') {
    return errorBody.message;
  }

  if (
    'error' in errorBody &&
    errorBody.error &&
    typeof errorBody.error === 'object' &&
    'message' in errorBody.error &&
    typeof errorBody.error.message === 'string'
  ) {
    return errorBody.error.message;
  }

  return null;
}

export async function apiFetch<T = unknown>(
  endpoint: string,
  config: FetchConfig = {},
): Promise<T> {
  const { method = 'GET', body, params, headers, timeoutMs = 30000 } = config;
  const baseUrl = getBaseUrl();

  const url = new URL(endpoint, baseUrl);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    }
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);

  const requestHeaders: Record<string, string> = {
    ...headers,
  };

  if (body !== undefined) {
    requestHeaders['Content-Type'] = 'application/json';
  }

  try {
    const response = await fetch(url.toString(), {
      method,
      headers: requestHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });

    if (!response.ok) {
      let message = `HTTP ${response.status}: ${response.statusText}`;
      try {
        const errorBody = await response.json();
        const extractedMessage = extractErrorMessage(errorBody);
        if (extractedMessage) {
          message = extractedMessage;
        }
      } catch {
        // use default message when error response is not json
      }
      throw new ApiError(response.status, message);
    }

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
      throw new NetworkError(error.message || `Network error while fetching ${endpoint}`);
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

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
