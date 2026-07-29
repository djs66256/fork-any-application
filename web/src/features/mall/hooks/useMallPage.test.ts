import { act, renderHook, waitFor } from '@testing-library/react';
import { ZodError } from 'zod';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useMallPage } from '@/features/mall/hooks/useMallPage';
import type { MallBanner, MallProduct } from '@/lib/schemas';
import { ApiError, NetworkError, TimeoutError } from '@/lib/types';

const {
  fetchMallProducts,
  openMallSearch,
  requestMallLogin,
  subscribeMallHostMessages,
} = vi.hoisted(() => ({
  fetchMallProducts: vi.fn(),
  openMallSearch: vi.fn(),
  requestMallLogin: vi.fn(),
  subscribeMallHostMessages: vi.fn(),
}));

vi.mock('@/lib/mall/api', () => ({
  fetchMallProducts,
}));

vi.mock('@/features/mall/bridge/mall-bridge', () => ({
  openMallSearch,
  requestMallLogin,
}));

vi.mock('@/features/mall/bridge/mall-host-sync', () => ({
  subscribeMallHostMessages,
}));

const firstProduct: MallProduct = {
  id: '550e8400-e29b-41d4-a716-446655440101',
  title: '轻奢真丝睡衣礼盒',
  image_url: 'https://example.com/1.jpg',
  price: 199,
  tags: ['热卖'],
};

const secondProduct: MallProduct = {
  id: '550e8400-e29b-41d4-a716-446655440102',
  title: '夏日清凉风扇',
  image_url: 'https://example.com/2.jpg',
  price: 89,
  tags: ['新品'],
};

const productBanner: MallBanner = {
  id: 'mall-banner-product',
  image_url: 'https://example.com/banner.jpg',
  target_type: 'product',
  target_value: firstProduct.id,
  sort_order: 0,
};

const offscreenProductBanner: MallBanner = {
  id: 'mall-banner-offscreen-product',
  image_url: 'https://example.com/banner-offscreen.jpg',
  target_type: 'product',
  target_value: '550e8400-e29b-41d4-a716-446655440103',
  sort_order: 1,
};

const invalidProductBanner: MallBanner = {
  id: 'mall-banner-invalid-product',
  image_url: 'https://example.com/banner-invalid.jpg',
  target_type: 'product',
  target_value: 'not-a-uuid',
  sort_order: 2,
};

function createResponse(data: MallProduct[], page: number, totalPages: number) {
  return {
    data,
    pagination: {
      page,
      page_size: 20,
      total: totalPages * 20,
      total_pages: totalPages,
    },
  };
}

describe('useMallPage', () => {
  let hostHandler: ((event: unknown) => void) | undefined;

  beforeEach(() => {
    vi.clearAllMocks();
    hostHandler = undefined;
    subscribeMallHostMessages.mockImplementation((handler: (event: unknown) => void) => {
      hostHandler = handler;
      return () => undefined;
    });
  });

  it('loads first page successfully on mount', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 2));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    expect(result.current.state.isLoading).toBe(false);
    expect(result.current.state.errorMessage).toBeNull();
    expect(fetchMallProducts).toHaveBeenCalledWith({ page: 1, pageSize: 20 });
  });

  it('keeps static sections when first page is empty', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.isLoading).toBe(false);
    });

    expect(result.current.state.items).toEqual([]);
    expect(result.current.shortcuts).toHaveLength(5);
    expect(result.current.banners.length).toBeGreaterThan(0);
  });

  it('stores initial load errors and supports retry', async () => {
    fetchMallProducts.mockRejectedValueOnce(new ApiError(400, '网络异常，请稍后重试'));
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.errorMessage).toBe('商城商品暂时无法加载，请稍后重试');
    });

    act(() => {
      result.current.retryInitialLoad();
    });

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });
  });

  it('appends next page without clearing existing items', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 2));
    fetchMallProducts.mockResolvedValueOnce(createResponse([secondProduct], 2, 2));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      result.current.loadMore();
    });

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(2);
    });

    expect(result.current.state.appendError).toBeNull();
  });

  it('keeps existing items when append fails', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 2));
    fetchMallProducts.mockRejectedValueOnce(new ApiError(400, '追加失败'));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      result.current.loadMore();
    });

    await waitFor(() => {
      expect(result.current.state.appendError).toBe('商城商品暂时无法加载，请稍后重试');
    });

    expect(result.current.state.items).toEqual([firstProduct]);
  });

  it('deduplicates in-flight append requests for the same page', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 2));

    let resolveAppend: ((value: ReturnType<typeof createResponse>) => void) | undefined;
    fetchMallProducts.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveAppend = resolve;
        }),
    );

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      result.current.loadMore();
      result.current.loadMore();
    });

    expect(fetchMallProducts).toHaveBeenCalledTimes(2);

    act(() => {
      resolveAppend?.(createResponse([secondProduct], 2, 2));
    });

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(2);
    });
  });

  it('shows login intercept for anonymous product clicks', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    let target: string | null = null;
    act(() => {
      target = result.current.handleProductClick(firstProduct);
    });

    expect(target).toBeNull();
    expect(result.current.state.loginInterceptVisible).toBe(true);
    expect(result.current.state.activeProduct).toEqual(firstProduct);
  });

  it('routes logged-in users to product placeholder pages', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      hostHandler?.({
        type: 'mall.syncAuthState',
        payload: {
          source: 'mall',
          isLoggedIn: true,
          reason: 'initial-load',
          returnTarget: '/mall',
        },
      });
    });

    expect(result.current.handleProductClick(firstProduct)).toBe(
      `/mall/product/${firstProduct.id}`,
    );
  });

  it('requests login and preserves intercept on bridge failure', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));
    requestMallLogin.mockImplementationOnce(() => {
      throw new Error('bridge failed');
    });

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      result.current.handleProductClick(firstProduct);
    });

    act(() => {
      result.current.continueLogin();
    });

    expect(requestMallLogin).toHaveBeenCalledWith({
      source: 'mall',
      productId: firstProduct.id,
      returnTarget: '/mall',
    });
    expect(result.current.state.loginInterceptVisible).toBe(true);
    expect(result.current.state.feedbackMessage).toBe('暂时无法打开登录，请稍后再试');
  });

  it('supports cancelling login intercept', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      result.current.handleProductClick(firstProduct);
    });

    act(() => {
      result.current.cancelLoginIntercept();
    });

    expect(result.current.state.loginInterceptVisible).toBe(false);
  });

  it('maps network, timeout, and invalid response errors to stable user-facing messages', async () => {
    fetchMallProducts.mockRejectedValueOnce(new NetworkError('Failed to fetch'));
    const { result: networkResult } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(networkResult.current.state.errorMessage).toBe('网络开小差了，请检查连接后重试');
    });

    fetchMallProducts.mockReset();
    fetchMallProducts.mockRejectedValueOnce(new TimeoutError('Request timed out'));
    const { result: timeoutResult } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(timeoutResult.current.state.errorMessage).toBe('加载超时，请稍后重试');
    });

    fetchMallProducts.mockReset();
    fetchMallProducts.mockRejectedValueOnce(new ZodError([]));
    const { result: schemaResult } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(schemaResult.current.state.errorMessage).toBe('商品数据暂时不可用，请稍后重试');
    });
  });

  it('stops exposing load more actions after the last page', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      result.current.loadMore();
    });

    expect(fetchMallProducts).toHaveBeenCalledTimes(1);
    expect(result.current.state.hasNextPage).toBe(false);
  });

  it('falls back to search handling errors with a feedback message', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));
    openMallSearch.mockImplementationOnce(() => {
      throw new Error('search failed');
    });

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      result.current.handleSearchClick();
    });

    expect(result.current.state.feedbackMessage).toBe('暂时无法打开搜索，请稍后再试');
  });

  it('updates auth state and restores first page on container recreation', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));
    fetchMallProducts.mockResolvedValueOnce(createResponse([secondProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      hostHandler?.({
        type: 'mall.syncAuthState',
        payload: {
          source: 'mall',
          isLoggedIn: true,
          reason: 'login-success',
          returnTarget: '/mall',
        },
      });
      result.current.handleProductClick(firstProduct);
      hostHandler?.({
        type: 'mall.restoreContext',
        payload: {
          source: 'mall',
          reason: 'container-recreated',
          returnTarget: '/mall',
        },
      });
    });

    await waitFor(() => {
      expect(result.current.state.items).toEqual([secondProduct]);
    });

    expect(result.current.state.isLoggedIn).toBe(true);
    expect(result.current.state.pendingRestoreReason).toBeNull();
    expect(result.current.state.loginInterceptVisible).toBe(false);
  });

  it('closes intercept and clears restore marker for login return without reloading', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      result.current.handleProductClick(firstProduct);
      hostHandler?.({
        type: 'mall.restoreContext',
        payload: {
          source: 'mall',
          reason: 'login-return',
          returnTarget: '/mall',
          preserveScroll: true,
        },
      });
    });

    await waitFor(() => {
      expect(result.current.state.pendingRestoreReason).toBeNull();
    });

    expect(result.current.state.loginInterceptVisible).toBe(false);
    expect(fetchMallProducts).toHaveBeenCalledTimes(1);
  });

  it('routes logged-in product banner clicks to placeholder pages', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      hostHandler?.({
        type: 'mall.syncAuthState',
        payload: {
          source: 'mall',
          isLoggedIn: true,
          reason: 'initial-load',
          returnTarget: '/mall',
        },
      });
    });

    expect(result.current.handleBannerClick(productBanner)).toBe(`/mall/product/${firstProduct.id}`);
  });

  it('shows login intercept for anonymous product banner clicks even when product is offscreen', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      expect(result.current.handleBannerClick(offscreenProductBanner)).toBeNull();
    });

    expect(result.current.state.loginInterceptVisible).toBe(true);
    expect(result.current.state.activeProduct).toEqual({
      id: offscreenProductBanner.target_value,
      title: '活动商品',
      image_url: '',
      price: 0,
      tags: [],
    });

    act(() => {
      result.current.continueLogin();
    });

    expect(requestMallLogin).toHaveBeenCalledWith({
      source: 'mall',
      productId: offscreenProductBanner.target_value,
      returnTarget: '/mall',
    });
  });

  it('shows feedback when product banner target is invalid', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([firstProduct], 1, 1));

    const { result } = renderHook(() => useMallPage());

    await waitFor(() => {
      expect(result.current.state.items).toHaveLength(1);
    });

    act(() => {
      result.current.handleBannerClick(invalidProductBanner);
    });

    expect(result.current.state.feedbackMessage).toBe('活动商品暂不可用，请稍后再试');
  });
});
