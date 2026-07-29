import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { MallPageScreen } from '@/features/mall/MallPageScreen';

const {
  mockPush,
  fetchMallProducts,
  openMallSearch,
  requestMallLogin,
  subscribeMallHostMessages,
} = vi.hoisted(() => ({
  mockPush: vi.fn(),
  fetchMallProducts: vi.fn(),
  openMallSearch: vi.fn(),
  requestMallLogin: vi.fn(),
  subscribeMallHostMessages: vi.fn(),
}));

vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
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

const product = {
  id: '550e8400-e29b-41d4-a716-446655440101',
  title: '轻奢真丝睡衣礼盒',
  image_url: 'https://example.com/product.jpg',
  price: 199,
  tags: ['热卖'],
};

function createResponse(data: typeof product[]) {
  return {
    data,
    pagination: {
      page: 1,
      page_size: 20,
      total: data.length,
      total_pages: 1,
    },
  };
}

describe('MallPageScreen', () => {
  let hostHandler: ((event: unknown) => void) | undefined;

  beforeEach(() => {
    vi.clearAllMocks();
    hostHandler = undefined;
    subscribeMallHostMessages.mockImplementation((handler: (event: unknown) => void) => {
      hostHandler = handler;
      return () => undefined;
    });
  });

  it('renders mall page sections on success', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([product]));

    render(<MallPageScreen />);

    await screen.findByRole('button', { name: `查看商品 ${product.title}` });

    expect(screen.getByRole('button', { name: '打开商城搜索' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '打开购物车入口' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '快捷入口' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '活动横幅' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '热门商品' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '加载更多' })).not.toBeInTheDocument();
    expect(screen.getByText('已经到底啦，去看看其他活动吧。')).toBeInTheDocument();
  });

  it('renders empty state without hiding static sections', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([]));

    render(<MallPageScreen />);

    await waitFor(() => {
      expect(screen.getByText('暂无商品，去看看其他活动吧。')).toBeInTheDocument();
    });

    expect(screen.getByRole('heading', { name: '快捷入口' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '活动横幅' })).toBeInTheDocument();
  });

  it('renders initial error state and retries', async () => {
    fetchMallProducts.mockRejectedValueOnce(new Error('服务开小差了，请稍后重试'));
    fetchMallProducts.mockResolvedValueOnce(createResponse([product]));

    render(<MallPageScreen />);

    expect(await screen.findByText('服务开小差了，请稍后重试')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '重试' }));

    expect(await screen.findByRole('button', { name: `查看商品 ${product.title}` })).toBeInTheDocument();
  });

  it('shows login intercept for anonymous product clicks', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([product]));

    render(<MallPageScreen />);

    fireEvent.click(await screen.findByRole('button', { name: `查看商品 ${product.title}` }));

    expect(screen.getByText('登录后可继续查看商品')).toBeInTheDocument();
    expect(mockPush).not.toHaveBeenCalled();
  });

  it('falls back to browser search flow in browser mode', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([product]));

    render(<MallPageScreen />);

    fireEvent.click(await screen.findByRole('button', { name: '打开商城搜索' }));

    expect(openMallSearch).toHaveBeenCalledWith({
      source: 'mall',
      returnTarget: '/mall',
    });
  });

  it('routes product banner clicks with full banner contract', async () => {
    fetchMallProducts.mockResolvedValueOnce(createResponse([product]));

    render(<MallPageScreen />);

    await screen.findByRole('button', { name: `查看商品 ${product.title}` });

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

    fireEvent.click(await screen.findByRole('button', { name: '活动横幅 mall-banner-newcomer' }));

    expect(mockPush).toHaveBeenCalledWith(`/mall/product/${product.id}`);
  });
});
