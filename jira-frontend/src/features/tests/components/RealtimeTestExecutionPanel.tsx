import React, { useEffect, useState } from 'react';
import { useTestWebSocket } from '../hooks/useTestWebSocket';
import { useTestEvents } from '../hooks/useTestEvents';
import { useAuth } from '../../auth/context/AuthContext';

interface ExecutionUpdate {
  executionId: string;
  status: string;
  currentStep: number;
  totalSteps: number;
  passedSteps: number;
  failedSteps: number;
  elapsedTime: number;
}

interface TestEvent {
  eventId: string;
  eventType: string;
  projectId: string;
  timestamp: string;
  payload: Record<string, unknown>;
}

export const RealtimeTestExecutionPanel: React.FC = () => {
  const { user } = useAuth();
  const { isConnected } = useTestWebSocket();
  useTestEvents(user?.projectId || null);

  const [liveUpdate, setLiveUpdate] = useState<ExecutionUpdate | null>(null);

  useEffect(() => {
    const handleExecutionUpdate = (event: CustomEvent<ExecutionUpdate>) => {
      setLiveUpdate(event.detail);
    };

    window.addEventListener('execution-update', handleExecutionUpdate as EventListener);
    return () => {
      window.removeEventListener('execution-update', handleExecutionUpdate as EventListener);
    };
  }, []);

  return (
    <div className="realtime-panel p-4 bg-slate-800 rounded-lg">
      <div className="flex items-center gap-2 mb-4">
        <span className={`text-lg ${isConnected ? 'text-green-500' : 'text-red-500'}`}>
          {isConnected ? '🔗' : '⚡'}
        </span>
        <span className="text-sm text-slate-300">
          {isConnected ? 'Live updates enabled' : 'Reconnecting...'}
        </span>
      </div>

      {liveUpdate && (
        <div className="execution-live-preview p-4 bg-slate-900 rounded-lg">
          <div className="flex justify-between items-center mb-2">
            <span className="text-sm text-slate-400">
              Step {liveUpdate.currentStep}/{liveUpdate.totalSteps}
            </span>
            <span className="text-sm text-slate-300">
              {Math.floor(liveUpdate.elapsedTime / 1000)}s
            </span>
          </div>

          <div className="flex gap-4">
            <span className="flex items-center gap-1 text-green-400">
              <span>✓</span> {liveUpdate.passedSteps}
            </span>
            <span className="flex items-center gap-1 text-red-400">
              <span>✗</span> {liveUpdate.failedSteps}
            </span>
          </div>

          <div className="mt-2">
            <span className={`text-sm px-2 py-1 rounded ${
              liveUpdate.status === 'RUNNING' ? 'bg-blue-900 text-blue-300' :
              liveUpdate.status === 'PASSED' ? 'bg-green-900 text-green-300' :
              liveUpdate.status === 'FAILED' ? 'bg-red-900 text-red-300' :
              'bg-slate-700 text-slate-300'
            }`}>
              {liveUpdate.status}
            </span>
          </div>
        </div>
      )}
    </div>
  );
};

export default RealtimeTestExecutionPanel;