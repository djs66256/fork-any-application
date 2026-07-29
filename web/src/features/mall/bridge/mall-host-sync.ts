import {
  MallHostMessageSchema,
  type MallHostAuthState,
  type MallHostMessage,
  type MallRestoreContext,
} from '@/lib/schemas';

export type MallHostEvent =
  | { type: 'mall.syncAuthState'; payload: MallHostAuthState }
  | { type: 'mall.restoreContext'; payload: MallRestoreContext };

export function parseMallHostMessage(message: unknown): MallHostEvent | null {
  const result = MallHostMessageSchema.safeParse(message);
  if (!result.success) {
    return null;
  }

  const parsed = result.data as MallHostMessage;
  if (parsed.type === 'mall.syncAuthState') {
    return {
      type: parsed.type,
      payload: parsed.payload,
    };
  }

  return {
    type: parsed.type,
    payload: parsed.payload,
  };
}

export function subscribeMallHostMessages(
  handler: (event: MallHostEvent) => void,
): () => void {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  const listener = (event: MessageEvent<unknown>) => {
    const parsed = parseMallHostMessage(event.data);
    if (parsed) {
      handler(parsed);
    }
  };

  window.addEventListener('message', listener);
  return () => {
    window.removeEventListener('message', listener);
  };
}
