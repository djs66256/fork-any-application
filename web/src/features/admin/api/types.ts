// Admin API types matching the backend response format

export interface AdminStats {
  total_dramas: number;
  total_episodes: number;
  total_users: number;
}

export interface AdminDrama {
  id: string;
  title: string;
  description: string;
  cover_url: string | null;
  category: string;
  episode_count: number;
  tags: string[];
  rating: number | null;
  created_at: string;
  updated_at: string;
}

export interface AdminDramaFormData {
  title: string;
  description?: string;
  cover_url?: string | null;
  category?: string;
  episode_count?: number;
  tags?: string[];
  rating?: number | null;
}

export interface AdminEpisode {
  id: string;
  drama_id: string;
  title: string;
  episode_number: number;
  duration: number | null;
  video_url: string | null;
  thumbnail_url: string | null;
  description: string | null;
  created_at: string;
  updated_at: string;
}

export interface AdminEpisodeFormData {
  title: string;
  episode_number: number;
  duration?: number | null;
  video_url?: string | null;
  thumbnail_url?: string | null;
  description?: string | null;
}

export interface AdminUser {
  id: string;
  email: string;
  display_name: string | null;
  avatar_url: string | null;
  role: string;
  created_at: string;
}

export interface Pagination {
  page: number;
  page_size: number;
  total: number;
  total_pages: number;
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: Pagination;
}

export interface ApiResponse<T> {
  code: number;
  data: T;
  message: string;
}

export interface ApiError {
  code: number;
  message: string;
}

export class AdminApiError extends Error {
  public readonly status: number;
  public readonly code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'AdminApiError';
    this.status = status;
    this.code = code;
  }
}