import { Drama, DramaListResponseSchema, HotSearchListResponse, HotSearchListResponseSchema } from '@/lib/schemas';
import { DramaRepositoryInterface, PaginationParams, PaginatedResult, SearchDramasParams } from '@/repositories/interfaces/drama.repository.interface';
import { Errors } from '@/lib/errors';

export class DramaService {
  constructor(private dramaRepository: DramaRepositoryInterface) {}

  async listDramas(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    return DramaListResponseSchema.parse(await this.dramaRepository.findMany(params));
  }

  async searchDramas(params: SearchDramasParams): Promise<PaginatedResult<Drama>> {
    try {
      return DramaListResponseSchema.parse(await this.dramaRepository.search(params));
    } catch (error) {
      if (error instanceof Error && 'code' in error) {
        throw error;
      }
      throw Errors.internal('Invalid drama search result');
    }
  }

  async listHotSearches(): Promise<HotSearchListResponse> {
    try {
      return HotSearchListResponseSchema.parse(await this.dramaRepository.listHotSearches());
    } catch (error) {
      if (error instanceof Error && 'code' in error) {
        throw error;
      }
      throw Errors.internal('Invalid hot search result');
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
