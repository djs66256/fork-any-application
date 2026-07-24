import { Drama } from '@/lib/schemas';
import { DramaRepositoryInterface, PaginationParams, PaginatedResult } from '@/repositories/interfaces/drama.repository.interface';

export class DramaMockRepository implements DramaRepositoryInterface {
  private data: Map<string, Drama> = new Map();

  async findMany(params: PaginationParams): Promise<PaginatedResult<Drama>> {
    const all = Array.from(this.data.values());
    const total = all.length;
    const totalPages = Math.ceil(total / params.pageSize);
    const start = (params.page - 1) * params.pageSize;
    const data = all.slice(start, start + params.pageSize);

    return {
      data,
      pagination: {
        page: params.page,
        page_size: params.pageSize,
        total,
        total_pages: totalPages,
      },
    };
  }

  async findById(id: string): Promise<Drama | null> {
    return this.data.get(id) ?? null;
  }

  async create(data: Omit<Drama, 'id' | 'created_at' | 'updated_at'>): Promise<Drama> {
    const now = new Date().toISOString();
    const id = crypto.randomUUID();
    const drama: Drama = {
      ...data,
      id,
      created_at: now,
      updated_at: now,
    } as Drama;
    this.data.set(id, drama);
    return drama;
  }

  async update(
    id: string,
    data: Partial<Omit<Drama, 'id' | 'created_at' | 'updated_at'>>,
  ): Promise<Drama | null> {
    const existing = this.data.get(id);
    if (!existing) return null;

    const updated: Drama = {
      ...existing,
      ...data,
      updated_at: new Date().toISOString(),
    };
    this.data.set(id, updated);
    return updated;
  }

  async delete(id: string): Promise<boolean> {
    return this.data.delete(id);
  }
}
