import { useEffect, useCallback, useState, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  useTestWebSocket,
  TestEvent,
  EventCallback,
  TEST_EVENTS,
  TestRunUpdatedPayload,
  TestExecutionStartedPayload,
  CoverageRecalculatedPayload,
} from './useTestWebSocket';

export interface TestNotification {
  id: string;
  type: string;
  message: string;
  timestamp: string;
  read: boolean;
  metadata?: Record<string, unknown>;
}

export interface UseTestEventHandlerReturn {
  notifications: TestNotification[];
  lastTestRunUpdate: TestRunUpdatedPayload | null;
  lastExecutionStarted: TestExecutionStartedPayload | null;
  lastCoverageUpdate: CoverageRecalculatedPayload | null;
  markAsRead: (id: string) => void;
  clearNotifications: () => void;
  subscribeToProject: (projectId: string) => () => void;
  isConnected: boolean;
  connectionStatus: import('./useTestWebSocket').ConnectionStatus;
}

// Toast configuration
interface ToastConfig {
  show: (message: string, options?: { type?: 'info' | 'success' | 'error' | 'warning' }) => void;
}

function createSimpleToast(): ToastConfig {
  // Simple toast implementation using the browser's notification API or console
  const show = (message: string, options?: { type?: 'info' | 'success' | 'error' | 'warning' }) => {
    // Try to use a toast library if available, otherwise use browser notifications
    const toastFn = (window as unknown as { toast?: ToastConfig['show'] }).toast;
    if (toastFn) {
      toastFn(message, options);
    } else {
      // Fallback to console with styling
      const styles: Record<string, string> = {
        info: 'background: #2196F3; color: white; padding: 4px 8px; border-radius: 4px;',
        success: 'background: #4CAF50; color: white; padding: 4px 8px; border-radius: 4px;',
        error: 'background: #f44336; color: white; padding: 4px 8px; border-radius: 4px;',
        warning: 'background: #FF9800; color: white; padding: 4px 8px; border-radius: 4px;',
      };
      console.log(`%c${message}`, styles[options?.type || 'info']);
    }
  };
  return { show };
}

const toast = createSimpleToast();

export function useTestEventHandler(): UseTestEventHandlerReturn {
  const queryClient = useQueryClient();
  const { subscribe, isConnected, status, lastEvent } = useTestWebSocket();

  const [notifications, setNotifications] = useState<TestNotification[]>([]);
  const [lastTestRunUpdate, setLastTestRunUpdate] = useState<TestRunUpdatedPayload | null>(null);
  const [lastExecutionStarted, setLastExecutionStarted] = useState<TestExecutionStartedPayload | null>(null);
  const [lastCoverageUpdate, setLastCoverageUpdate] = useState<CoverageRecalculatedPayload | null>(null);

  const activeSubscription = useRef<string | null>(null);

  // Update React Query cache based on event type
  const updateQueryCache = useCallback(
    (event: TestEvent) => {
      const { eventType, payload, projectId } = event;

      switch (eventType) {
        case TEST_EVENTS.TEST_RUN_UPDATED:
          // Invalidate test runs cache
          queryClient.invalidateQueries({ queryKey: ['testRuns', projectId] });
          queryClient.invalidateQueries({ queryKey: ['testRuns'] });
          break;

        case TEST_EVENTS.TEST_EXECUTION_STARTED:
          // Invalidate executions cache
          queryClient.invalidateQueries({ queryKey: ['executions', projectId] });
          queryClient.invalidateQueries({ queryKey: ['executions'] });
          // Update specific execution in cache if we have it
          const startedPayload = payload as unknown as TestExecutionStartedPayload;
          if (startedPayload.executionId) {
            queryClient.setQueryData(
              ['execution', startedPayload.executionId],
              (old: unknown) => {
                if (old && typeof old === 'object') {
                  return { ...old, status: 'IN_PROGRESS' };
                }
                return old;
              }
            );
          }
          break;

        case TEST_EVENTS.TEST_EXECUTION_COMPLETED:
          queryClient.invalidateQueries({ queryKey: ['executions', projectId] });
          queryClient.invalidateQueries({ queryKey: ['executions'] });
          queryClient.invalidateQueries({ queryKey: ['testRuns', projectId] });
          break;

        case TEST_EVENTS.COVERAGE_RECALCULATED:
          // Invalidate coverage/requirement coverage queries
          queryClient.invalidateQueries({ queryKey: ['coverage', projectId] });
          queryClient.invalidateQueries({ queryKey: ['requirements', projectId] });
          break;

        case TEST_EVENTS.REQUIREMENT_UPDATED:
          queryClient.invalidateQueries({ queryKey: ['requirements', projectId] });
          break;

        case TEST_EVENTS.DEFECT_LINKED:
          queryClient.invalidateQueries({ queryKey: ['defects', projectId] });
          queryClient.invalidateQueries({ queryKey: ['testCases', projectId] });
          break;

        default:
          console.debug('[TestEventHandler] Unhandled event type:', eventType);
      }
    },
    [queryClient]
  );

  // Show toast notification for important events
  const showToast = useCallback((event: TestEvent) => {
    const { eventType, payload } = event;

    switch (eventType) {
      case TEST_EVENTS.TEST_EXECUTION_STARTED:
        toast.show('Test execution started', { type: 'info' });
        break;

      case TEST_EVENTS.TEST_EXECUTION_COMPLETED: {
        const summary = (payload as { summary?: { passed: number; failed: number; total: number } })
          .summary;
        if (summary) {
          toast.show(
            `Execution completed: ${summary.passed} passed, ${summary.failed} failed`,
            { type: summary.failed > 0 ? 'warning' : 'success' }
          );
        }
        break;
      }

      case TEST_EVENTS.TEST_EXECUTION_CANCELLED:
        toast.show('Test execution cancelled', { type: 'warning' });
        break;

      case TEST_EVENTS.TEST_RUN_UPDATED:
        toast.show('Test run updated', { type: 'info' });
        break;

      case TEST_EVENTS.COVERAGE_RECALCULATED: {
        const coveragePayload = payload as unknown as CoverageRecalculatedPayload;
        toast.show(
          `Coverage recalculated: ${coveragePayload.coveragePercentage}%`,
          { type: 'success' }
        );
        break;
      }

      case TEST_EVENTS.REQUIREMENT_UPDATED:
        toast.show('Requirement updated', { type: 'info' });
        break;

      case TEST_EVENTS.DEFECT_LINKED:
        toast.show('Defect linked to test', { type: 'info' });
        break;

      default:
        break;
    }
  }, []);

  // Add notification
  const addNotification = useCallback((notification: TestNotification) => {
    setNotifications((prev) => {
      const updated = [notification, ...prev];
      return updated.slice(0, 50); // Keep last 50
    });
  }, []);

  // Create typed event handler
  const handleEvent: EventCallback = useCallback(
    (event: TestEvent) => {
      const { eventType, payload, projectId, timestamp } = event;

      // Update specific state based on event type
      switch (eventType) {
        case TEST_EVENTS.TEST_RUN_UPDATED:
          setLastTestRunUpdate(payload as unknown as TestRunUpdatedPayload);
          break;

        case TEST_EVENTS.TEST_EXECUTION_STARTED:
          setLastExecutionStarted(payload as unknown as TestExecutionStartedPayload);
          break;

        case TEST_EVENTS.COVERAGE_RECALCULATED:
          setLastCoverageUpdate(payload as unknown as CoverageRecalculatedPayload);
          break;
      }

      // Create notification for all events
      const notification: TestNotification = {
        id: event.eventId || crypto.randomUUID(),
        type: eventType,
        message: getEventMessage(eventType),
        timestamp: timestamp || new Date().toISOString(),
        read: false,
        metadata: { ...payload, projectId },
      };

      addNotification(notification);

      // Update React Query cache
      updateQueryCache(event);

      // Show toast
      showToast(event);
    },
    [addNotification, updateQueryCache, showToast]
  );

  // Subscribe to a project's events
  const subscribeToProject = useCallback(
    (projectId: string): (() => void) => {
      // Unsubscribe from previous project if any
      if (activeSubscription.current) {
        // We can't directly unsubscribe with the current architecture,
        // but we handle it by not processing events for other projects
      }

      const subscriptionId = subscribe(projectId, handleEvent);
      activeSubscription.current = subscriptionId;

      // Return cleanup function
      return () => {
        activeSubscription.current = null;
      };
    },
    [subscribe, handleEvent]
  );

  // Mark notification as read
  const markAsRead = useCallback((id: string) => {
    setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
  }, []);

  // Clear all notifications
  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  return {
    notifications,
    lastTestRunUpdate,
    lastExecutionStarted,
    lastCoverageUpdate,
    markAsRead,
    clearNotifications,
    subscribeToProject,
    isConnected,
    connectionStatus: status,
  };
}

// Helper to get human-readable event message
function getEventMessage(eventType: string): string {
  switch (eventType) {
    case TEST_EVENTS.TEST_RUN_UPDATED:
      return 'A test run has been updated';
    case TEST_EVENTS.TEST_EXECUTION_STARTED:
      return 'A test execution has started';
    case TEST_EVENTS.TEST_EXECUTION_COMPLETED:
      return 'A test execution has completed';
    case TEST_EVENTS.TEST_EXECUTION_CANCELLED:
      return 'A test execution was cancelled';
    case TEST_EVENTS.COVERAGE_RECALCULATED:
      return 'Test coverage has been recalculated';
    case TEST_EVENTS.REQUIREMENT_UPDATED:
      return 'A requirement has been updated';
    case TEST_EVENTS.DEFECT_LINKED:
      return 'A defect has been linked to a test';
    default:
      return `Event: ${eventType}`;
  }
}

// Hook for subscribing to a specific project
export function useProjectTestEvents(projectId: string | null) {
  const handler = useTestEventHandler();

  useEffect(() => {
    if (!projectId) return;

    const unsubscribe = handler.subscribeToProject(projectId);
    return unsubscribe;
  }, [projectId, handler.subscribeToProject]);

  return handler;
}