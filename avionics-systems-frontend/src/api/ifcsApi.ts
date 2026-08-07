import axiosClient from './axiosClient';

export const vvmCardApi = {
  create: (issueId: string, data: any) => axiosClient.post(`/api/issues/${issueId}/vvm-card`, null, { params: data }),
  get: (issueId: string) => axiosClient.get(`/api/issues/${issueId}/vvm-card`),
  update: (issueId: string, data: any) => axiosClient.put(`/api/issues/${issueId}/vvm-card`, null, { params: data }),
  getIvvCards: (vvmCardId: string) => axiosClient.get(`/api/issues/vvm-cards/${vvmCardId}/ivv-cards`),
};

export const ivvCardApi = {
  create: (issueId: string, data: any) => axiosClient.post(`/api/issues/${issueId}/ivv-card`, null, { params: data }),
  get: (issueId: string) => axiosClient.get(`/api/issues/${issueId}/ivv-card`),
  update: (issueId: string, data: any) => axiosClient.put(`/api/issues/${issueId}/ivv-card`, null, { params: data }),
};

export const groupApi = {
  create: (issueId: string, data: any) => axiosClient.post(`/api/issues/${issueId}/group`, null, { params: data }),
  get: (issueId: string) => axiosClient.get(`/api/issues/${issueId}/group`),
  update: (issueId: string, data: any) => axiosClient.put(`/api/issues/${issueId}/group`, null, { params: data }),
};

export const subChangeApi = {
  create: (issueId: string, data: any) => axiosClient.post(`/api/issues/${issueId}/sub-change`, null, { params: data }),
  get: (issueId: string) => axiosClient.get(`/api/issues/${issueId}/sub-change`),
  getByParent: (parentId: string) => axiosClient.get(`/api/issues/change-cards/${parentId}/sub-changes`),
  update: (issueId: string, data: any) => axiosClient.put(`/api/issues/${issueId}/sub-change`, null, { params: data }),
};
