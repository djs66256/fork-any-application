import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import { PlaceholderRouteScreen } from '@/features/placeholder-route/PlaceholderRouteScreen';

describe('PlaceholderRouteScreen', () => {
  it('renders title and description', () => {
    render(
      <PlaceholderRouteScreen
        title="搜索"
        description="搜索页面占位内容，后续 PRD 会在这里接入真实内容。"
      />,
    );

    expect(screen.getByText('搜索')).toBeInTheDocument();
    expect(screen.getByText('搜索页面占位内容，后续 PRD 会在这里接入真实内容。')).toBeInTheDocument();
  });
});
