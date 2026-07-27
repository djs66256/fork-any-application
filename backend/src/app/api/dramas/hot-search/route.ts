import { NextResponse } from 'next/server';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { DramaService } from '@/services/drama/drama.service';
import { withErrorHandler } from '@/middleware/error-handler';

export const GET = withErrorHandler(async () => {
  const repository = new DramaMockRepository();
  const service = new DramaService(repository);
  const result = await service.listHotSearches();

  return NextResponse.json(result);
});
