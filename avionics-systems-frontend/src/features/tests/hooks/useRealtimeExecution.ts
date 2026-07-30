import { useEffect, useCallback, useState } from 'react';

interface StepResultUpdate {
  executionId: string;
  stepId: string;
  result: 'PASSED' | 'FAILED' | 'BLOCKED' | 'SKIPPED';
  duration: number;
  errorMessage?: string;
}

interface ExecutionCompleteUpdate {
  executionId: string;
  status: 'PASSED' | 'FAILED' | 'BLOCKED' | 'CANCELLED';
  summary: {
    totalSteps: number;
    passed: number;
    failed: number;
    blocked: number;
    duration: number;
  };
}

interface ExecutionUpdate {
  executionId: string;
  status: string;
  currentStep: number;
  totalSteps: number;
  passedSteps: number;
  failedSteps: number;
  elapsedTime: number;
}

interface UseRealtimeExecutionReturn {
  liveExecution: ExecutionUpdate | null;
  currentStepResult: StepResultUpdate | null;
  executionCompleted: ExecutionCompleteUpdate | null;
}

export function useRealtimeExecution(executionId: string | null): UseRealtimeExecutionReturn {
  const [liveExecution, setLiveExecution] = useState<ExecutionUpdate | null>(null);
  const [currentStepResult, setCurrentStepResult] = useState<StepResultUpdate | null>(null);
  const [executionCompleted, setExecutionCompleted] = useState<ExecutionCompleteUpdate | null>(null);

  const handleExecutionUpdate = useCallback((event: CustomEvent<ExecutionUpdate>) => {
    if (executionId && event.detail.executionId !== executionId) return;
    setLiveExecution(event.detail);
  }, [executionId]);

  const handleStepResult = useCallback((event: CustomEvent<StepResultUpdate>) => {
    if (executionId && event.detail.executionId !== executionId) return;
    setCurrentStepResult(event.detail);
  }, [executionId]);

  const handleExecutionComplete = useCallback((event: CustomEvent<ExecutionCompleteUpdate>) => {
    if (executionId && event.detail.executionId !== executionId) return;
    setExecutionCompleted(event.detail);
  }, [executionId]);

  useEffect(() => {
    window.addEventListener('execution-update', handleExecutionUpdate as EventListener);
    window.addEventListener('execution-step-result', handleStepResult as EventListener);
    window.addEventListener('execution-completed', handleExecutionComplete as EventListener);

    return () => {
      window.removeEventListener('execution-update', handleExecutionUpdate as EventListener);
      window.removeEventListener('execution-step-result', handleStepResult as EventListener);
      window.removeEventListener('execution-completed', handleExecutionComplete as EventListener);
    };
  }, [handleExecutionUpdate, handleStepResult, handleExecutionComplete]);

  // Reset state when executionId changes
  useEffect(() => {
    setLiveExecution(null);
    setCurrentStepResult(null);
    setExecutionCompleted(null);
  }, [executionId]);

  return { liveExecution, currentStepResult, executionCompleted };
}