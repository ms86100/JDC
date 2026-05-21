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
  /** Optional: show connection status indicator in UI */
  showStatusIndicator?: boolean;
}

export function WebSocketProvider({ children, showStatusIndicator = false }: WebSocketProviderProps) {
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

  return (
    <WebSocketContext.Provider value={value}>
      {children}
      {showStatusIndicator && <ConnectionStatusIndicator status={status} />}
    </WebSocketContext.Provider>
  );
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
 * Connection status indicator component
 * Shows a small badge in the corner of the screen
 */
function ConnectionStatusIndicator({ status }: { status: ConnectionStatus }) {
  const statusConfig: Record<ConnectionStatus, { color: string; label: string }> = {
    connected: { color: '#4CAF50', label: 'Live' },
    connecting: { color: '#FF9800', label: 'Connecting...' },
    disconnected: { color: '#9E9E9E', label: 'Offline' },
    error: { color: '#f44336', label: 'Error' },
  };

  const config = statusConfig[status];

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '16px',
        right: '16px',
        zIndex: 9999,
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        padding: '8px 12px',
        backgroundColor: 'white',
        borderRadius: '20px',
        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.15)',
        fontSize: '12px',
        fontFamily: 'system-ui, -apple-system, sans-serif',
      }}
    >
      <span
        style={{
          width: '8px',
          height: '8px',
          borderRadius: '50%',
          backgroundColor: config.color,
          animation: status === 'connecting' ? 'pulse 1.5s infinite' : 'none',
        }}
      />
      <span style={{ color: '#333' }}>{config.label}</span>
      <style>
        {`
          @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.4; }
          }
        `}
      </style>
    </div>
  );
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