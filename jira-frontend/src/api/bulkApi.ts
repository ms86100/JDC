import apiClient from './axiosClient';

export type BulkOperationType = 'UPDATE_STATUS' | 'UPDATE_FIELDS' | 'CLONE' | 'MOVE_TO_SPRINT' | 'ADD_LABELS' | 'DELETE';

export interface BulkOperationRequest {
  issueIds: string[];
  operationType: BulkOperationType;
  newStatus?: string;
  assigneeId?: string;
  priority?: string;
  labels?: string;
  sprintId?: string;
  targetProjectId?: string;
  keepLinks?: boolean;
  keepAttachments?: boolean;
}

export interface BulkOperationResult {
  issueKey: string;
  success: boolean;
  message: string;
  errorCode?: string;
}

export type OperationStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'PARTIAL_SUCCESS';

export interface BulkOperationResponse {
  operationId: string;
  operationType: BulkOperationType;
  totalIssues: number;
  successCount: number;
  failedCount: number;
  status: OperationStatus;
  results: BulkOperationResult[];
  startedAt: string;
  completedAt: string;
}

export const bulkApi = {
  execute: (data: BulkOperationRequest) =>
    apiClient.post<BulkOperationResponse>('/api/bulk-operations', data),

  getStatus: (operationId: string) =>
    apiClient.get<BulkOperationResponse>(`/api/bulk-operations/${operationId}`),

  getRecent: () =>
    apiClient.get<BulkOperationResponse[]>('/api/bulk-operations/recent'),
};