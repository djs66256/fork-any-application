'use client';

import { useCallback, useEffect, useMemo, useReducer, useRef } from 'react';
import { ZodError } from 'zod';
import { config } from '@/lib/config';
import { fetchMallProducts } from '@/lib/mall/api';
import {
  MallProductIdSchema,
  type MallBanner,
  type MallProduct,
  type MallRestoreContext,
  type MallShortcut,
} from '@/lib/schemas';
import { ApiError, NetworkError, TimeoutError } from '@/lib/types';
import { requestMallLogin, openMallSearch } from '@/features/mall/bridge/mall-bridge';
import { subscribeMallHostMessages } from '@/features/mall/bridge/mall-host-sync';
import {
  buildMallStableFeed,
  mallBanners,
  mallShortcuts,
  mergeMallProducts,
} from '@/features/mall/config/mall-seed';

export interface MallPageState {
  items: MallProduct[];
  page: number;
  hasNextPage: boolean;
  isLoading: boolean;
  isAppending: boolean;
  errorMessage: string | null;
  appendError: string | null;
  loginInterceptVisible: boolean;
  activeProduct: MallProduct | null;
  isLoggedIn: boolean;
  pendingRestoreReason: MallRestoreContext['reason'] | null;
}

interface MallPageHookState extends MallPageState {
  feedbackMessage: string | null;
}

type MallPageAction =
  | { type: 'load-start'; mode: 'initial' | 'append' }
  | {
      type: 'load-success';
      items: MallProduct[];
      page: number;
      hasNextPage: boolean;
      mode: 'initial' | 'append';
    }
  | { type: 'load-error'; message: string; mode: 'initial' | 'append' }
  | { type: 'set-auth'; isLoggedIn: boolean }
  | { type: 'show-login-intercept'; product: MallProduct }
  | { type: 'hide-login-intercept' }
  | { type: 'set-feedback'; message: string | null }
  | { type: 'restore-context'; reason: MallRestoreContext['reason'] }
  | { type: 'clear-restore-context' };

const initialState: MallPageHookState = {
  items: buildMallStableFeed([]),
  page: 0,
  hasNextPage: true,
  isLoading: false,
  isAppending: false,
  errorMessage: null,
  appendError: null,
  loginInterceptVisible: false,
  activeProduct: null,
  isLoggedIn: false,
  pendingRestoreReason: null,
  feedbackMessage: null,
};

function getUserFriendlyMessage(error: unknown): string {
  if (error instanceof TimeoutError) {
    return '加载超时，请稍后重试';
  }

  if (error instanceof NetworkError) {
    return '网络开小差了，请检查连接后重试';
  }

  if (error instanceof ApiError) {
    if (error.status >= 500) {
      return '商城服务暂不可用，请稍后重试';
    }

    return '商城商品暂时无法加载，请稍后重试';
  }

  if (error instanceof ZodError) {
    return '商品数据暂时不可用，请稍后重试';
  }

  return '服务开小差了，请稍后重试';
}

function createBannerTargetProduct(productId: string): MallProduct {
  return {
    id: productId,
    title: '活动商品',
    image_url: '',
    price: 0,
    tags: [],
  };
}

function reducer(state: MallPageHookState, action: MallPageAction): MallPageHookState {
  switch (action.type) {
    case 'load-start':
      if (action.mode === 'initial') {
        return {
          ...state,
          isLoading: true,
          errorMessage: null,
          appendError: null,
        };
      }

      return {
        ...state,
        isAppending: true,
        appendError: null,
      };
    case 'load-success': {
      const items =
        action.mode === 'initial'
          ? action.items
          : mergeMallProducts(state.items, action.items);

      return {
        ...state,
        items,
        page: action.page,
        hasNextPage: action.hasNextPage,
        isLoading: false,
        isAppending: false,
        errorMessage: null,
        appendError: null,
      };
    }
    case 'load-error':
      if (action.mode === 'initial') {
        return {
          ...state,
          isLoading: false,
          errorMessage: action.message,
          items: state.items.length > 0 ? state.items : buildMallStableFeed([]),
        };
      }

      return {
        ...state,
        isAppending: false,
        appendError: action.message,
      };
    case 'set-auth':
      return {
        ...state,
        isLoggedIn: action.isLoggedIn,
      };
    case 'show-login-intercept':
      return {
        ...state,
        loginInterceptVisible: true,
        activeProduct: action.product,
      };
    case 'hide-login-intercept':
      return {
        ...state,
        loginInterceptVisible: false,
      };
    case 'set-feedback':
      return {
        ...state,
        feedbackMessage: action.message,
      };
    case 'restore-context':
      return {
        ...state,
        pendingRestoreReason: action.reason,
        loginInterceptVisible: false,
      };
    case 'clear-restore-context':
      return {
        ...state,
        pendingRestoreReason: null,
      };
    default:
      return state;
  }
}

function normalizeMallProducts(items: MallProduct[], mode: 'initial' | 'append'): MallProduct[] {
  return mode === 'initial' ? buildMallStableFeed(items) : items;
}

function getEffectiveHasNextPage(totalPages: number, page: number): boolean {
  return totalPages > 0 ? page < totalPages : false;
}

export function useMallPage() {
  const [state, dispatch] = useReducer(reducer, initialState);
  const requestIdRef = useRef(0);
  const inFlightPagesRef = useRef<Set<number>>(new Set());

  const loadPage = useCallback(async (page: number, mode: 'initial' | 'append') => {
    if (inFlightPagesRef.current.has(page)) {
      return;
    }

    inFlightPagesRef.current.add(page);
    const requestId = ++requestIdRef.current;
    dispatch({ type: 'load-start', mode });

    try {
      const response = await fetchMallProducts({
        page,
        pageSize: config.mall.pageSize,
      });

      if (requestId !== requestIdRef.current) {
        return;
      }

      dispatch({
        type: 'load-success',
        items: normalizeMallProducts(response.data, mode),
        page: response.pagination.page,
        hasNextPage: getEffectiveHasNextPage(
          response.pagination.total_pages,
          response.pagination.page,
        ),
        mode,
      });
    } catch (error) {
      if (requestId !== requestIdRef.current) {
        return;
      }

      dispatch({
        type: 'load-error',
        message: getUserFriendlyMessage(error),
        mode,
      });
    } finally {
      inFlightPagesRef.current.delete(page);
    }
  }, []);

  useEffect(() => {
    void loadPage(1, 'initial');
  }, [loadPage]);

  useEffect(() => {
    return subscribeMallHostMessages((event) => {
      if (event.type === 'mall.syncAuthState') {
        dispatch({ type: 'set-auth', isLoggedIn: event.payload.isLoggedIn });
        return;
      }

      dispatch({ type: 'restore-context', reason: event.payload.reason });
    });
  }, []);

  useEffect(() => {
    if (!state.pendingRestoreReason) {
      return;
    }

    if (state.pendingRestoreReason === 'container-recreated') {
      dispatch({ type: 'clear-restore-context' });
      void loadPage(1, 'initial');
      return;
    }

    dispatch({ type: 'clear-restore-context' });
  }, [loadPage, state.pendingRestoreReason]);

  const retryInitialLoad = useCallback(() => {
    void loadPage(1, 'initial');
  }, [loadPage]);

  const loadMore = useCallback(() => {
    if (state.isLoading || state.isAppending || !state.hasNextPage) {
      return;
    }

    void loadPage(state.page + 1, 'append');
  }, [loadPage, state.hasNextPage, state.isAppending, state.isLoading, state.page]);

  const retryAppend = useCallback(() => {
    if (state.page < 1) {
      return;
    }

    void loadPage(state.page + 1, 'append');
  }, [loadPage, state.page]);

  const handleSearchClick = useCallback(() => {
    try {
      openMallSearch({
        source: 'mall',
        returnTarget: '/mall',
      });
    } catch {
      dispatch({ type: 'set-feedback', message: '暂时无法打开搜索，请稍后再试' });
    }
  }, []);

  const handlePlaceholderEntryClick = useCallback((entryName: string) => {
    dispatch({ type: 'set-feedback', message: `${entryName}功能开发中` });
  }, []);

  const handleBannerClick = useCallback(
    (banner: MallBanner): string | null => {
      if (banner.target_type === 'search') {
        handleSearchClick();
        return null;
      }

      if (banner.target_type === 'product') {
        const productId = MallProductIdSchema.safeParse(banner.target_value);
        if (!productId.success) {
          dispatch({ type: 'set-feedback', message: '活动商品暂不可用，请稍后再试' });
          return null;
        }

        if (!state.isLoggedIn) {
          const targetProduct =
            state.items.find((item) => item.id === productId.data) ??
            createBannerTargetProduct(productId.data);

          dispatch({ type: 'show-login-intercept', product: targetProduct });
          return null;
        }

        return `/mall/product/${productId.data}`;
      }

      if (banner.target_type === 'web') {
        try {
          const targetUrl = new URL(banner.target_value);
          if (typeof window !== 'undefined') {
            window.location.assign(targetUrl.toString());
          }
          return null;
        } catch {
          dispatch({ type: 'set-feedback', message: '活动链接暂不可用，请稍后再试' });
          return null;
        }
      }

      handlePlaceholderEntryClick('活动');
      return null;
    },
    [handlePlaceholderEntryClick, handleSearchClick, state.isLoggedIn, state.items],
  );

  const handleProductClick = useCallback(
    (product: MallProduct): string | null => {
      if (!state.isLoggedIn) {
        dispatch({ type: 'show-login-intercept', product });
        return null;
      }

      return `/mall/product/${product.id}`;
    },
    [state.isLoggedIn],
  );

  const cancelLoginIntercept = useCallback(() => {
    dispatch({ type: 'hide-login-intercept' });
  }, []);

  const continueLogin = useCallback(() => {
    if (!state.activeProduct) {
      dispatch({ type: 'set-feedback', message: '当前商品信息无效，请稍后重试' });
      return;
    }

    try {
      requestMallLogin({
        source: 'mall',
        productId: state.activeProduct.id,
        returnTarget: '/mall',
      });
    } catch {
      dispatch({ type: 'set-feedback', message: '暂时无法打开登录，请稍后再试' });
    }
  }, [state.activeProduct]);

  const dismissFeedback = useCallback(() => {
    dispatch({ type: 'set-feedback', message: null });
  }, []);

  const shortcuts = useMemo<MallShortcut[]>(() => mallShortcuts, []);

  return {
    state,
    banners: mallBanners,
    shortcuts,
    retryInitialLoad,
    retryAppend,
    loadMore,
    handleSearchClick,
    handleBannerClick,
    handleCartClick: () => handlePlaceholderEntryClick('购物车'),
    handleShortcutClick: (shortcut: MallShortcut) => handlePlaceholderEntryClick(shortcut.title),
    handleProductClick,
    cancelLoginIntercept,
    continueLogin,
    dismissFeedback,
  };
}
