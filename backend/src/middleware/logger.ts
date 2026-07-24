import { NextRequest } from 'next/server';
import type { RouteHandler } from './error-handler';

export function withLogger(handler: RouteHandler): RouteHandler {
  return async (request: NextRequest, context: unknown) => {
    const start = Date.now();
    const response = await handler(request, context);
    const duration = Date.now() - start;

    console.log(`[${request.method}] [${request.nextUrl.pathname}] [${duration}ms]`);

    return response;
  };
}
