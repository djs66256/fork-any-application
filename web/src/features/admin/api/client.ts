import { getSupabaseBrowserClient } from '@/lib/supabase';
import type {
  AdminStats,
  AdminDrama,
  AdminDramaFormData,
  AdminEpisode,
  AdminEpisodeFormData,
  AdminUser,
  PaginatedResponse,
  ApiResponse,
} from './types';
import { AdminApiError } from './types';

function getBaseUrl(): string {
  if (typeof window !== 'undefined') {
    return process.env.NEXT_PUBLIC_API_URL ?? '';
  }
  return process.env.NEXT_PUBLIC_API_URL ?? `http://localhost:${process.env.PORT ?? '3001'}`;
}

async function getAuthHeaders(): Promise<Record<string, string>> {
  const supabase = getSupabaseBrowserClient();
  const { data: { session } } = await supabase.auth.getSession();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };
  if (session?.access_token) {
    headers['Authorization'] = `Bearer ${session.access_token}`;
  }
  return headers;
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let body: { code?: string; message?: string } = {};
    try {
      body = await response.json();
    } catch {
      // ignore parse errors
    }

    if (response.status === 401) {
      // Redirect to login
      if (typeof window !== 'undefined') {
        window.location.href = '/admin/login';
      }
      throw new AdminApiError(401, 'UNAUTHORIZED', '请先登录');
    }

    throw new AdminApiError(
      response.status,
      body.code ?? 'UNKNOWN',
      body.message ?? `请求失败 (${response.status})`,
    );
  }

  const result: ApiResponse<T> = await response.json();
  return result.data;
}

async function adminFetch<T>(
  endpoint: string,
  options: RequestInit = {},
): Promise<T> {
  const baseUrl = getBaseUrl();
  const url = `${baseUrl}${endpoint}`;
  const headers = await getAuthHeaders();

  const response = await fetch(url, {
    ...options,
    headers: {
      ...headers,
      ...options.headers,
    },
  });

  return handleResponse<T>(response);
}

export const adminApi = {
  // Stats
  async getStats(): Promise<AdminStats> {
    return adminFetch<AdminStats>('/api/admin/stats');
  },

  // Dramas
  async listDramas(page = 1, pageSize = 20): Promise<PaginatedResponse<AdminDrama>> {
    return adminFetch<PaginatedResponse<AdminDrama>>(
      `/api/admin/dramas?page=${page}&pageSize=${pageSize}`,
    );
  },

  async getDrama(id: string): Promise<AdminDrama> {
    return adminFetch<AdminDrama>(`/api/admin/dramas/${id}`);
  },

  async createDrama(data: AdminDramaFormData): Promise<AdminDrama> {
    return adminFetch<AdminDrama>('/api/admin/dramas', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async updateDrama(id: string, data: Partial<AdminDramaFormData>): Promise<AdminDrama> {
    return adminFetch<AdminDrama>(`/api/admin/dramas/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async deleteDrama(id: string): Promise<{ deleted: boolean }> {
    return adminFetch<{ deleted: boolean }>(`/api/admin/dramas/${id}`, {
      method: 'DELETE',
    });
  },

  // Episodes
  async listEpisodes(dramaId: string): Promise<AdminEpisode[]> {
    const result = await adminFetch<{ drama_id: string; items: AdminEpisode[] }>(`/api/admin/dramas/${dramaId}/episodes`);
    return result.items ?? [];
  },

  async createEpisode(dramaId: string, data: AdminEpisodeFormData): Promise<AdminEpisode> {
    return adminFetch<AdminEpisode>(`/api/admin/dramas/${dramaId}/episodes`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  async updateEpisode(id: string, data: Partial<AdminEpisodeFormData>): Promise<AdminEpisode> {
    return adminFetch<AdminEpisode>(`/api/admin/episodes/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  async deleteEpisode(id: string): Promise<{ deleted: boolean }> {
    return adminFetch<{ deleted: boolean }>(`/api/admin/episodes/${id}`, {
      method: 'DELETE',
    });
  },

  // Users
  async listUsers(page = 1, pageSize = 20): Promise<PaginatedResponse<AdminUser>> {
    return adminFetch<PaginatedResponse<AdminUser>>(
      `/api/admin/users?page=${page}&pageSize=${pageSize}`,
    );
  },

  async updateUserRole(userId: string, role: string): Promise<AdminUser> {
    return adminFetch<AdminUser>(`/api/admin/users/${userId}/role`, {
      method: 'PUT',
      body: JSON.stringify({ role }),
    });
  },
};