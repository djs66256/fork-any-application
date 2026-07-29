'use client';

import { useCallback, useEffect, useReducer, useRef } from 'react';
import { ZodError } from 'zod';
import { config } from '@/lib/config';
import { completeEarnTask, getEarnOverview } from '@/lib/earn/api';
import type {
  CompleteEarnTaskResponse,
  EarnOverviewResponse,
  EarnRestoreContext,
  EarnTask,
  EarnTaskPlayerResult,
} from '@/lib/schemas';
import { ApiError, NetworkError, TimeoutError } from '@/lib/types';
import {
  openEarnTaskPlayer,
  requestEarnLogin,
} from '@/features/earn/bridge/earn-bridge';
import { subscribeEarnHostMessages } from '@/features/earn/bridge/earn-host-sync';

export interface EarnPageState {
  overview: EarnOverviewResponse | null;
  isLoading: boolean;
  errorMessage: string | null;
  isLoggedIn: boolean;
  apiAccessToken: string | null;
  loginPromptVisible: boolean;
  activeTask: EarnTask | null;
  pendingCompletionTaskId: string | null;
  feedbackMessage: string | null;
  pendingRestoreReason: EarnRestoreContext['reason'] | null;
  isCompletingTask: boolean;
}

type EarnPageAction =
  | { type: 'load-start' }
  | { type: 'load-success'; overview: EarnOverviewResponse }
  | { type: 'load-error'; message: string }
  | { type: 'show-login-prompt'; task: EarnTask }
  | { type: 'hide-login-prompt' }
  | { type: 'set-feedback'; message: string | null }
  | { type: 'set-auth'; isLoggedIn: boolean; apiAccessToken: string | null }
  | { type: 'start-completion'; taskId: string }
  | { type: 'complete-success'; result: CompleteEarnTaskResponse }
  | { type: 'complete-error'; message: string }
  | { type: 'require-relogin'; task: EarnTask | null }
  | { type: 'restore-context'; reason: EarnRestoreContext['reason'] }
  | { type: 'clear-restore-context' };

const initialState: EarnPageState = {
  overview: null,
  isLoading: false,
  errorMessage: null,
  isLoggedIn: false,
  apiAccessToken: null,
  loginPromptVisible: false,
  activeTask: null,
  pendingCompletionTaskId: null,
  feedbackMessage: null,
  pendingRestoreReason: null,
  isCompletingTask: false,
};

function getOverviewErrorMessage(error: unknown): string {
  if (error instanceof TimeoutError) {
    return '加载超时，请稍后重试';
  }

  if (error instanceof NetworkError) {
    return '网络异常，请检查后重试';
  }

  if (error instanceof ApiError) {
    if (error.status === 401) {
      return config.earn.browserFeedback.reloginRequired;
    }

    if (error.status >= 500) {
      return '服务暂不可用，请稍后再试';
    }

    return error.message;
  }

  if (error instanceof ZodError) {
    return '赚钱数据暂时不可用，请稍后重试';
  }

  return '服务开小差了，请稍后重试';
}

function getCompletionErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 401) {
    return config.earn.browserFeedback.reloginRequired;
  }

  if (error instanceof ApiError && error.message) {
    return error.message;
  }

  if (error instanceof TimeoutError) {
    return '奖励领取超时，请稍后重试';
  }

  if (error instanceof NetworkError) {
    return '网络异常，请检查后重试';
  }

  if (error instanceof ZodError) {
    return config.earn.browserFeedback.completionFailed;
  }

  return config.earn.browserFeedback.completionFailed;
}

function findTaskById(overview: EarnOverviewResponse | null, taskId: string): EarnTask | null {
  if (!overview) {
    return null;
  }

  if (overview.new_user_task.id === taskId) {
    return overview.new_user_task;
  }

  return overview.cash_tasks.find((task) => task.id === taskId) ?? null;
}

function updateTaskStatus(task: EarnTask, taskId: string): EarnTask {
  if (task.id !== taskId) {
    return task;
  }

  return {
    ...task,
    status: 'completed',
  };
}

function applyTaskCompletion(
  overview: EarnOverviewResponse | null,
  result: CompleteEarnTaskResponse,
): EarnOverviewResponse | null {
  if (!overview) {
    return overview;
  }

  return {
    ...overview,
    coins: result.total_coins,
    new_user_task: updateTaskStatus(overview.new_user_task, result.task_id),
    cash_tasks: overview.cash_tasks.map((task) => updateTaskStatus(task, result.task_id)),
  };
}

function reducer(state: EarnPageState, action: EarnPageAction): EarnPageState {
  switch (action.type) {
    case 'load-start':
      return {
        ...state,
        isLoading: true,
        errorMessage: null,
      };
    case 'load-success':
      return {
        ...state,
        overview: action.overview,
        isLoading: false,
        errorMessage: null,
        isLoggedIn: state.apiAccessToken ? true : action.overview.is_logged_in,
      };
    case 'load-error':
      return {
        ...state,
        isLoading: false,
        errorMessage: action.message,
      };
    case 'show-login-prompt':
      return {
        ...state,
        loginPromptVisible: true,
        activeTask: action.task,
      };
    case 'hide-login-prompt':
      return {
        ...state,
        loginPromptVisible: false,
      };
    case 'set-feedback':
      return {
        ...state,
        feedbackMessage: action.message,
      };
    case 'set-auth':
      return {
        ...state,
        isLoggedIn: action.isLoggedIn,
        apiAccessToken: action.apiAccessToken,
      };
    case 'start-completion':
      return {
        ...state,
        pendingCompletionTaskId: action.taskId,
        isCompletingTask: true,
      };
    case 'complete-success':
      return {
        ...state,
        overview: applyTaskCompletion(state.overview, action.result),
        pendingCompletionTaskId: null,
        isCompletingTask: false,
        feedbackMessage: null,
      };
    case 'complete-error':
      return {
        ...state,
        pendingCompletionTaskId: null,
        isCompletingTask: false,
        feedbackMessage: action.message,
      };
    case 'require-relogin':
      return {
        ...state,
        isLoggedIn: false,
        apiAccessToken: null,
        loginPromptVisible: true,
        activeTask: action.task,
        pendingCompletionTaskId: null,
        isCompletingTask: false,
        feedbackMessage: config.earn.browserFeedback.reloginRequired,
      };
    case 'restore-context':
      return {
        ...state,
        pendingRestoreReason: action.reason,
        loginPromptVisible: false,
        activeTask: null,
        feedbackMessage: null,
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

export function useEarnPage() {
  const [state, dispatch] = useReducer(reducer, initialState);
  const requestIdRef = useRef(0);
  const completionInFlightRef = useRef<Set<string>>(new Set());
  const latestOverviewRef = useRef<EarnOverviewResponse | null>(null);
  const latestTokenRef = useRef<string | null>(null);
  const latestStateRef = useRef(state);

  useEffect(() => {
    latestOverviewRef.current = state.overview;
    latestTokenRef.current = state.apiAccessToken;
    latestStateRef.current = state;
  }, [state]);

  const loadOverview = useCallback(async () => {
    const requestId = ++requestIdRef.current;
    dispatch({ type: 'load-start' });

    try {
      const overview = await getEarnOverview();
      if (requestId !== requestIdRef.current) {
        return;
      }

      dispatch({ type: 'load-success', overview });
    } catch (error) {
      if (requestId !== requestIdRef.current) {
        return;
      }

      dispatch({
        type: 'load-error',
        message: getOverviewErrorMessage(error),
      });
    }
  }, []);

  const completeTaskFlow = useCallback(async (payload: EarnTaskPlayerResult) => {
    const accessToken = latestTokenRef.current;
    if (!accessToken) {
      dispatch({
        type: 'require-relogin',
        task: findTaskById(latestOverviewRef.current, payload.taskId),
      });
      return;
    }

    if (completionInFlightRef.current.has(payload.taskId)) {
      return;
    }

    completionInFlightRef.current.add(payload.taskId);
    dispatch({ type: 'start-completion', taskId: payload.taskId });

    try {
      const result = await completeEarnTask(payload.taskId, accessToken);
      dispatch({ type: 'complete-success', result });
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        dispatch({
          type: 'require-relogin',
          task: findTaskById(latestOverviewRef.current, payload.taskId),
        });
      } else {
        dispatch({
          type: 'complete-error',
          message: getCompletionErrorMessage(error),
        });
      }
    } finally {
      completionInFlightRef.current.delete(payload.taskId);
    }
  }, []);

  useEffect(() => {
    void loadOverview();
  }, [loadOverview]);

  useEffect(() => {
    return subscribeEarnHostMessages((event) => {
      if (event.type === 'earn.syncAuthState') {
        dispatch({
          type: 'set-auth',
          isLoggedIn: event.payload.isLoggedIn,
          apiAccessToken:
            event.payload.isLoggedIn && event.payload.apiAccessToken
              ? event.payload.apiAccessToken
              : null,
        });
        return;
      }

      if (event.type === 'earn.restoreContext') {
        dispatch({ type: 'restore-context', reason: event.payload.reason });
        return;
      }

      if (!event.payload.completed) {
        return;
      }

      void completeTaskFlow(event.payload);
    });
  }, [completeTaskFlow]);

  useEffect(() => {
    if (!state.pendingRestoreReason) {
      return;
    }

    const shouldReload =
      state.pendingRestoreReason === 'container-recreated' ||
      (state.pendingRestoreReason === 'login-return' && state.isLoggedIn);

    dispatch({ type: 'clear-restore-context' });

    if (shouldReload) {
      void loadOverview();
    }
  }, [loadOverview, state.isLoggedIn, state.pendingRestoreReason]);

  const handleTaskClick = useCallback((task: EarnTask) => {
    if (task.action.type === 'placeholder') {
      dispatch({ type: 'set-feedback', message: task.action.feedback });
      return;
    }

    if (!latestStateRef.current.isLoggedIn) {
      dispatch({ type: 'show-login-prompt', task });
      return;
    }

    if (task.action.type !== 'play') {
      dispatch({ type: 'set-feedback', message: config.earn.browserFeedback.taskUnavailable });
      return;
    }

    const result = openEarnTaskPlayer({
      taskId: task.id,
      source: 'earn',
      returnTarget: '/earn',
      videoId: task.action.video_id,
    });

    if (result === 'browser-fallback') {
      dispatch({ type: 'set-feedback', message: config.earn.browserFeedback.taskRequiresApp });
      return;
    }

    dispatch({ type: 'hide-login-prompt' });
    dispatch({ type: 'set-feedback', message: null });
  }, []);

  const retryInitialLoad = useCallback(() => {
    void loadOverview();
  }, [loadOverview]);

  const continueLogin = useCallback(() => {
    const result = requestEarnLogin({
      source: 'earn',
      returnTarget: '/earn',
    });

    if (result === 'browser-fallback') {
      dispatch({
        type: 'set-feedback',
        message: config.earn.browserFeedback.loginUnavailable,
      });
    }
  }, []);

  const cancelLoginPrompt = useCallback(() => {
    dispatch({ type: 'hide-login-prompt' });
  }, []);

  const dismissFeedback = useCallback(() => {
    dispatch({ type: 'set-feedback', message: null });
  }, []);

  const handleHeroLoginClick = useCallback(() => {
    const result = requestEarnLogin({
      source: 'earn',
      returnTarget: '/earn',
    });

    if (result === 'browser-fallback') {
      dispatch({
        type: 'set-feedback',
        message: config.earn.browserFeedback.loginUnavailable,
      });
    }
  }, []);

  return {
    state,
    retryInitialLoad,
    handleTaskClick,
    continueLogin,
    cancelLoginPrompt,
    dismissFeedback,
    handleHeroLoginClick,
  };
}
