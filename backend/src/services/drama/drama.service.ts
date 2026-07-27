import {
  BookDramaResponse,
  BookDramaResponseSchema,
  Drama,
  DramaListResponseSchema,
  HotSearchListResponse,
  HotSearchListResponseSchema,
  RankingDrama,
  RankingListResponseSchema,
} from '@/lib/schemas';
import {
  AuthContext,
  BookDramaParams,
  DramaRepositoryInterface,
  PaginatedResult,
  PaginationParams,
  RankingParams,
  SearchDramasParams,
} from '@/repositories/interfaces/drama.repository.interface';
import { Errors } from '@/lib/errors';

function isAppError(error: unknown): error is Error & { code: string } {
  return error instanceof Error && 'code' in error;
}

export class DramaService {
  constructor(private dramaRepository: DramaRepositoryInterface) {}

  async listDramas(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    return DramaListResponseSchema.parse(await this.dramaRepository.findMany(params));
  }

  async searchDramas(params: SearchDramasParams): Promise<PaginatedResult<Drama>> {
    try {
      return DramaListResponseSchema.parse(await this.dramaRepository.search(params));
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.internal('Invalid drama search result');
    }
  }

  async listRankings(
    params: RankingParams,
    authContext?: AuthContext,
  ): Promise<PaginatedResult<RankingDrama>> {
    try {
      return RankingListResponseSchema.parse(await this.dramaRepository.listRankings(params, authContext));
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.internal('Invalid drama rankings result');
    }
  }

  async listHotSearches(): Promise<HotSearchListResponse> {
    try {
      return HotSearchListResponseSchema.parse(await this.dramaRepository.listHotSearches());
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.internal('Invalid hot search result');
    }
  }

  async bookDrama(params: BookDramaParams): Promise<BookDramaResponse> {
    try {
      return BookDramaResponseSchema.parse(await this.dramaRepository.bookDrama(params));
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }
      throw Errors.internal('Invalid drama booking result');
    }
  }

  async getDramaById(id: string): Promise<Drama> {
    void id;
    throw Errors.notImplemented('getDramaById not implemented');
  }

  async createDrama(data: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama> {
    void data;
    throw Errors.notImplemented('createDrama not implemented');
  }
}
