import { describe, it, expect, vi, beforeEach } from 'vitest';
import { apiFetch, api } from '@/lib/api-client';
import { ApiError, TimeoutError, NetworkError } from '@/lib/types';

describe('apiFetch', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe('T-05: Successful GET request', () => {
    it('should construct URL with baseUrl and endpoint', async () => {
      const mockData = { ok: true };
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockData),
      } as Response);

      const result = await apiFetch('/api/health');

      expect(result).toEqual(mockData);
      expect(fetch).toHaveBeenCalledTimes(1);
      const callUrl = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][0] as string;
      expect(callUrl).toContain('/api/health');
    });

    it('should append query params to URL', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({}),
      } as Response);

      await apiFetch('/api/dramas', {
        params: { page: '1', size: '10' },
      });

      const callUrl = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][0] as string;
      expect(callUrl).toContain('page=1');
      expect(callUrl).toContain('size=10');
    });

    it('should return parsed JSON response', async () => {
      const mockData = { status: 'ok', version: '0.1.0' };
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(mockData),
      } as Response);

      const result = await apiFetch('/api/health');

      expect(result).toEqual(mockData);
    });
  });

  describe('T-06: HTTP error handling', () => {
    it('should throw ApiError on 500 response', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Internal Server Error',
        json: () => Promise.resolve({}),
      } as Response);

      let caught: ApiError | null = null;
      try {
        await apiFetch('/api/test');
      } catch (e) {
        caught = e as ApiError;
      }
      expect(caught).toBeInstanceOf(ApiError);
      expect(caught!.status).toBe(500);
    });

    it('should include error body message in ApiError', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 400,
        statusText: 'Bad Request',
        json: () => Promise.resolve({ message: 'Validation failed' }),
      } as Response);

      await expect(apiFetch('/api/test')).rejects.toMatchObject({
        status: 400,
        message: 'Validation failed',
      });
    });

    it('should throw ApiError on 404 response', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 404,
        statusText: 'Not Found',
        json: () => Promise.resolve({}),
      } as Response);

      await expect(apiFetch('/api/test')).rejects.toMatchObject({
        status: 404,
      });
    });
  });

  describe('Timeout handling', () => {
    it('should throw TimeoutError on abort', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValueOnce(
        Object.assign(new Error('The operation was aborted'), { name: 'AbortError' }),
      );

      await expect(apiFetch('/api/test', { timeoutMs: 1 })).rejects.toThrow(TimeoutError);
    });
  });

  describe('Network error handling', () => {
    it('should throw NetworkError on TypeError (e.g. offline)', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValueOnce(
        new TypeError('Failed to fetch'),
      );

      await expect(apiFetch('/api/test')).rejects.toThrow(NetworkError);
    });
  });

  describe('Convenience methods', () => {
    it('api.get should use GET method', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ data: [] }),
      } as Response);

      await api.get('/api/dramas');

      const init = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][1] as RequestInit;
      expect(init.method).toBe('GET');
    });

    it('api.post should use POST method with body', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: () => Promise.resolve({ id: '1' }),
      } as Response);

      await api.post('/api/dramas', { title: 'Test' });

      const init = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][1] as RequestInit;
      expect(init.method).toBe('POST');
      expect(init.body).toBe(JSON.stringify({ title: 'Test' }));
    });

    it('should return undefined for 204 No Content', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 204,
      } as Response);

      const result = await api.delete('/api/dramas/1');
      expect(result).toBeUndefined();
    });
  });
});
