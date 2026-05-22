import apiClient from './axiosClient';

export interface DefectDensityReport {
  projectId?: string;
  totalDefects?: number;
  defectsPerStoryPoint?: number;
  defectsPerTest?: number;
  bySeverity?: Record<string, number>;
}

export interface SprintQualityReport {
  projectId?: string;
  sprintId?: string;
  passRate?: number;
  totalTests?: number;
  passed?: number;
  failed?: number;
  blocked?: number;
}

export interface AutomationCoverageReport {
  projectId?: string;
  totalTests?: number;
  automatedTests?: number;
  manualTests?: number;
  automationPercent?: number;
}

export interface WebhookResponse {
  success?: boolean;
  message?: string;
  executionId?: string;
}

export const issueTestOpsApi = {
  getDefectDensity: (projectId: string) =>
    apiClient.get<DefectDensityReport>('/api/reports/defect-density', { params: { projectId } }),

  getSprintQuality: (projectId: string, sprintId?: string) =>
    apiClient.get<SprintQualityReport>('/api/reports/sprint-quality', {
      params: { projectId, sprintId },
    }),

  getAutomationCoverage: (projectId: string) =>
    apiClient.get<AutomationCoverageReport>('/api/reports/automation-coverage', {
      params: { projectId },
    }),

  analyzeDuplicates: (tests: unknown[]) =>
    apiClient.post('/api/ai/analyze-duplicates', { tests }),

  getCoverageRecommendations: (projectId: string, requirementKeys: string[]) =>
    apiClient.get(`/api/ai/coverage-recommendations/${projectId}`, {
      params: { requirementKeys },
      paramsSerializer: (params) => {
        const keys = (params.requirementKeys as string[]) || [];
        return keys.map((k) => `requirementKeys=${encodeURIComponent(k)}`).join('&');
      },
    }),

  clusterFailures: (failures: unknown[]) =>
    apiClient.post('/api/ai/cluster-failures', { failures }),

  suggestTests: (requirementDescription: string) =>
    apiClient.post('/api/ai/suggest-tests', { requirementDescription }),

  assessRisk: (testId: string, history: unknown[]) =>
    apiClient.post(`/api/ai/assess-risk/${testId}`, { history }),

  triggerCiExecution: (projectId: string, payload: Record<string, unknown>) =>
    apiClient.post('/api/webhooks/trigger', payload, { params: { projectId } }),

  sendGitHubWebhook: (projectId: string, payload: Record<string, unknown>) =>
    apiClient.post<WebhookResponse>('/api/webhooks/github-actions', payload, {
      params: { projectId },
    }),

  sendJenkinsWebhook: (projectId: string, payload: Record<string, unknown>) =>
    apiClient.post<WebhookResponse>('/api/webhooks/jenkins', payload, { params: { projectId } }),

  sendGitLabWebhook: (projectId: string, payload: Record<string, unknown>) =>
    apiClient.post<WebhookResponse>('/api/webhooks/gitlab', payload, { params: { projectId } }),

  sendAzureDevOpsWebhook: (projectId: string, payload: Record<string, unknown>) =>
    apiClient.post<WebhookResponse>('/api/webhooks/azure-devops', payload, { params: { projectId } }),
};
