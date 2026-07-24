import axiosClient from './axiosClient';

export const techEventApi = {
  create: (data: any) => axiosClient.post('/api/tech-events', data),
  getById: (id: string) => axiosClient.get(`/api/tech-events/${id}`),
  getByProject: (projectId: string) => axiosClient.get(`/api/tech-events/project/${projectId}`),
  update: (id: string, data: any) => axiosClient.put(`/api/tech-events/${id}`, data),
  transition: (id: string, targetStatus: string, userId?: string) =>
    axiosClient.post(`/api/tech-events/${id}/transition?targetStatus=${targetStatus}${userId ? '&userId=' + userId : ''}`),
  getAvailableTransitions: (id: string) => axiosClient.get(`/api/tech-events/${id}/available-transitions`),
  shareWithSupplier: (id: string, supplierProjectId: string) =>
    axiosClient.post(`/api/tech-events/${id}/share-supplier?supplierProjectId=${supplierProjectId}`),
  createBenchDefect: (id: string) => axiosClient.post(`/api/tech-events/${id}/create-bench-defect`),
  createProblemReport: (id: string, prOrigin?: string, prType?: string) =>
    axiosClient.post(`/api/tech-events/${id}/create-problem-report?${prOrigin ? 'prOrigin=' + prOrigin : ''}${prType ? '&prType=' + prType : ''}`),
};

export const benchDefectApi = {
  create: (data: any) => axiosClient.post('/api/bench-defects', data),
  getById: (id: string) => axiosClient.get(`/api/bench-defects/${id}`),
  getByProject: (projectId: string) => axiosClient.get(`/api/bench-defects/project/${projectId}`),
  update: (id: string, data: any) => axiosClient.put(`/api/bench-defects/${id}`, data),
};

export const problemReportApi = {
  create: (data: any) => axiosClient.post('/api/problem-reports', data),
  getById: (id: string) => axiosClient.get(`/api/problem-reports/${id}`),
  getByProject: (projectId: string) => axiosClient.get(`/api/problem-reports/project/${projectId}`),
  update: (id: string, data: any) => axiosClient.put(`/api/problem-reports/${id}`, data),
};
