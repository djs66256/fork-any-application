import { Drama } from '@/lib/schemas';
import { DramaRepositoryInterface, PaginationParams, PaginatedResult } from '@/repositories/interfaces/drama.repository.interface';
import { Errors } from '@/lib/errors';

export class DramaService {
  constructor(private dramaRepository: DramaRepositoryInterface) {}

  async listDramas(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    return this.dramaRepository.findMany(params);
  }

  async getDramaById(_id: string): Promise<Drama> {
    throw Errors.notImplemented('getDramaById not implemented');
  }

  async createDrama(_data: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama> {
    throw Errors.notImplemented('createDrama not implemented');
  }
}
