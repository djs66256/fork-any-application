import { Drama, DramaListResponseSchema } from '@/lib/schemas';
import { DramaRepositoryInterface, PaginationParams, PaginatedResult } from '@/repositories/interfaces/drama.repository.interface';
import { Errors } from '@/lib/errors';

export class DramaService {
  constructor(private dramaRepository: DramaRepositoryInterface) {}

  async listDramas(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    return DramaListResponseSchema.parse(await this.dramaRepository.findMany(params));
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
