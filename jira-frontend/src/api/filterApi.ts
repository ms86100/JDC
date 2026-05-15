import apiClient from './axiosClient';

export interface SavedFilter {
  id: string;
  name: string;
  jql: string;
  owner: string;
  isShared: boolean;
  favorite: boolean;
  shareType: string;
  isSystem?: boolean;
  usageCount?: number;
  lastUsed?: string;
  createdAt?: string;
}

export interface FilterSubscription {
  id: string;
  userId: string;
  filterName: string;
  jqlQuery: string;
  frequency: 'INSTANT' | 'DAILY' | 'WEEKLY';
  isActive: boolean;
  emailNotification: boolean;
  lastNotified?: string;
  createdAt: string;
}

export const filterApi = {
  getSavedFilters: (tab: 'my' | 'shared' | 'system' = 'my') =>
    apiClient.get<SavedFilter[]>('/api/filters', { params: { tab } }),

  createFilter: (data: { name: string; jql: string; isShared?: boolean }) =>
    apiClient.post<SavedFilter>('/api/filters', data),

  deleteFilter: (filterId: string) =>
    apiClient.delete(`/api/filters/${filterId}`),

  toggleFavorite: (filterId: string) =>
    apiClient.post<SavedFilter>(`/api/filters/${filterId}/favorite`),

  getSubscriptions: () =>
    apiClient.get<FilterSubscription[]>('/api/filters/subscriptions'),

  createSubscription: (data: {
    filterName: string;
    jql: string;
    frequency?: 'INSTANT' | 'DAILY' | 'WEEKLY';
    emailNotification?: boolean;
  }) =>
    apiClient.post<FilterSubscription>('/api/filters/subscriptions', data),

  deleteSubscription: (subscriptionId: string) =>
    apiClient.delete(`/api/filters/subscriptions/${subscriptionId}`),

  toggleSubscription: (subscriptionId: string) =>
    apiClient.post<FilterSubscription>(`/api/filters/subscriptions/${subscriptionId}/toggle`),
};