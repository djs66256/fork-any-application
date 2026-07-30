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

function MallTabBar() {
  return (
    <nav className={styles.tabBar} aria-label="商城主导航">
      <button type="button" className={styles.tabItem} aria-label="首页">
        <span className={styles.tabLabel}>首页</span>
      </button>
      <button type="button" className={styles.tabItem} aria-label="剧场">
        <span className={styles.tabLabel}>剧场</span>
      </button>
      <button type="button" className={`${styles.tabItem} ${styles.tabItemActive}`} aria-current="page" aria-label="商城">
        <span className={styles.tabLabel}>商城</span>
      </button>
      <button type="button" className={styles.tabItem} aria-label="赚钱">
        <span className={styles.tabBadge}>签到</span>
        <span className={styles.tabLabel}>赚钱</span>
      </button>
      <button type="button" className={styles.tabItem} aria-label="我的">
        <span className={styles.tabDot} aria-hidden="true" />
        <span className={styles.tabLabel}>我的</span>
      </button>
    </nav>
  );
}

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
    <Container maxWidth="760px">
      <main className={styles.page}>
        <section className={styles.hero}>
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
        </section>
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
        <MallTabBar />
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
