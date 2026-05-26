import apiClient from './axiosClient';

export interface ImportBatchResponse {
  id: string;
  importType?: string;
  ciSource?: string;
  ciBuildUrl?: string;
  status?: string;
  totalTests?: number;
  totalPassed?: number;
  totalFailed?: number;
  totalSkipped?: number;
  testsCreated?: number;
  testsUpdated?: number;
  executionsCreated?: number;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
}

export const importApi = {
  getHistory: (projectId: string) =>
    apiClient.get<ImportBatchResponse[]>('/import/history', { params: { projectId } }),

  getCucumberStatus: (batchId: string) =>
    apiClient.get<ImportBatchResponse>(`/api/import/cucumber/status/${batchId}`),

  getJunitStatus: (batchId: string) =>
    apiClient.get<ImportBatchResponse>(`/api/import/junit/status/${batchId}`),
};
