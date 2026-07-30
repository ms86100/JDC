import { useEffect, useRef, useCallback, useState } from 'react';
import { useAuth } from '../../auth/context/AuthContext';
import { Client } from '@stomp/stompjs';

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected' | 'error';

export interface TestEvent {
  eventId: string;
  eventType: string;
  projectId: string;
  timestamp: string;
  payload: Record<string, unknown>;
}

export type EventCallback = (event: TestEvent) => void;
export type SubscriptionId = string;

export interface UseTestWebSocketReturn {
  isConnected: boolean;
  status: ConnectionStatus;
  lastEvent: TestEvent | null;
  connect: () => void;
  disconnect: () => void;
  subscribe: (projectId: string, callback: EventCallback) => SubscriptionId;
  unsubscribe: (subscriptionId: SubscriptionId) => void;
  sendMessage: (destination: string, body: Record<string, unknown>) => void;
  reconnect: () => void;
}

// STOMP endpoint from backend WebSocketConfig - use /ws/issues since that's what the backend exposes
const STOMP_ENDPOINT = '/ws/issues';
const TOPIC_PREFIX = '/topic/test-events';

// Reconnection config
const INITIAL_RECONNECT_DELAY = 1000;
const MAX_RECONNECT_DELAY = 30000;
const RECONNECT_MULTIPLIER = 1.5;

// Connection status callbacks
type StatusCallback = (status: ConnectionStatus) => void;
const statusListeners = new Set<StatusCallback>();

// Subscribe to connection status changes
export function useConnectionStatus(callback: StatusCallback) {
  useEffect(() => {
    statusListeners.add(callback);
    return () => {
      statusListeners.delete(callback);
    };
  }, [callback]);
}

function notifyStatusChange(status: ConnectionStatus) {
  statusListeners.forEach((cb) => cb(status));
}

// Singleton STOMP client
let stompClient: Client | null = null;
let reconnectAttempt = 0;
let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

// Active subscriptions map
const activeSubscriptions = new Map<string, { projectId: string; callback: EventCallback }>();

function getWebSocketUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host;
  return `${protocol}//${host}${STOMP_ENDPOINT}`;
}

function createClient(): Client {
  const client = new Client({
    brokerURL: getWebSocketUrl(),
    reconnectDelay: 0, // We handle reconnection manually
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    // Log debug info in development
    debug: (str) => {
      if (import.meta.env.DEV) {
        console.debug('[STOMP]', str);
      }
    },
  });

  client.onConnect = () => {
    reconnectAttempt = 0;
    notifyStatusChange('connected');

    // Re-subscribe to all active subscriptions
    activeSubscriptions.forEach((sub, subId) => {
      doSubscribe(subId, sub.projectId, sub.callback);
    });
  };

  client.onDisconnect = () => {
    notifyStatusChange('disconnected');
    scheduleReconnect();
  };

  client.onStompError = (frame) => {
    console.error('[STOMP] Error:', frame.headers.message);
    notifyStatusChange('error');
    scheduleReconnect();
  };

  client.onWebSocketError = (event) => {
    console.error('[STOMP] WebSocket error:', event);
    notifyStatusChange('error');
    scheduleReconnect();
  };

  return client;
}

function scheduleReconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
  }

  // Exponential backoff
  const delay = Math.min(
    INITIAL_RECONNECT_DELAY * Math.pow(RECONNECT_MULTIPLIER, reconnectAttempt),
    MAX_RECONNECT_DELAY
  );

  reconnectAttempt++;

  console.log(`[STOMP] Scheduling reconnect in ${delay}ms (attempt ${reconnectAttempt})`);

  notifyStatusChange('connecting');

  reconnectTimer = setTimeout(() => {
    if (stompClient && !stompClient.connected) {
      stompClient.activate();
    }
  }, delay);
}

function doSubscribe(subscriptionId: string, projectId: string, callback: EventCallback) {
  if (!stompClient || !stompClient.connected) {
    console.warn('[STOMP] Cannot subscribe - not connected');
    return;
  }

  const destination = `${TOPIC_PREFIX}/${projectId}`;

  const subscription = stompClient.subscribe(destination, (message) => {
    try {
      const event: TestEvent = JSON.parse(message.body);
      callback(event);

      // Dispatch to global event system for other listeners
      window.dispatchEvent(new CustomEvent('test-event', { detail: event }));

      // Dispatch specific event types
      window.dispatchEvent(new CustomEvent(`test-event:${event.eventType}`, { detail: event }));
    } catch (err) {
      console.error('[STOMP] Failed to parse message:', err);
    }
  });

  // Store subscription info
  activeSubscriptions.set(subscriptionId, { projectId, callback });

  console.log(`[STOMP] Subscribed to ${destination} (id: ${subscriptionId})`);
}

function doUnsubscribe(subscriptionId: string) {
  activeSubscriptions.delete(subscriptionId);
  console.log(`[STOMP] Unsubscribed (id: ${subscriptionId})`);
}

export function useTestWebSocket(): UseTestWebSocketReturn {
  const { user } = useAuth();
  const [status, setStatus] = useState<ConnectionStatus>('disconnected');
  const [lastEvent, setLastEvent] = useState<TestEvent | null>(null);
  const isManualDisconnect = useRef(false);

  // Listen to global status changes
  useEffect(() => {
    const handleStatusChange = (newStatus: ConnectionStatus) => {
      setStatus(newStatus);
    };
    statusListeners.add(handleStatusChange);
    return () => {
      statusListeners.delete(handleStatusChange);
    };
  }, []);

  const connect = useCallback(() => {
    if (!user) {
      console.log('[STOMP] Skipping connect - no authenticated user');
      return;
    }

    if (stompClient?.connected) {
      console.log('[STOMP] Already connected');
      return;
    }

    isManualDisconnect.current = false;

    if (!stompClient) {
      stompClient = createClient();
    }

    setStatus('connecting');
    stompClient.activate();
  }, [user]);

  const disconnect = useCallback(() => {
    isManualDisconnect.current = true;

    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }

    if (stompClient) {
      stompClient.deactivate();
      stompClient = null;
    }

    activeSubscriptions.clear();
    setStatus('disconnected');
    notifyStatusChange('disconnected');
  }, []);

  const subscribe = useCallback((projectId: string, callback: EventCallback): SubscriptionId => {
    const subscriptionId = `sub_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

    if (stompClient?.connected) {
      doSubscribe(subscriptionId, projectId, callback);
    } else {
      // Queue subscription for when connection is established
      activeSubscriptions.set(subscriptionId, { projectId, callback });
      console.log(`[STOMP] Queued subscription for ${projectId} (id: ${subscriptionId})`);
    }

    return subscriptionId;
  }, []);

  const unsubscribe = useCallback((subscriptionId: string) => {
    doUnsubscribe(subscriptionId);
  }, []);

  const sendMessage = useCallback((destination: string, body: Record<string, unknown>) => {
    if (!stompClient?.connected) {
      console.warn('[STOMP] Cannot send message - not connected');
      return;
    }

    stompClient.publish({
      destination,
      body: JSON.stringify(body),
      headers: {
        contentType: 'application/json',
      },
    });
  }, []);

  const reconnect = useCallback(() => {
    disconnect();
    reconnectAttempt = 0;
    setTimeout(connect, 100);
  }, [disconnect, connect]);

  // Auto-connect when authenticated
  useEffect(() => {
    if (user) {
      connect();
    } else {
      disconnect();
    }
  }, [user, connect, disconnect]);

  // Listen for test events to track last event
  useEffect(() => {
    const handleEvent = (e: CustomEvent<TestEvent>) => {
      setLastEvent(e.detail);
    };

    window.addEventListener('test-event', handleEvent as EventListener);
    return () => {
      window.removeEventListener('test-event', handleEvent as EventListener);
    };
  }, []);

  return {
    isConnected: status === 'connected',
    status,
    lastEvent,
    connect,
    disconnect,
    subscribe,
    unsubscribe,
    sendMessage,
    reconnect,
  };
}

// Event type constants
export const TEST_EVENTS = {
  TEST_RUN_UPDATED: 'TEST_RUN_UPDATED',
  TEST_EXECUTION_STARTED: 'TEST_EXECUTION_STARTED',
  TEST_EXECUTION_COMPLETED: 'TEST_EXECUTION_COMPLETED',
  TEST_EXECUTION_CANCELLED: 'TEST_EXECUTION_CANCELLED',
  COVERAGE_RECALCULATED: 'COVERAGE_RECALCULATED',
  REQUIREMENT_UPDATED: 'REQUIREMENT_UPDATED',
  DEFECT_LINKED: 'DEFECT_LINKED',
} as const;

export type TestEventType = (typeof TEST_EVENTS)[keyof typeof TEST_EVENTS];

// Payload types for each event
export interface TestRunUpdatedPayload {
  testRunId: string;
  status: string;
  updatedBy: string;
  changes?: Record<string, unknown>;
}

export interface TestExecutionStartedPayload {
  executionId: string;
  testCaseId: string;
  startedBy: string;
  environment?: string;
}

export interface CoverageRecalculatedPayload {
  projectId: string;
  coveragePercentage: number;
  requirementsCovered: number;
  requirementsTotal: number;
}