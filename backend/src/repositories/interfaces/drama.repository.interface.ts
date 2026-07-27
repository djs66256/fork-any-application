import {
  BookDramaResponse,
  Drama,
  HotSearchListResponse,
  RankingContentType,
  RankingDrama,
  RankingType,
} from '@/lib/schemas';

export interface PaginationParams {
  page: number;
  pageSize: number;
}

export interface SearchDramasParams extends PaginationParams {
  q: string;
}

export interface RankingParams extends PaginationParams {
  type: RankingType;
  contentType: RankingContentType;
}

export interface AuthContext {
  userId: string;
}

export interface BookDramaParams {
  dramaId: string;
  userId: string;
}

export type BookDramaResult = BookDramaResponse;

export interface PaginatedResult<T> {
  data: T[];
  pagination: {
    page: number;
    page_size: number;
    total: number;
    total_pages: number;
  };
}

export interface DramaRepositoryInterface {
  findMany(params: PaginationParams): Promise<PaginatedResult<Drama>>;
  search(params: SearchDramasParams): Promise<PaginatedResult<Drama>>;
  listRankings(params: RankingParams, authContext?: AuthContext): Promise<PaginatedResult<RankingDrama>>;
  listHotSearches(): Promise<HotSearchListResponse>;
  bookDrama(params: BookDramaParams): Promise<BookDramaResult>;
  findById(id: string): Promise<Drama | null>;
  create(data: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama>;
  update(id: string, data: Partial<Omit<Drama, 'id' | 'created_at' | 'updated_at'>>): Promise<Drama | null>;
  delete(id: string): Promise<boolean>;
  count(): Promise<number>;
}
