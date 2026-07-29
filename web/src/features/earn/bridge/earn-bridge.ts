import { config } from '@/lib/config';
import {
  EarnBridgeMessageSchema,
  type EarnBridgeMessage,
  type EarnLoginContext,
  type EarnTaskContext,
} from '@/lib/schemas';

export interface EarnNativeBridge {
  postMessage: (message: EarnBridgeMessage) => void;
}

export type EarnBridgeResult = 'bridge' | 'browser-fallback';

function getWindowBridge(): EarnNativeBridge | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const bridge = (window as typeof window & {
    __EARN_NATIVE_BRIDGE__?: EarnNativeBridge;
  }).__EARN_NATIVE_BRIDGE__;

  if (!config.earn.bridgeEnabled || !bridge) {
    return null;
  }

  return bridge;
}

export function isEarnBridgeAvailable(): boolean {
  return getWindowBridge() !== null;
}

function postBridgeMessage(message: EarnBridgeMessage): void {
  const bridge = getWindowBridge();
  if (!bridge) {
    throw new Error('Earn bridge is unavailable');
  }

  bridge.postMessage(EarnBridgeMessageSchema.parse(message));
}

export function requestEarnLogin(payload: EarnLoginContext): EarnBridgeResult {
  const bridge = getWindowBridge();
  if (!bridge) {
    return 'browser-fallback';
  }

  bridge.postMessage(
    EarnBridgeMessageSchema.parse({
      type: 'earn.requestLogin',
      payload,
    }),
  );

  return 'bridge';
}

export function openEarnTaskPlayer(payload: EarnTaskContext): EarnBridgeResult {
  const bridge = getWindowBridge();
  if (!bridge) {
    return 'browser-fallback';
  }

  postBridgeMessage({
    type: 'earn.openTaskPlayer',
    payload,
  });

  return 'bridge';
}
