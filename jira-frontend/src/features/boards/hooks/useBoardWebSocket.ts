import { useEffect, useRef, useCallback, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';

export interface BoardWebSocketEvent {
  type: 'ISSUE_MOVED' | 'ISSUE_CREATED' | 'ISSUE_UPDATED' | 'ISSUE_DELETED' | 'BOARD_UPDATED' | 'COLUMN_UPDATED' | 'SPRINT_STARTED' | 'SPRINT_ENDED';
  payload: Record<string, unknown>;
  userId?: string;
  timestamp: string;
}

interface UseBoardWebSocketOptions {
  boardId: string;
  onEvent?: (event: BoardWebSocketEvent) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Error) => void;
  enabled?: boolean;
}

export function useBoardWebSocket({
  boardId,
  onEvent,
  onConnect,
  onDisconnect,
  onError,
  enabled = true,
}: UseBoardWebSocketOptions) {
  const queryClient = useQueryClient();
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const reconnectAttemptsRef = useRef(0);

  const connect = useCallback(() => {
    if (!enabled || !boardId) return;
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    try {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${protocol}//${window.location.host}/ws/boards/${boardId}`;

      const ws = new WebSocket(wsUrl);

      ws.onopen = () => {
        setIsConnected(true);
        reconnectAttemptsRef.current = 0;
        onConnect?.();
      };

      ws.onmessage = (event) => {
        try {
          const data: BoardWebSocketEvent = JSON.parse(event.data);

          switch (data.type) {
            case 'ISSUE_MOVED':
              queryClient.invalidateQueries({ queryKey: ['board-issues'] });
              queryClient.invalidateQueries({ queryKey: ['board-data'] });
              break;
            case 'ISSUE_CREATED':
            case 'ISSUE_UPDATED':
            case 'ISSUE_DELETED':
              queryClient.invalidateQueries({ queryKey: ['board-issues'] });
              break;
            case 'BOARD_UPDATED':
            case 'COLUMN_UPDATED':
              queryClient.invalidateQueries({ queryKey: ['board-config'] });
              break;
            case 'SPRINT_STARTED':
            case 'SPRINT_ENDED':
              queryClient.invalidateQueries({ queryKey: ['sprints'] });
              break;
          }

          onEvent?.(data);
        } catch {
          // Ignore parse errors
        }
      };

      ws.onclose = () => {
        setIsConnected(false);
        onDisconnect?.();

        if (enabled && boardId) {
          const delay = Math.min(1000 * Math.pow(2, reconnectAttemptsRef.current), 30000);
          reconnectTimeoutRef.current = setTimeout(() => {
            reconnectAttemptsRef.current++;
            connect();
          }, delay);
        }
      };

      ws.onerror = (error) => {
        onError?.(new Error('WebSocket error'));
      };

      wsRef.current = ws;
    } catch (error) {
      onError?.(error instanceof Error ? error : new Error('WebSocket connection failed'));
    }
  }, [boardId, enabled, onEvent, onConnect, onDisconnect, onError, queryClient]);

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setIsConnected(false);
  }, []);

  const send = useCallback((event: Omit<BoardWebSocketEvent, 'timestamp'>) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({
        ...event,
        timestamp: new Date().toISOString(),
      }));
    }
  }, []);

  useEffect(() => {
    if (enabled && boardId) {
      connect();
    }
    return () => {
      disconnect();
    };
  }, [boardId, enabled, connect, disconnect]);

  return {
    isConnected,
    send,
    disconnect,
    reconnect: connect,
  };
}
