import { NextRequest, NextResponse } from 'next/server';
import { DramaMockRepository } from '@/repositories/mock/drama.mock.repository';
import { ClassificationTagsQuerySchema, ClassificationTagsResponseSchema } from '@/lib/schemas';
import { DramaService } from '@/services/drama/drama.service';
import { withErrorHandler } from '@/middleware/error-handler';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const query = ClassificationTagsQuerySchema.parse({
    gender: searchParams.get('gender') ?? undefined,
  });

  const repository = new DramaMockRepository();
  const service = new DramaService(repository);
  const result = await service.listClassificationTags(query);

  return NextResponse.json(ClassificationTagsResponseSchema.parse({ data: result }));
});
