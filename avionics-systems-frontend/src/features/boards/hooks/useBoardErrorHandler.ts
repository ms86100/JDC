import React, { useState, useCallback } from 'react';

interface UseBoardErrorHandlerOptions {
  onError?: (error: Error) => void;
  onRetry?: () => void;
}

interface BoardError {
  id: string;
  type: 'network' | 'validation' | 'permission' | 'conflict' | 'unknown';
  message: string;
  retryable: boolean;
  timestamp: Date;
}

export function useBoardErrorHandler({ onError, onRetry }: UseBoardErrorHandlerOptions = {}) {
  const [errors, setErrors] = useState<BoardError[]>([]);
  const [isRetrying, setIsRetrying] = useState(false);

  const addError = useCallback((error: Error | string, type: BoardError['type'] = 'unknown') => {
    const boardError: BoardError = {
      id: `err-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      type,
      message: typeof error === 'string' ? error : error.message || 'An unexpected error occurred',
      retryable: type === 'network' || type === 'conflict',
      timestamp: new Date(),
    };

    setErrors((prev) => [...prev.slice(-5), boardError]);
    onError?.(typeof error === 'string' ? new Error(error) : error);

    setTimeout(() => {
      setErrors((prev) => prev.filter((e) => e.id !== boardError.id));
    }, 10000);

    return boardError.id;
  }, [onError]);

  const removeError = useCallback((errorId: string) => {
    setErrors((prev) => prev.filter((e) => e.id !== errorId));
  }, []);

  const clearErrors = useCallback(() => {
    setErrors([]);
  }, []);

  const retry = useCallback(async () => {
    setIsRetrying(true);
    try {
      await onRetry?.();
      clearErrors();
    } finally {
      setIsRetrying(false);
    }
  }, [onRetry, clearErrors]);

  return {
    errors,
    addError,
    removeError,
    clearErrors,
    retry,
    isRetrying,
    hasErrors: errors.length > 0,
  };
}

interface UseOptimisticUpdateOptions<T> {
  onSuccess?: (data: T) => void;
  onError?: (error: Error, rollback: () => void) => void;
}

export function useOptimisticUpdate<T>({
  onSuccess,
  onError,
}: UseOptimisticUpdateOptions<T> = {}) {
  const [pendingUpdates, setPendingUpdates] = useState<Map<string, T>>(new Map());

  const startUpdate = useCallback((key: string, value: T) => {
    setPendingUpdates((prev) => new Map(prev).set(key, value));
  }, []);

  const confirmUpdate = useCallback((key: string) => {
    setPendingUpdates((prev) => {
      const next = new Map(prev);
      next.delete(key);
      return next;
    });
  }, []);

  const rollback = useCallback((key: string) => {
    setPendingUpdates((prev) => {
      const next = new Map(prev);
      next.delete(key);
      return next;
    });
  }, []);

  const getPendingValue = useCallback(
    (key: string): T | undefined => pendingUpdates.get(key),
    [pendingUpdates],
  );

  return {
    pendingUpdates,
    startUpdate,
    confirmUpdate,
    rollback,
    getPendingValue,
    hasPendingUpdates: pendingUpdates.size > 0,
  };
}

interface UseBoardRetryOptions {
  maxRetries?: number;
  baseDelay?: number;
  maxDelay?: number;
}

export function useBoardRetry({
  maxRetries = 3,
  baseDelay = 1000,
  maxDelay = 30000,
}: UseBoardRetryOptions = {}) {
  const [attempts, setAttempts] = useState(0);
  const [isRetrying, setIsRetrying] = useState(false);

  const execute = useCallback(async <T>(
    fn: () => Promise<T>,
    onError?: (error: Error, attempt: number) => void,
  ): Promise<T> => {
    let lastError: Error;

    for (let i = 0; i <= maxRetries; i++) {
      try {
        setAttempts(i);
        const result = await fn();
        return result;
      } catch (error) {
        lastError = error instanceof Error ? error : new Error(String(error));
        onError?.(lastError, i);

        if (i < maxRetries) {
          setIsRetrying(true);
          const delay = Math.min(baseDelay * Math.pow(2, i), maxDelay);
          await new Promise((resolve) => setTimeout(resolve, delay));
        }
      }
    }

    setIsRetrying(false);
    throw lastError!;
  }, [maxRetries, baseDelay, maxDelay]);

  const reset = useCallback(() => {
    setAttempts(0);
    setIsRetrying(false);
  }, []);

  return {
    execute,
    attempts,
    isRetrying,
    reset,
  };
}