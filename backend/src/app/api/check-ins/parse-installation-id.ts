import { NextRequest } from 'next/server';
import { Errors } from '@/lib/errors';
import { InstallationIdHeaderSchema } from '@/lib/schemas';

export function parseInstallationId(request: NextRequest): string | null {
  const installationId = request.headers.get('X-Installation-Id');
  if (!installationId) {
    return null;
  }

  const parsed = InstallationIdHeaderSchema.safeParse(installationId);
  if (!parsed.success) {
    throw Errors.validationError('Invalid X-Installation-Id');
  }

  return parsed.data;
}
