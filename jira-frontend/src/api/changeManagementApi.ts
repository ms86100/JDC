import axiosClient from './axiosClient';

export const changeManagementApi = {
  createChangeCard: (issueId: string, data: any) => axiosClient.post(`/api/issues/${issueId}/change-card`, null, { params: data }),
  getChangeCard: (issueId: string) => axiosClient.get(`/api/issues/${issueId}/change-card`),
  updateChangeCard: (issueId: string, data: any) => axiosClient.put(`/api/issues/${issueId}/change-card`, null, { params: data }),
  getChangeCardsByDesignItem: (designItemId: string) => axiosClient.get(`/api/issues/design-items/${designItemId}/change-cards`),
  createDesignItem: (issueId: string, data: any) => axiosClient.post(`/api/issues/${issueId}/design-item`, null, { params: data }),
  getDesignItem: (issueId: string) => axiosClient.get(`/api/issues/${issueId}/design-item`),
  createDcl: (issueId: string, data: any) => axiosClient.post(`/api/issues/${issueId}/dcl`, null, { params: data }),
  getDcl: (issueId: string) => axiosClient.get(`/api/issues/${issueId}/dcl`),
  createDeliverable: (issueId: string, data: any) => axiosClient.post(`/api/issues/${issueId}/deliverable`, null, { params: data }),
  getDeliverable: (issueId: string) => axiosClient.get(`/api/issues/${issueId}/deliverable`),
};
