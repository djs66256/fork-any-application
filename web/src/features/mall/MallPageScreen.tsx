'use client';

import { useRouter } from 'next/navigation';
import { Container } from '@/components/ui';
import { useMallPage } from '@/features/mall/hooks/useMallPage';
import {
  MallBannerCarousel,
  MallHeader,
  MallLoginInterceptOverlay,
  MallProductGrid,
  MallShortcutGrid,
} from '@/features/mall/components';
import styles from './MallPageScreen.module.css';

export function MallPageScreen() {
  const router = useRouter();
  const {
    state,
    banners,
    shortcuts,
    retryInitialLoad,
    retryAppend,
    loadMore,
    handleSearchClick,
    handleBannerClick,
    handleCartClick,
    handleShortcutClick,
    handleProductClick,
    cancelLoginIntercept,
    continueLogin,
    dismissFeedback,
  } = useMallPage();

  return (
    <Container maxWidth="720px">
      <main className={styles.page}>
        <MallHeader onSearch={handleSearchClick} onCart={handleCartClick} />
        <MallShortcutGrid shortcuts={shortcuts} onShortcutClick={handleShortcutClick} />
        <MallBannerCarousel
          banners={banners}
          onBannerClick={(banner) => {
            const target = handleBannerClick(banner);
            if (target) {
              router.push(target);
            }
          }}
        />
        <MallProductGrid
          items={state.items}
          hasNextPage={state.hasNextPage}
          isLoading={state.isLoading}
          isAppending={state.isAppending}
          appendError={state.appendError}
          errorMessage={state.errorMessage}
          onRetryInitial={retryInitialLoad}
          onLoadMore={loadMore}
          onRetryAppend={retryAppend}
          onProductClick={(product) => {
            const target = handleProductClick(product);
            if (target) {
              router.push(target);
            }
          }}
        />
        {state.feedbackMessage ? (
          <div className={styles.feedback} role="status" aria-live="polite">
            <span>{state.feedbackMessage}</span>
            <button type="button" onClick={dismissFeedback} className={styles.feedbackClose}>
              知道了
            </button>
          </div>
        ) : null}
        <MallLoginInterceptOverlay
          visible={state.loginInterceptVisible}
          product={state.activeProduct}
          onContinueLogin={continueLogin}
          onCancel={cancelLoginIntercept}
        />
      </main>
    </Container>
  );
}
