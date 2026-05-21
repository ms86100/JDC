import { useEffect, useCallback, useState } from 'react';

interface TestEvent {
  eventId: string;
  eventType: string;
  projectId: string;
  timestamp: string;
  payload: Record<string, unknown>;
}

interface TestNotification {
  id: string;
  type: string;
  message: string;
  timestamp: string;
  read: boolean;
  metadata?: Record<string, unknown>;
}

interface UseTestEventsReturn {
  notifications: TestNotification[];
  lastExecutionUpdate: Record<string, unknown> | null;
  lastTestRunUpdate: Record<string, unknown> | null;
  lastCoverageUpdate: Record<string, unknown> | null;
  markAsRead: (id: string) => void;
  clearNotifications: () => void;
}

export function useTestEvents(projectId: string | null): UseTestEventsReturn {
  const [notifications, setNotifications] = useState<TestNotification[]>([]);
  const [lastExecutionUpdate, setLastExecutionUpdate] = useState<Record<string, unknown> | null>(null);
  const [lastTestRunUpdate, setLastTestRunUpdate] = useState<Record<string, unknown> | null>(null);
  const [lastCoverageUpdate, setLastCoverageUpdate] = useState<Record<string, unknown> | null>(null);

  const handleTestEvent = useCallback((event: CustomEvent<TestEvent>) => {
    const { eventType, payload } = event.detail;

    switch (eventType) {
      case 'TEST_RUN_UPDATED':
        setLastTestRunUpdate(payload);
        addNotification({
          id: crypto.randomUUID(),
          type: 'TEST_RUN_UPDATED',
          message: 'Test run has been updated',
          timestamp: new Date().toISOString(),
          read: false,
          metadata: payload,
        });
        break;

      case 'TEST_EXECUTION_STARTED':
        setLastExecutionUpdate(payload);
        addNotification({
          id: crypto.randomUUID(),
          type: 'TEST_EXECUTION_STARTED',
          message: 'Test execution started',
          timestamp: new Date().toISOString(),
          read: false,
          metadata: payload,
        });
        break;

      case 'TEST_EXECUTION_COMPLETED':
        setLastExecutionUpdate(payload);
        addNotification({
          id: crypto.randomUUID(),
          type: 'TEST_EXECUTION_COMPLETED',
          message: 'Test execution completed',
          timestamp: new Date().toISOString(),
          read: false,
          metadata: payload,
        });
        break;

      case 'TEST_EXECUTION_CANCELLED':
        setLastExecutionUpdate(payload);
        addNotification({
          id: crypto.randomUUID(),
          type: 'TEST_EXECUTION_CANCELLED',
          message: 'Test execution cancelled',
          timestamp: new Date().toISOString(),
          read: false,
          metadata: payload,
        });
        break;

      case 'COVERAGE_RECALCULATED':
        setLastCoverageUpdate(payload);
        addNotification({
          id: crypto.randomUUID(),
          type: 'COVERAGE_RECALCULATED',
          message: 'Coverage has been recalculated',
          timestamp: new Date().toISOString(),
          read: false,
          metadata: payload,
        });
        break;

      case 'REQUIREMENT_UPDATED':
        addNotification({
          id: crypto.randomUUID(),
          type: 'REQUIREMENT_UPDATED',
          message: 'Requirement has been updated',
          timestamp: new Date().toISOString(),
          read: false,
          metadata: payload,
        });
        break;

      case 'DEFECT_LINKED':
        addNotification({
          id: crypto.randomUUID(),
          type: 'DEFECT_LINKED',
          message: 'Defect has been linked',
          timestamp: new Date().toISOString(),
          read: false,
          metadata: payload,
        });
        break;

      default:
        console.log('Unknown event type:', eventType);
    }
  }, []);

  const addNotification = (notification: TestNotification) => {
    setNotifications((prev) => {
      const updated = [notification, ...prev];
      // Keep only last 50 notifications
      return updated.slice(0, 50);
    });
  };

  const markAsRead = useCallback((id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  }, []);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  useEffect(() => {
    window.addEventListener('test-event', handleTestEvent as EventListener);
    return () => {
      window.removeEventListener('test-event', handleTestEvent as EventListener);
    };
  }, [handleTestEvent]);

  return {
    notifications,
    lastExecutionUpdate,
    lastTestRunUpdate,
    lastCoverageUpdate,
    markAsRead,
    clearNotifications,
  };
}