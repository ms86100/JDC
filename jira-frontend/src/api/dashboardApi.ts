import apiClient from './axiosClient';

export interface Gadget {
  id: string;
  gadgetType: string;
  title: string;
  positionX: number;
  positionY: number;
  width: number;
  height: number;
  preferences: Record<string, any>;
}

export interface Dashboard {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  isDefault: boolean;
  isGlobal: boolean;
  gadgets: Gadget[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateDashboardRequest {
  name: string;
  description?: string;
  isDefault?: boolean;
  isGlobal?: boolean;
}

export const dashboardApi = {
  getAll: (includeGlobal = true) =>
    apiClient.get<Dashboard[]>('/dashboards', { params: { includeGlobal } }),

  getById: (id: string) =>
    apiClient.get<Dashboard>(`/dashboards/${id}`),

  create: (data: CreateDashboardRequest) =>
    apiClient.post<Dashboard>('/dashboards', data),

  update: (id: string, data: CreateDashboardRequest) =>
    apiClient.put<Dashboard>(`/dashboards/${id}`, data),

  delete: (id: string) =>
    apiClient.delete(`/dashboards/${id}`),

  addGadget: (dashboardId: string, gadget: Omit<Gadget, 'id'>) =>
    apiClient.post<Dashboard>(`/dashboards/${dashboardId}/gadgets`, gadget),

  updateGadget: (dashboardId: string, gadgetId: string, gadget: Gadget) =>
    apiClient.put<Dashboard>(`/dashboards/${dashboardId}/gadgets/${gadgetId}`, gadget),

  removeGadget: (dashboardId: string, gadgetId: string) =>
    apiClient.delete<Dashboard>(`/dashboards/${dashboardId}/gadgets/${gadgetId}`),

  getGadgetData: (gadgetType: string, preferences?: Record<string, any>) =>
    apiClient.get<Record<string, any>>('/dashboards/gadgets/data', {
      params: { gadgetType, ...preferences },
    }),
};