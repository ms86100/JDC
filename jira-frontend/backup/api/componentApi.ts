import axiosClient from './axiosClient';

export interface ComponentResponse {
  id: string;
  projectId: string;
  name: string;
  description?: string;
  leadUserId?: string;
  assigneeType: string;
  defaultAssignee?: string;
  archived: boolean;
  color?: string;
  icon?: string;
  sequence: number;
  createdBy?: string;
  updatedBy?: string;
  createdAt: string;
  updatedAt: string;
  issueCount?: number;
  openIssueCount?: number;
  closedIssueCount?: number;
  bugCount?: number;
  storyCount?: number;
}

export interface CreateComponentRequest {
  projectId: string;
  name: string;
  description?: string;
  leadUserId?: string;
  assigneeType?: string;
  defaultAssignee?: string;
  color?: string;
  icon?: string;
}

export interface UpdateComponentRequest {
  name?: string;
  description?: string;
  leadUserId?: string;
  assigneeType?: string;
  defaultAssignee?: string;
  color?: string;
  icon?: string;
  sequence?: number;
}

export interface TransferOwnershipRequest {
  newLeadId: string;
  reason?: string;
  transferredBy?: string;
}

export const componentApi = {
  getByProject: async (projectId: string, includeArchived = false): Promise<ComponentResponse[]> => {
    const response = await axiosClient.get(`/components/project/${projectId}?includeArchived=${includeArchived}`);
    return response.data;
  },

  getById: async (componentId: string): Promise<ComponentResponse> => {
    const response = await axiosClient.get(`/components/${componentId}`);
    return response.data;
  },

  create: async (data: CreateComponentRequest): Promise<ComponentResponse> => {
    const response = await axiosClient.post('/components', data);
    return response.data;
  },

  update: async (componentId: string, data: UpdateComponentRequest): Promise<ComponentResponse> => {
    const response = await axiosClient.put(`/components/${componentId}`, data);
    return response.data;
  },

  delete: async (componentId: string): Promise<void> => {
    await axiosClient.delete(`/components/${componentId}`);
  },

  archive: async (componentId: string): Promise<ComponentResponse> => {
    const response = await axiosClient.post(`/components/${componentId}/archive`);
    return response.data;
  },

  unarchive: async (componentId: string): Promise<ComponentResponse> => {
    const response = await axiosClient.post(`/components/${componentId}/unarchive`);
    return response.data;
  },

  restore: async (componentId: string): Promise<ComponentResponse> => {
    const response = await axiosClient.post(`/components/${componentId}/restore`);
    return response.data;
  },

  transferOwnership: async (componentId: string, data: TransferOwnershipRequest): Promise<ComponentResponse> => {
    const response = await axiosClient.post(`/components/${componentId}/transfer-ownership`, data);
    return response.data;
  },

  bulkAssign: async (issueIds: string[], componentId: string): Promise<number> => {
    const response = await axiosClient.post('/components/bulk-assign', { issueIds, componentId });
    return response.data;
  },

  bulkRemove: async (issueIds: string[], componentId: string): Promise<number> => {
    const response = await axiosClient.post('/components/bulk-remove', { issueIds, componentId });
    return response.data;
  },

  assignToIssue: async (issueId: string, componentId: string): Promise<void> => {
    await axiosClient.post('/components/issue', null, { params: { issueId, componentId } });
  },

  removeFromIssue: async (issueId: string, componentId: string): Promise<void> => {
    await axiosClient.delete('/components/issue', { params: { issueId, componentId } });
  },

  getIssueComponents: async (issueId: string): Promise<string[]> => {
    const response = await axiosClient.get(`/components/issue/${issueId}`);
    return response.data;
  },

  getOwnershipHistory: async (componentId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/components/${componentId}/ownership-history`);
    return response.data;
  },

  getMetrics: async (componentId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/components/${componentId}/metrics`);
    return response.data;
  },

  getAssignmentRules: async (componentId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/components/${componentId}/assignment-rules`);
    return response.data;
  },

  createAssignmentRule: async (componentId: string, rule: any): Promise<any> => {
    const response = await axiosClient.post(`/components/${componentId}/assignment-rules`, rule);
    return response.data;
  },

  deleteAssignmentRule: async (ruleId: string): Promise<void> => {
    await axiosClient.delete(`/components/assignment-rules/${ruleId}`);
  },

  getAuditLogs: async (componentId: string): Promise<any[]> => {
    const response = await axiosClient.get(`/components/${componentId}/audit`);
    return response.data;
  },
};