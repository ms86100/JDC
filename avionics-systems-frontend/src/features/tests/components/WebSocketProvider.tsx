import React, { createContext, useContext, ReactNode } from 'react';
import { useTestWebSocket, type ConnectionStatus, type TestEvent, type EventCallback } from '../hooks/useTestWebSocket';
import { useAuth } from '../../auth/context/AuthContext';

export type { ConnectionStatus, TestEvent, EventCallback };

interface WebSocketContextValue {
  isConnected: boolean;
  status: ConnectionStatus;
  connect: () => void;
  disconnect: () => void;
  subscribe: (projectId: string, callback: EventCallback) => string;
  unsubscribe: (subscriptionId: string) => void;
}

const WebSocketContext = createContext<WebSocketContextValue | null>(null);

interface WebSocketProviderProps {
  children: ReactNode;
}

export function WebSocketProvider({ children }: WebSocketProviderProps) {
  const { isAuthenticated } = useAuth();
  const { isConnected, status, connect, disconnect, subscribe, unsubscribe } = useTestWebSocket();

  // The useTestWebSocket hook handles auto-connect when authenticated
  // So we don't need additional logic here

  const value: WebSocketContextValue = {
    isConnected,
    status,
    connect,
    disconnect,
    subscribe,
    unsubscribe,
  };

  return <WebSocketContext.Provider value={value}>{children}</WebSocketContext.Provider>;
}

/**
 * Hook to access WebSocket context from any component
 */
export function useWebSocket(): WebSocketContextValue {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
}

/**
 * Hook to subscribe to test events for a specific project
 * Automatically manages subscription lifecycle
 */
export function useProjectWebSocket(projectId: string | null) {
  const { subscribe, unsubscribe, status, isConnected } = useWebSocket();
  const [subscriptionId, setSubscriptionId] = React.useState<string | null>(null);

  const handleEvent = React.useCallback((event: TestEvent) => {
    // Event handling logic can be added here
    console.debug('[useProjectWebSocket] Event received:', event.eventType);
  }, []);

  React.useEffect(() => {
    if (!projectId) return;

    const subId = subscribe(projectId, handleEvent);
    setSubscriptionId(subId);

    return () => {
      unsubscribe(subId);
      setSubscriptionId(null);
    };
  }, [projectId, subscribe, unsubscribe, handleEvent]);

  return {
    subscriptionId,
    status,
    isConnected,
  };
}