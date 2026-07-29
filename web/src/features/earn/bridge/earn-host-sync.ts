import {
  EarnHostMessageSchema,
  type EarnHostAuthState,
  type EarnHostMessage,
  type EarnRestoreContext,
  type EarnTaskPlayerResult,
} from '@/lib/schemas';

export type EarnHostEvent =
  | { type: 'earn.syncAuthState'; payload: EarnHostAuthState }
  | { type: 'earn.restoreContext'; payload: EarnRestoreContext }
  | { type: 'earn.completeTask'; payload: EarnTaskPlayerResult };

export function parseEarnHostMessage(message: unknown): EarnHostEvent | null {
  const result = EarnHostMessageSchema.safeParse(message);
  if (!result.success) {
    return null;
  }

  const parsed = result.data as EarnHostMessage;
  return {
    type: parsed.type,
    payload: parsed.payload,
  } as EarnHostEvent;
}

export function subscribeEarnHostMessages(
  handler: (event: EarnHostEvent) => void,
): () => void {
  if (typeof window === 'undefined') {
    return () => undefined;
  }

  const listener = (event: Event) => {
    if (!(event instanceof CustomEvent) || event.type !== 'earn.hostMessage') {
      return;
    }

    const parsed = parseEarnHostMessage(event.detail);
    if (parsed) {
      handler(parsed);
    }
  };

  window.addEventListener('earn.hostMessage', listener as EventListener);
  return () => {
    window.removeEventListener('earn.hostMessage', listener as EventListener);
  };
}
