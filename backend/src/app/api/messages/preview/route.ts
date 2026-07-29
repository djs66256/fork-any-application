import { NextResponse } from 'next/server';
import { withErrorHandler } from '@/middleware/error-handler';
import { MessageService } from '@/services/message/message.service';
import {
  getInteractionMessageRepository,
  getSystemMessageRepository,
} from '@/repositories/repository-registry';

export const GET = withErrorHandler(async () => {
  const service = new MessageService(
    getSystemMessageRepository(),
    getInteractionMessageRepository(),
  );
  const result = await service.getPreview();

  if (!result) {
    return new NextResponse(null, { status: 204 });
  }

  return NextResponse.json(result);
});
