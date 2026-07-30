import axiosClient from './axiosClient';

export const vvReportApi = {
  getCoverageReport: (projectId: string, fixVersionId: string) =>
    axiosClient.get(`/api/vv-reports/coverage?projectId=${projectId}&fixVersionId=${fixVersionId}`),
  exportCoverageCsv: (projectId: string, fixVersionId: string) =>
    axiosClient.get(`/api/vv-reports/coverage/export?projectId=${projectId}&fixVersionId=${fixVersionId}`, { responseType: 'blob' }),
  getTechEventReport: (projectId: string) => axiosClient.get(`/api/vv-reports/tech-events?projectId=${projectId}`),
  getBenchDefectReport: (projectId: string) => axiosClient.get(`/api/vv-reports/bench-defects?projectId=${projectId}`),
  getProblemReportSummary: (projectId: string) => axiosClient.get(`/api/vv-reports/problem-reports?projectId=${projectId}`),
  getProjectDashboard: (projectId: string) => axiosClient.get(`/api/vv-reports/dashboard/${projectId}`),
  exportForPlanning: (testPlanId: string) => axiosClient.get(`/api/vv-reports/planning-export/${testPlanId}`, { responseType: 'blob' }),
};
