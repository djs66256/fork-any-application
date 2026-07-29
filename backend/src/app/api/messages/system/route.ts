import { NextRequest, NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { MessageListQuerySchema } from '@/lib/schemas';
import { MessageService } from '@/services/message/message.service';
import {
  getInteractionMessageRepository,
  getSystemMessageRepository,
} from '@/repositories/repository-registry';

export const GET = withErrorHandler(async (request: NextRequest) => {
  const { searchParams } = new URL(request.url);
  const { page, pageSize } = MessageListQuerySchema.parse({
    page: searchParams.get('page') ?? undefined,
    pageSize: searchParams.get('pageSize') ?? undefined,
  });

  const service = new MessageService(
    getSystemMessageRepository(),
    getInteractionMessageRepository(),
  );
  const result = await service.listSystemMessages({ page, pageSize });

  return NextResponse.json(result);
});
