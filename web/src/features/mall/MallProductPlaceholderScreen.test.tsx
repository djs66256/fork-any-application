import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { MallProductPlaceholderScreen } from '@/features/mall/MallProductPlaceholderScreen';

describe('MallProductPlaceholderScreen', () => {
  it('renders placeholder detail content for valid product ids', () => {
    render(<MallProductPlaceholderScreen productId="550e8400-e29b-41d4-a716-446655440101" />);

    expect(screen.getByText('商品详情开发中')).toBeInTheDocument();
    expect(screen.getByText(/550e8400-e29b-41d4-a716-446655440101/)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '返回商城' })).toHaveAttribute('href', '/mall');
  });

  it('renders fallback content for invalid product ids', () => {
    render(<MallProductPlaceholderScreen productId="invalid-id" />);

    expect(screen.getByText('商品信息无效，请返回商城首页重新选择。')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '返回商城' })).toHaveAttribute('href', '/mall');
  });
});
