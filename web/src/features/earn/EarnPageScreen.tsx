'use client';

import { Container } from '@/components/ui';
import { useEarnPage } from '@/features/earn/hooks/useEarnPage';
import {
  EarnCashTaskSection,
  EarnDailyRewardsGrid,
  EarnFeedbackToast,
  EarnHeroCard,
  EarnLoginPromptOverlay,
  EarnNewUserTaskCard,
} from '@/features/earn/components';
import styles from './EarnPageScreen.module.css';

export function EarnPageScreen() {
  const {
    state,
    retryInitialLoad,
    handleTaskClick,
    continueLogin,
    cancelLoginPrompt,
    dismissFeedback,
    handleHeroLoginClick,
  } = useEarnPage();

  return (
    <Container maxWidth="720px">
      <main className={styles.page}>
        {state.overview ? (
          <>
            <EarnHeroCard
              coins={state.overview.coins}
              isLoggedIn={state.isLoggedIn}
              onLoginClick={handleHeroLoginClick}
            />
            <EarnNewUserTaskCard
              task={state.overview.new_user_task}
              onActionClick={handleTaskClick}
              disabled={state.isCompletingTask}
            />
            <EarnDailyRewardsGrid rewards={state.overview.daily_rewards} />
            <EarnCashTaskSection
              tasks={state.overview.cash_tasks}
              onTaskClick={handleTaskClick}
              disabled={state.isCompletingTask}
            />
          </>
        ) : null}

        {state.isLoading && !state.overview ? (
          <section className={styles.stateCard} aria-label="赚钱首页加载中">
            <div className={styles.skeleton} />
            <div className={styles.skeleton} />
            <div className={styles.skeleton} />
          </section>
        ) : null}

        {!state.isLoading && state.errorMessage ? (
          <section className={styles.stateCard}>
            <h2>赚钱首页加载失败</h2>
            <p>{state.errorMessage}</p>
            <button type="button" onClick={retryInitialLoad} className={styles.retryButton}>
              重试
            </button>
          </section>
        ) : null}

        <EarnFeedbackToast message={state.feedbackMessage} onDismiss={dismissFeedback} />
        <EarnLoginPromptOverlay
          visible={state.loginPromptVisible}
          task={state.activeTask}
          onContinue={continueLogin}
          onCancel={cancelLoginPrompt}
        />
      </main>
    </Container>
  );
}
