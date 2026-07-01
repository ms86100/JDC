import apiClient from './axiosClient';

export interface ApplicationLink {
  id: string;
  name: string;
  url: string;
  applicationType: string;
  direction: string;
  status: string;
  primary: boolean;
}

export const integrationApi = {
  listApplicationLinks: () =>
    apiClient.get<ApplicationLink[]>('/integration/applinks'),
  createApplicationLink: (data: {
    name: string;
    url: string;
    applicationType?: string;
    direction?: string;
  }) => apiClient.post<ApplicationLink>('/integration/applinks', data),
  deleteApplicationLink: (id: string) => apiClient.delete(`/integration/applinks/${id}`),
  setPrimary: (id: string) =>
    apiClient.put<ApplicationLink>(`/integration/applinks/${id}/primary`),
  testConnection: (id: string) =>
    apiClient.get<{ linkId: string; status: string; message: string }>(
      `/integration/applinks/${id}/health`
    ),
};
