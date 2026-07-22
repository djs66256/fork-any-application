import { NextResponse } from 'next/server';
import { HealthResponseSchema } from '@/lib/schemas';
import { config } from '@/lib/config';

export async function GET() {
  const data = {
    status: 'ok' as const,
    timestamp: new Date().toISOString(),
    version: config.app.version,
  };

  const parsed = HealthResponseSchema.parse(data);
  return NextResponse.json(parsed);
}
