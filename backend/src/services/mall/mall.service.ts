import { Errors } from '@/lib/errors';
import { MallProduct, MallProductsQuery, MallProductsResponseSchema } from '@/lib/schemas';
import { MallPaginatedResult, MallRepositoryInterface } from '@/repositories/interfaces/mall.repository.interface';

function isAppError(error: unknown): error is Error & { code: string } {
  return error instanceof Error && 'code' in error;
}

export class MallService {
  constructor(private readonly mallRepository: MallRepositoryInterface) {}

  async listProducts(params: MallProductsQuery): Promise<MallPaginatedResult<MallProduct>> {
    try {
      return MallProductsResponseSchema.parse(await this.mallRepository.listProducts(params));
    } catch (error) {
      if (isAppError(error)) {
        throw error;
      }

      throw Errors.internal('Invalid mall products result');
    }
  }
}
