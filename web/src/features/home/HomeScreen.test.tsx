import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { HomeScreen } from '@/features/home/HomeScreen';

describe('HomeScreen', () => {
  it('renders app metadata', () => {
    render(<HomeScreen />);
    expect(screen.getByText('ShortDrama')).toBeInTheDocument();
    expect(screen.getByText(/Version: 0\.1\.0/)).toBeInTheDocument();
    expect(screen.getByText(/Environment:/)).toBeInTheDocument();
  });

  it('renders representative navigation links', () => {
    render(<HomeScreen />);

    expect(screen.getByRole('link', { name: '播放页示例' })).toHaveAttribute('href', '/play/sample');
    expect(screen.getByRole('link', { name: '详情页示例' })).toHaveAttribute('href', '/detail/sample');
    expect(screen.getByRole('link', { name: '搜索' })).toHaveAttribute('href', '/search');
    expect(screen.getByRole('link', { name: '榜单' })).toHaveAttribute('href', '/rankings');
    expect(screen.getByRole('link', { name: '商城' })).toHaveAttribute('href', '/mall');
  });
});
