import { config } from '@/lib/config';
import {
  MallBridgeMessageSchema,
  type MallBridgeMessage,
  type MallLoginContext,
  type MallSearchContext,
} from '@/lib/schemas';

export interface MallNativeBridge {
  postMessage: (message: MallBridgeMessage) => void;
}

function getWindowBridge(): MallNativeBridge | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const bridge = (window as typeof window & {
    __MALL_NATIVE_BRIDGE__?: MallNativeBridge;
  }).__MALL_NATIVE_BRIDGE__;

  if (!config.mall.bridgeEnabled || !bridge) {
    return null;
  }

  return bridge;
}

export function isMallBridgeAvailable(): boolean {
  return getWindowBridge() !== null;
}

function postBridgeMessage(message: MallBridgeMessage): void {
  const bridge = getWindowBridge();
  if (!bridge) {
    throw new Error('Mall bridge is unavailable');
  }

  bridge.postMessage(MallBridgeMessageSchema.parse(message));
}

export function openMallSearch(payload: MallSearchContext): 'bridge' | 'browser-fallback' {
  const bridge = getWindowBridge();
  if (bridge) {
    bridge.postMessage(
      MallBridgeMessageSchema.parse({
        type: 'mall.openSearch',
        payload,
      }),
    );
    return 'bridge';
  }

  if (typeof window !== 'undefined') {
    window.location.assign(config.mall.searchFallbackRoute);
  }

  return 'browser-fallback';
}

export function requestMallLogin(payload: MallLoginContext): void {
  postBridgeMessage({
    type: 'mall.requestLogin',
    payload,
  });
}
