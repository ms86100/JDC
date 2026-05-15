import { useEffect, useRef, useCallback, useState } from 'react';
import { Client, IMessage } from '@stomp/stompjs';

/**
 * Type definitions for migration WebSocket events
 */
export interface EntityProgress {
  entityType: string;
  total: number;
  completed: number;
  failed: number;
  pending: number;
  processing: number;
}

export interface JobProgressUpdate {
  jobId: string;
  progressPercentage: number;
  processedEntities: number;
  totalEntities: number;
  failedEntities: number;
  currentStage: string;
  currentEntityType: string;
  timestamp: string;
  entityProgress: EntityProgress[];
}

export interface ValidationError {
  row: number;
  field: string;
  message: string;
  errorCode: string;
  severity: 'ERROR' | 'WARNING' | 'INFO';
}

export interface MigrationError {
  jobId: string;
  errorCode: string;
  errorMessage: string;
  entityType: string;
  entityKey: string;
  row: number;
  field: string;
  severity: 'ERROR' | 'WARNING' | 'CRITICAL';
  timestamp: string;
}

export interface ImportCompleteNotification {
  jobId: string;
  status: 'COMPLETED' | 'FAILED' | 'PARTIAL_SUCCESS';
  successCount: number;
  failedCount: number;
  completedAt: string;
  downloadUrl?: string;
}

interface WebSocketMessage<T> {
  eventType: string;
  jobId: string;
  userId: string;
  payload: T;
  timestamp: string;
  correlationId?: string;
}

interface UseMigrationWebSocketOptions {
  wsUrl?: string;
  reconnectDelay?: number;
  heartbeatInterval?: number;
  onProgressUpdate?: (update: JobProgressUpdate) => void;
  onValidationError?: (errors: ValidationError[]) => void;
  onJobCompleted?: (notification: ImportCompleteNotification) => void;
  onError?: (error: MigrationError) => void;
  onConnectionChange?: (connected: boolean) => void;
}

interface UseMigrationWebSocketReturn {
  connected: boolean;
  progress: JobProgressUpdate | null;
  errors: ValidationError[];
  jobCompleted: ImportCompleteNotification | null;
  subscribe: (jobId: string) => void;
  unsubscribe: (jobId: string) => void;
  sendHeartbeat: () => void;
  disconnect: () => void;
  lastEventId: string | null;
}

/**
 * Hook for WebSocket communication with the migration service.
 * Supports STOMP protocol with automatic reconnection and heartbeats.
 */
export function useMigrationWebSocket(
  jobId: string | null,
  userId: string,
  options: UseMigrationWebSocketOptions = {}
): UseMigrationWebSocketReturn {
  const {
    wsUrl = 'ws://localhost:8090/ws/migration',
    reconnectDelay = 5000,
    heartbeatInterval = 25000,
    onProgressUpdate,
    onValidationError,
    onJobCompleted,
    onError,
    onConnectionChange,
  } = options;

  const clientRef = useRef<Client | null>(null);
  const [connected, setConnected] = useState(false);
  const [progress, setProgress] = useState<JobProgressUpdate | null>(null);
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const [jobCompleted, setJobCompleted] = useState<ImportCompleteNotification | null>(null);
  const [lastEventId, setLastEventId] = useState<string | null>(null);
  const subscriptionsRef = useRef<Map<string, string>>(new Map());

  // Initialize STOMP client
  useEffect(() => {
    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay,
      heartbeatIncoming: heartbeatInterval,
      heartbeatOutgoing: heartbeatInterval,
      debug: (str) => {
        if (process.env.NODE_ENV === 'development') {
          console.log('[STOMP]', str);
        }
      },
    });

    client.onConnect = (frame) => {
      console.log('WebSocket connected');
      setConnected(true);
      onConnectionChange?.(true);

      // Resubscribe to previous job if any
      if (jobId) {
        subscribeToJob(jobId, client);
      }
    };

    client.onDisconnect = (frame) => {
      console.log('WebSocket disconnected');
      setConnected(false);
      onConnectionChange?.(false);
    };

    client.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers.message);
    };

    client.onWebSocketError = (event) => {
      console.error('WebSocket error:', event);
    };

    client.activate();
    clientRef.current = client;

    return () => {
      // Clean up subscriptions
      subscriptionsRef.current.forEach((subscriptionId) => {
        try {
          client.unsubscribe(subscriptionId);
        } catch (e) {
          // Subscription might already be removed
        }
      });
      subscriptionsRef.current.clear();

      client.deactivate();
      clientRef.current = null;
    };
  }, [wsUrl, reconnectDelay, heartbeatInterval]);

  // Subscribe to job updates when jobId changes
  useEffect(() => {
    if (jobId && connected && clientRef.current) {
      subscribeToJob(jobId, clientRef.current);
    }
  }, [jobId, connected]);

  const subscribeToJob = useCallback((targetJobId: string, client: Client) => {
    // Subscribe to progress updates
    const progressSub = `/topic/job/${targetJobId}/progress`;
    const progressSubscription = client.subscribe(progressSub, (message: IMessage) => {
      try {
        const parsed: WebSocketMessage<JobProgressUpdate> = JSON.parse(message.body);
        setProgress(parsed.payload);
        setLastEventId(parsed.correlationId || message.headers['message-id']);
        onProgressUpdate?.(parsed.payload);
      } catch (e) {
        console.error('Failed to parse progress update:', e);
      }
    });
    subscriptionsRef.current.set(`progress-${targetJobId}`, progressSubscription.id);

    // Subscribe to validation errors
    const validationSub = `/topic/job/${targetJobId}/validation`;
    const validationSubscription = client.subscribe(validationSub, (message: IMessage) => {
      try {
        const parsed: WebSocketMessage<{ newErrors: ValidationError[] }> = JSON.parse(message.body);
        setErrors((prev) => [...prev, ...(parsed.payload?.newErrors || [])]);
        setLastEventId(parsed.correlationId || message.headers['message-id']);
        onValidationError?.(parsed.payload?.newErrors || []);
      } catch (e) {
        console.error('Failed to parse validation update:', e);
      }
    });
    subscriptionsRef.current.set(`validation-${targetJobId}`, validationSubscription.id);

    // Subscribe to job completion
    const completedSub = `/topic/job/${targetJobId}/completed`;
    const completedSubscription = client.subscribe(completedSub, (message: IMessage) => {
      try {
        const parsed: WebSocketMessage<ImportCompleteNotification> = JSON.parse(message.body);
        setJobCompleted(parsed.payload);
        setLastEventId(parsed.correlationId || message.headers['message-id']);
        onJobCompleted?.(parsed.payload);
      } catch (e) {
        console.error('Failed to parse completion notification:', e);
      }
    });
    subscriptionsRef.current.set(`completed-${targetJobId}`, completedSubscription.id);

    // Subscribe to errors
    const errorsSub = `/topic/job/${targetJobId}/errors`;
    const errorsSubscription = client.subscribe(errorsSub, (message: IMessage) => {
      try {
        const parsed: WebSocketMessage<MigrationError> = JSON.parse(message.body);
        onError?.(parsed.payload);
      } catch (e) {
        console.error('Failed to parse error notification:', e);
      }
    });
    subscriptionsRef.current.set(`errors-${targetJobId}`, errorsSubscription.id);

    console.log(`Subscribed to job ${targetJobId} topics`);
  }, [onProgressUpdate, onValidationError, onJobCompleted, onError]);

  const subscribe = useCallback((targetJobId: string) => {
    if (clientRef.current?.connected) {
      subscribeToJob(targetJobId, clientRef.current);
    }
  }, [subscribeToJob]);

  const unsubscribe = useCallback((targetJobId: string) => {
    const prefixes = ['progress', 'validation', 'completed', 'errors'];
    prefixes.forEach((prefix) => {
      const subId = subscriptionsRef.current.get(`${prefix}-${targetJobId}`);
      if (subId && clientRef.current) {
        try {
          clientRef.current.unsubscribe(subId);
        } catch (e) {
          // Might already be unsubscribed
        }
        subscriptionsRef.current.delete(`${prefix}-${targetJobId}`);
      }
    });
    console.log(`Unsubscribed from job ${targetJobId}`);
  }, []);

  const sendHeartbeat = useCallback(() => {
    if (clientRef.current?.connected) {
      clientRef.current.publish({
        destination: '/app/heartbeat',
        body: JSON.stringify({ timestamp: new Date().toISOString() }),
      });
    }
  }, []);

  const disconnect = useCallback(() => {
    if (clientRef.current) {
      clientRef.current.deactivate();
    }
  }, []);

  return {
    connected,
    progress,
    errors,
    jobCompleted,
    subscribe,
    unsubscribe,
    sendHeartbeat,
    disconnect,
    lastEventId,
  };
}

/**
 * Hook for Server-Sent Events (SSE) fallback
 * Use when WebSocket is not available
 */
export function useMigrationSSE(
  jobId: string | null,
  options: {
    baseUrl?: string;
    onProgressUpdate?: (update: JobProgressUpdate) => void;
    onError?: (error: MigrationError) => void;
  } = {}
) {
  const { baseUrl = 'http://localhost:8090', onProgressUpdate, onError } = options;
  const [connected, setConnected] = useState(false);
  const [progress, setProgress] = useState<JobProgressUpdate | null>(null);
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!jobId) return;

    const eventSource = new EventSource(`${baseUrl}/api/sse/job/${jobId}/stream?userId=${userId}`);
    eventSourceRef.current = eventSource;

    eventSource.onopen = () => {
      console.log('SSE connected');
      setConnected(true);
    };

    eventSource.addEventListener('INITIAL_STATUS', (event) => {
      try {
        const data = JSON.parse(event.data);
        setProgress(data);
      } catch (e) {
        console.error('Failed to parse initial status:', e);
      }
    });

    eventSource.addEventListener('PROGRESS_UPDATE', (event) => {
      try {
        const data = JSON.parse(event.data) as JobProgressUpdate;
        setProgress(data);
        onProgressUpdate?.(data);
      } catch (e) {
        console.error('Failed to parse progress update:', e);
      }
    });

    eventSource.addEventListener('VALIDATION_ERROR', (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.newErrors) {
          setErrors((prev) => [...prev, ...data.newErrors]);
        }
      } catch (e) {
        console.error('Failed to parse validation error:', e);
      }
    });

    eventSource.addEventListener('JOB_COMPLETED', (event) => {
      console.log('Job completed via SSE:', event.data);
    });

    eventSource.addEventListener('ERROR', (event) => {
      try {
        const data = JSON.parse(event.data);
        if (data.errorCode && data.errorMessage) {
          onError?.(data as MigrationError);
        }
      } catch (e) {
        console.error('Failed to parse error:', e);
      }
    });

    eventSource.onerror = (error) => {
      console.error('SSE error:', error);
      setConnected(false);
      // EventSource will automatically try to reconnect
    };

    return () => {
      eventSource.close();
      eventSourceRef.current = null;
    };
  }, [jobId, baseUrl, userId]);

  return {
    connected,
    progress,
    errors,
    reconnect: () => {
      if (eventSourceRef.current) {
        eventSourceRef.current.close();
      }
      if (jobId) {
        const eventSource = new EventSource(`${baseUrl}/api/sse/job/${jobId}/stream?userId=${userId}`);
        eventSourceRef.current = eventSource;
      }
    },
  };
}

/**
 * Hook for polling fallback
 * Use when WebSocket and SSE are not available
 */
export function useMigrationPolling(
  jobId: string | null,
  options: {
    baseUrl?: string;
    pollInterval?: number;
    maxRetries?: number;
    onProgressUpdate?: (update: JobProgressUpdate) => void;
    onError?: (error: MigrationError) => void;
  } = {}
) {
  const {
    baseUrl = 'http://localhost:8090',
    pollInterval = 2000,
    maxRetries = 3,
    onProgressUpdate,
    onError,
  } = options;

  const [progress, setProgress] = useState<JobProgressUpdate | null>(null);
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const retriesRef = useRef(0);

  const fetchProgress = useCallback(async () => {
    if (!jobId) return;

    setLoading(true);
    try {
      const response = await fetch(`${baseUrl}/api/ws/job/${jobId}/status`);

      if (response.ok) {
        const data = await response.json() as JobProgressUpdate;
        setProgress(data);
        setError(null);
        retriesRef.current = 0;
        onProgressUpdate?.(data);
      } else if (response.status === 404) {
        // Job not found, stop polling
        stopPolling();
      } else {
        handleError(`HTTP ${response.status}`);
      }
    } catch (e) {
      handleError(e instanceof Error ? e.message : 'Network error');
    } finally {
      setLoading(false);
    }
  }, [jobId, baseUrl, onProgressUpdate]);

  const handleError = (message: string) => {
    retriesRef.current++;
    setError(message);

    if (retriesRef.current >= maxRetries) {
      stopPolling();
      setError(`Max retries exceeded: ${message}`);
    }
  };

  const startPolling = useCallback(() => {
    if (pollIntervalRef.current) return;

    fetchProgress();
    pollIntervalRef.current = setInterval(fetchProgress, pollInterval);
  }, [fetchProgress, pollInterval]);

  const stopPolling = useCallback(() => {
    if (pollIntervalRef.current) {
      clearInterval(pollIntervalRef.current);
      pollIntervalRef.current = null;
    }
  }, []);

  useEffect(() => {
    if (jobId) {
      startPolling();
    }

    return () => {
      stopPolling();
    };
  }, [jobId]);

  return {
    progress,
    errors,
    loading,
    error,
    refetch: fetchProgress,
    startPolling,
    stopPolling,
  };
}