import axiosClient from './axiosClient';

const TEST_SERVICE_BASE = '/api';

// VVO endpoints
export const vvoApi = {
  create: (data: any) => axiosClient.post(`${TEST_SERVICE_BASE}/vvo`, data),
  getById: (id: string) => axiosClient.get(`${TEST_SERVICE_BASE}/vvo/${id}`),
  getByProject: (projectId: string) => axiosClient.get(`${TEST_SERVICE_BASE}/vvo/project/${projectId}`),
  getByHlvvo: (hlvvoId: string) => axiosClient.get(`${TEST_SERVICE_BASE}/vvo/hlvvo/${hlvvoId}`),
  getByDoorsId: (idDoors: string) => axiosClient.get(`${TEST_SERVICE_BASE}/vvo/by-doors-id/${idDoors}`),
  getByFixVersion: (fixVersionId: string) => axiosClient.get(`${TEST_SERVICE_BASE}/vvo/by-fix-version/${fixVersionId}`),
  update: (id: string, data: any) => axiosClient.put(`${TEST_SERVICE_BASE}/vvo/${id}`, data),
  clone: (id: string) => axiosClient.post(`${TEST_SERVICE_BASE}/vvo/${id}/clone`),
  archive: (id: string) => axiosClient.delete(`${TEST_SERVICE_BASE}/vvo/${id}`),
  // Baseline
  tagBaseline: (data: any) => axiosClient.post(`${TEST_SERVICE_BASE}/vvo/baseline/tag`, data),
  publishBaseline: (projectId: string, fixVersionId: string) =>
    axiosClient.post(`${TEST_SERVICE_BASE}/vvo/baseline/publish?projectId=${projectId}&fixVersionId=${fixVersionId}`),
  getBaselineSummary: (projectId: string, fixVersionId: string) =>
    axiosClient.get(`${TEST_SERVICE_BASE}/vvo/baseline/summary?projectId=${projectId}&fixVersionId=${fixVersionId}`),
  cloneWithSupersede: (id: string) => axiosClient.post(`${TEST_SERVICE_BASE}/vvo/baseline/clone-with-supersede/${id}`),
  // DOORS
  exportForDoors: (data: any) => axiosClient.post(`${TEST_SERVICE_BASE}/vvo/baseline/doors/export`, data),
  importDoorsIds: (projectId: string, csvContent: string) =>
    axiosClient.post(`${TEST_SERVICE_BASE}/vvo/baseline/doors/import?projectId=${projectId}`, csvContent, { headers: { 'Content-Type': 'text/plain' } }),
  // Transfer
  transferVvos: (data: any) => axiosClient.post(`${TEST_SERVICE_BASE}/vvo/baseline/transfer`, data),
};

// HLVVO endpoints
export const hlvvoApi = {
  create: (data: any) => axiosClient.post(`${TEST_SERVICE_BASE}/hlvvo`, data),
  getById: (id: string) => axiosClient.get(`${TEST_SERVICE_BASE}/hlvvo/${id}`),
  getByProject: (projectId: string) => axiosClient.get(`${TEST_SERVICE_BASE}/hlvvo/project/${projectId}`),
  update: (id: string, data: any) => axiosClient.put(`${TEST_SERVICE_BASE}/hlvvo/${id}`, data),
  getChildVvos: (id: string) => axiosClient.get(`${TEST_SERVICE_BASE}/hlvvo/${id}/child-vvos`),
};

// TestRequest endpoints
export const testRequestApi = {
  create: (data: any) => axiosClient.post(`${TEST_SERVICE_BASE}/test-requests`, data),
  getById: (id: string) => axiosClient.get(`${TEST_SERVICE_BASE}/test-requests/${id}`),
  getByProject: (projectId: string) => axiosClient.get(`${TEST_SERVICE_BASE}/test-requests/project/${projectId}`),
  getVvos: (id: string) => axiosClient.get(`${TEST_SERVICE_BASE}/test-requests/${id}/vvos`),
};
